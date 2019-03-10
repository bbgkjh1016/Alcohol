package com.example.alcohol;

import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.content.Intent;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    static final int REQUEST_ENABLE_BT = 10;
    BluetoothAdapter mBluetoothAdapter;

    public void start_btn(View view) {
        if(!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "블루투스를 활성화 하세요", Toast.LENGTH_SHORT).show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else {
            is_execute_by_phone mode = new is_execute_by_phone();
            mode.changeMode("ALCOHOL");
            Intent AlcoholIntent = new Intent(MainActivity.this, AlcoholActivity.class);
            startActivity(AlcoholIntent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // 타이틀 변경하기
        getSupportActionBar().setTitle("HOME");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    Intent AlcoholIntent = new Intent(MainActivity.this, AlcoholActivity.class);
                    startActivity(AlcoholIntent);
                }
                else if (resultCode == RESULT_CANCELED) {
                    finish();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // 액션버튼 메뉴 액션바에 집어 넣기
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    // 액션버튼을 클릭했을때의 동작
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if (id == R.id.action_button) {
            Toast.makeText(getApplicationContext(), "패어링할 디바이스를 선택하기 전에 자동차 측정버튼을 누르세요", Toast.LENGTH_SHORT).show();

            is_execute_by_phone mode = new is_execute_by_phone();
            mode.changeMode("REALTIME");
            Intent AlcoholIntent = new Intent(MainActivity.this, AlcoholActivity.class);
            startActivity(AlcoholIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
