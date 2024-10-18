package com.example.hasee.myapplication;

import android.app.Activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.MapView;

import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;


public class MainActivity extends Activity implements View.OnClickListener {
    TextView Aircraft_type, connection_status;
    Button open;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.VIBRATE,
                            android.Manifest.permission.INTERNET, android.Manifest.permission.ACCESS_WIFI_STATE,
                            android.Manifest.permission.WAKE_LOCK, android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.ACCESS_NETWORK_STATE, android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.CHANGE_WIFI_STATE, android.Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.SYSTEM_ALERT_WINDOW,
                            android.Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        setContentView(R.layout.activity_main);

        Aircraft_type = (TextView) findViewById(R.id.aircraft_type);
        connection_status = (TextView) findViewById(R.id.connection_status);
        open = (Button) findViewById(R.id.open);

        open.setOnClickListener(this);
        open.setEnabled(false);

        Aircraft_type.setText("无人机型号");
        connection_status.setText("尚未连接无人机");

        IntentFilter i = new IntentFilter();
        i.addAction(DemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(reciver, i);

    }


    @Override
    protected void onDestroy() {

        unregisterReceiver(reciver);
        super.onDestroy();
    }

    protected BroadcastReceiver reciver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            is_reciver();
        }
    };

    private void is_reciver() {
        BaseProduct product = DemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            open.setEnabled(true);
            String A_type = product instanceof Aircraft ? "AirCraft!" : "HandHold!";
            connection_status.setText("Success Connect To " + A_type);

            if (null != product.getModel())
                Aircraft_type.setText(product.getModel().getDisplayName());
            else
                Aircraft_type.setText(" ");
        } else {
            open.setEnabled(false);
            Aircraft_type.setText("无人机型号");
            connection_status.setText("尚未连接无人机");
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.open: {
                Intent t = new Intent(MainActivity.this, main.class);

                startActivity(t);
                break;
            }

            default:
                break;
        }
    }
}

