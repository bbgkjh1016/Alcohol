package com.example.alcohol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AlcoholActivity extends AppCompatActivity {
    int mPairedDeviceCount = 0;
    byte[] readBuffer;
    int readBufferPosition;

    Set<BluetoothDevice> mDevices;
    BluetoothAdapter mBluetoothAdapter;

    BluetoothDevice mRemoteDevice;
    BluetoothSocket mSocket = null;
    OutputStream mOutputStream = null;
    InputStream mInputStream = null;
    Thread mWorkerThread = null;

    TextView textViewReceive;

    BluetoothDevice getDeviceFromBondedList(String name) {
        BluetoothDevice selectedDevice = null;
        for (BluetoothDevice device : mDevices) {
            if(name.equals(device.getName())) {
                selectedDevice = device;
                break;
            }
        }
        return selectedDevice;
    }

    void sendData(String msg) {
        msg += "\n"; // 문자열 종료 표시
        try {
            mOutputStream.write(msg.getBytes()); // 문자열 전송
        }
        catch (Exception e) {
            finish();
        }
    }

    void beginListenForData() {
        final Handler handler = new Handler();

        textViewReceive = (TextView) findViewById(R.id.alcohol_textView);

        readBuffer = new byte[1024]; // 수신 버퍼
        readBufferPosition = 0; // 버퍼 내 수신 문자 저장 위치

        // 문자열 수신 쓰레드
        mWorkerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()) {
                    try {
                        int bytesAvailable = mInputStream.available(); // 수신 데이터 확인
                        if(bytesAvailable > 0) { // 데이터가 수신된 경우
                            byte[] packetBytes = new byte[bytesAvailable];
                            mInputStream.read(packetBytes);
                            // 입력 스트림 바이트를 한 바이트씩 읽음
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                // 개행문자를 기준으로 받음(한줄)
                                if(b == '\n') {
                                    // readBuffer 배열을 encodedBytes로 복사
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);

                                    // 인코딩 된 바이트 배열을 문자열로 변환
                                    final String text = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            // 텍스트 뷰에 출력
                                            textViewReceive.setText(text + "\n");
                                        }
                                    });
                                } // 개행 문자가 아닐 경우
                                else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex) {
                        finish();
                    }

                    try {
                        // 1초마다 받아옴
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e) {
                        finish();
                    }
                }
            }
        });

        mWorkerThread.start();
    }

    void connectToSelectedDevice(String selectedDeviceName) {
        mRemoteDevice = getDeviceFromBondedList(selectedDeviceName);
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        try {
            // 소켓 생성
            mSocket = mRemoteDevice.createRfcommSocketToServiceRecord(uuid);

            // RFCOMM 채널을 통한 연결
            mSocket.connect();

            // 데이터 송수신을 위한 스트림 얻기
            mOutputStream = mSocket.getOutputStream();
            mInputStream = mSocket.getInputStream();

            is_execute_by_phone mode = new is_execute_by_phone();
            String currentMode = mode.getMode();

            if(currentMode == "ALCOHOL")
            {
                Toast.makeText(getApplicationContext(), "자동차에서 음주측정을 시작합니다", Toast.LENGTH_SHORT).show();
                sendData("Y");
            }
            else if(currentMode == "REALTIME") {
                sendData("N");
            }

            // 데이터 수신 준비
            beginListenForData();
        }
        catch (Exception e) {
            Toast.makeText(getApplicationContext(), "페어링을 실패했습니다", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    void selectDevice() {
        mDevices = mBluetoothAdapter.getBondedDevices();
        mPairedDeviceCount = mDevices.size();

        // 페어링 된 장치가 없는 경우
        if(mPairedDeviceCount == 0) {
            finish();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("블루투스 디바이스 선택");

        // 페어링 된 블루투스 장치의 이름 목록 작성
        List<String> listItems = new ArrayList<String>();
        for (BluetoothDevice device : mDevices) {
            listItems.add(device.getName());
        }
        listItems.add("취소");    // 취소 항목 추가

        final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
        builder.setItems(items, new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int item){
                if(item == mPairedDeviceCount){
                    // 연결할 장치를 선택하지 않고 ‘취소’를 누른 경우
                    Toast.makeText(getApplicationContext(), "뒤로가기", Toast.LENGTH_SHORT).show();
                    finish();
                }
                else{
                    // 연결할 장치를 선택한 경우, 선택한 장치와 연결을 시도함
                    connectToSelectedDevice(items[item].toString());
                }
            }
        });

        // 뒤로 가기 버튼 사용 금지
        builder.setCancelable(false);

        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onDestroy() {
        try {
            mWorkerThread.interrupt(); // 데이터 수신 쓰레드 종료
            mInputStream.close();
            mOutputStream.close();
            mSocket.close();
        } catch (Exception e) {}
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alcohol);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        is_execute_by_phone mode = new is_execute_by_phone();
        String currentMode = mode.getMode();

        // 타이틀 변경하기
        if(currentMode == "ALCOHOL") {
            getSupportActionBar().setTitle("음주측정");
        }
        else if(currentMode == "REALTIME") {
            getSupportActionBar().setTitle("현재 실시간 음주측정 상황");
        }

        // 홈버튼 표시
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        selectDevice();
    }

    // 액션버튼을 클릭했을때의 동작
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if (id == android.R.id.home) {
            Toast.makeText(this, "뒤로가기", Toast.LENGTH_SHORT).show();
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }
}
