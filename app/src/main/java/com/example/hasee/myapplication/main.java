package com.example.hasee.myapplication;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.amap.api.maps2d.AMap.OnMapClickListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.battery.BatteryState;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.gimbal.GimbalState;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.product.Model;
import dji.common.remotecontroller.AircraftStickMappingTarget;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * Created by hasee on 2017/11/14.
 */

public class main extends Activity implements AMap.OnMarkerDragListener, AMap.OnMapLoadedListener, AMap.OnInfoWindowClickListener, AMap.InfoWindowAdapter, View.OnClickListener, AMap.OnMarkerClickListener, OnMapClickListener, TextureView.SurfaceTextureListener, ServiceConnection {
    TextView aircraft_msg, record_time;
    FlightController controller;
    double aircraft_lat = 110, aircraft_lng = 110;
    float altitude = 0, roll = 0, pitch = 0, yaw = 0;
    int sat_num, battary_left;

    Button take_picture, switch_mode;
    ToggleButton record;
    TextureView m_picture;

    protected DJICodecManager m_Codec;
    protected VideoFeeder.VideoDataCallback m_viderfeeder;
    private Boolean mode = false;
    MapView mMapView;
    AMap aMap;
    private Button waypoint_back, waypoint_add, waypoint_clear,waypoint_set, waypoint_upload, waypoint_start, waypoint_stop;

    private WaypointMissionOperator istance;
    private Boolean isadd = false;
    private int i = 0;
    private final Map<Integer, Marker> markers = new ConcurrentHashMap<>();
    float waypoint_alt = 20.0f;
    private List<Waypoint> waypoint_list = new ArrayList<>();
    public static WaypointMission.Builder waypointMissionBuilder;
    Marker fly_mark = null;
    DecimalFormat decimalFormat = new DecimalFormat("0.0000");//构造方法的字符格式这里如果小数不足4位,会以0补足.

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

        setContentView(R.layout.main);

        aircraft_msg = (TextView) findViewById(R.id.text);
        take_picture = (Button) findViewById(R.id.takephoto);
        record = (ToggleButton) findViewById(R.id.record);
        switch_mode = (Button) findViewById(R.id.switchmode);
        m_picture = (TextureView) findViewById(R.id.picture);
        record_time = (TextView) findViewById(R.id.time);

        waypoint_back = (Button) findViewById(R.id.back);
        waypoint_add = (Button) findViewById(R.id.addpoint);
        waypoint_clear = (Button) findViewById(R.id.clearpoint);
//        waypoint_set=(Button)findViewById(R.id.setting);
        waypoint_upload = (Button) findViewById(R.id.upload);
        waypoint_start = (Button) findViewById(R.id.waypointstart);
        waypoint_stop = (Button) findViewById(R.id.waypointstop);

        mMapView = (MapView) findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);

//        if (aMap == null) {
//            aMap = mMapView.getMap();
//        }

        take_picture.setOnClickListener(this);
        switch_mode.setOnClickListener(this);

        waypoint_back.setOnClickListener(this);
        waypoint_add.setOnClickListener(this);
        waypoint_clear.setOnClickListener(this);
        waypoint_upload.setOnClickListener(this);
        waypoint_start.setOnClickListener(this);
        waypoint_stop.setOnClickListener(this);

        if (m_picture != null)
            m_picture.setSurfaceTextureListener(this);

        m_viderfeeder = new VideoFeeder.VideoDataCallback() {

            @Override
            public void onReceive(byte[] bytes, int i) {
                m_Codec.sendDataToDecoder(bytes, i);
            }
        };

        record.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    startRecord();
                } else {
                    stopRecord();
                }
            }
        });

        Camera camera = DemoApplication.getCameraInstance();

        if (camera != null) {

            camera.setSystemStateCallback(new SystemState.Callback() {
                @Override
                public void onUpdate(SystemState cameraSystemState) {
                    if (null != cameraSystemState) {

                        int recordTime = cameraSystemState.getCurrentVideoRecordingTimeInSeconds();
                        int minutes = (recordTime % 3600) / 60;
                        int seconds = recordTime % 60;

                        final String timeString = String.format("%02d:%02d", minutes, seconds);
                        final boolean isVideoRecording = cameraSystemState.isRecording();

                        main.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                record_time.setText(timeString);

                                /*
                                 * Update recordingTime TextView visibility and mRecordBtn's check state
                                 */
                                if (isVideoRecording) {
                                    record_time.setVisibility(View.VISIBLE);
                                } else {
                                    record_time.setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                    }
                }
            });

        }
        IntentFilter i = new IntentFilter();
        i.addAction(DemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(reciver, i);


        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                Message m = new Message();
                m.what = 1;
                handler.sendMessage(m);
            }
        }, 200, 200);

        new move_to().start();
        addListener();
        initmap();
    }

    private void initmap() {
        if (aMap == null) {
            aMap = mMapView.getMap();
            aMap.setOnMapClickListener(this);// add the listener for click for amap object
            aMap.setOnMarkerClickListener(this);//去除单个标点
            aMap.setInfoWindowAdapter(this);
            aMap.setOnInfoWindowClickListener(this);//显示条可点击
            aMap.setOnMarkerDragListener(this);// 设置marker可拖拽事件监听器
            aMap.setOnMapLoadedListener(this);
        }
    }

    private void addListener() {
        if (WaypointMissionOperator() != null) {
            WaypointMissionOperator().addListener(addListener);
        }
    }

    private void removeListener() {
        if (WaypointMissionOperator() != null) {
            WaypointMissionOperator().removeListener(addListener);
        }
    }

    private WaypointMissionOperatorListener addListener = new WaypointMissionOperatorListener() {
        @Override
        public void onDownloadUpdate(@NonNull WaypointMissionDownloadEvent waypointMissionDownloadEvent) {

        }

        @Override
        public void onUploadUpdate(@NonNull WaypointMissionUploadEvent waypointMissionUploadEvent) {

        }

        @Override
        public void onExecutionUpdate(@NonNull WaypointMissionExecutionEvent waypointMissionExecutionEvent) {

        }

        @Override
        public void onExecutionStart() {

        }

        @Override
        public void onExecutionFinish(@Nullable DJIError djiError) {

        }
    };


    public WaypointMissionOperator WaypointMissionOperator() {
        if (istance == null)
            istance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        return istance;
    }


    final Handler handler = new Handler() {
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1: {
                    String p_lat = decimalFormat.format(aircraft_lat);
                    String p_lng = decimalFormat.format(aircraft_lng);
                    aircraft_msg.setText("经纬度：" + p_lat + "," + p_lng + "\n高度：" + altitude + "\n电池电量"
                            + battary_left + "%\n云台角度" + pitch + "," + yaw + "," + roll + "\n卫星数" + sat_num);
                    break;
                }
            }
        }
    };

    protected BroadcastReceiver reciver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            get_aircraft_connect();

        }
    };


    private void get_aircraft_connect() {
        BaseProduct product = DemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            controller = ((Aircraft) product).getFlightController();
        }
        if (controller != null) {
            controller.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(@NonNull FlightControllerState flightControllerState) {
                    aircraft_lat = flightControllerState.getAircraftLocation().getLatitude();
                    aircraft_lng = flightControllerState.getAircraftLocation().getLongitude();
                    altitude = flightControllerState.getAircraftLocation().getAltitude();

                    sat_num = flightControllerState.getSatelliteCount();
                    update_fly_marker();
                }
            });

            get_battary(product);
            get_gimbal(product);
            get_picture(product);
        }
    }

    private void get_battary(BaseProduct product) {
        product.getBattery().setStateCallback(new BatteryState.Callback() {
            @Override
            public void onUpdate(BatteryState batteryState) {
                battary_left = batteryState.getChargeRemainingInPercent();
            }
        });
    }

    private void get_gimbal(BaseProduct product) {
        product.getGimbal().setStateCallback(new GimbalState.Callback() {
            @Override
            public void onUpdate(@NonNull GimbalState gimbalState) {
                pitch = gimbalState.getAttitudeInDegrees().getPitch();
                yaw = gimbalState.getAttitudeInDegrees().getYaw();
                roll = gimbalState.getAttitudeInDegrees().getRoll();
            }
        });
    }

    private void get_picture(BaseProduct product) {
        if (null != m_picture)
            m_picture.setSurfaceTextureListener(this);
        if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
            VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(m_viderfeeder);

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mMapView.onSaveInstanceState(outState);
    }


    protected void onResume() {
        super.onResume();
        get_aircraft_connect();
        mMapView.onResume();
    }

    protected void onDestroy() {
        mMapView.onDestroy();
        unregisterReceiver(reciver);
        removeListener();
        super.onDestroy();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        if (m_Codec == null)
            m_Codec = new DJICodecManager(this, surfaceTexture, i, i1);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        if (m_Codec != null) {
            m_Codec.cleanSurface();
            m_Codec = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.takephoto: {
                take_photo();
                break;
            }
            case R.id.switchmode: {
                switch_camera_mode(mode);

                break;
            }
            case R.id.back: {
//                mMapView.setScaleX(3f);
//                mMapView.setScaleY(3f);
//                m_picture.setVisibility(View.INVISIBLE);
                update_fly_marker();
                move_cam();
                break;
            }
            case R.id.addpoint: {
                isadd = !isadd;
                if (isadd) {
                    waypoint_add.setText("退出添加");
                } else {
                    waypoint_add.setText("添加锚点");
                    waypoint_setting();
                }
                break;
            }
            case R.id.clearpoint: {

                runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                aMap.clear();
                            }
                        }
                );
                update_fly_marker();
                waypoint_list.clear();
                waypointMissionBuilder.waypointList(waypoint_list);
                i = 0;
                break;
            }

            case R.id.upload: {
                waypoint_upload_mission();
                break;
            }
            case R.id.waypointstart: {
//                waypoint_upload_mission();
                waypoint_start_mission();
                break;
            }
            case R.id.waypointstop: {
                waypoint_stop_mission();
                break;
            }
        }

    }


    private void switch_camera_mode(Boolean mode1) {
        Camera camera = DemoApplication.getCameraInstance();
        if (mode1) {
            camera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null) {
                        mode = false;
                        Toast.makeText(main.this, "模式变换成功为拍照", Toast.LENGTH_SHORT).show();
                    } else
                        Toast.makeText(main.this, djiError.getDescription(), Toast.LENGTH_LONG).show();

                }
            });
        } else {
            camera.setMode(SettingsDefinitions.CameraMode.RECORD_VIDEO, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null) {
                        mode = true;
                        Toast.makeText(main.this, "模式变换成功为录像", Toast.LENGTH_SHORT).show();
                    } else
                        Toast.makeText(main.this, djiError.getDescription(), Toast.LENGTH_LONG).show();

                }
            });
        }
    }

    private void take_photo() {
        final Camera camera = DemoApplication.getCameraInstance();
        if (camera != null) {
            SettingsDefinitions.ShootPhotoMode mode = SettingsDefinitions.ShootPhotoMode.SINGLE;
            camera.setShootPhotoMode(mode, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null) {
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        if (djiError == null)
                                            Toast.makeText(main.this, "拍照成功", Toast.LENGTH_SHORT).show();
                                        else
                                            Toast.makeText(main.this, djiError.getDescription(), Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }, 2000);
                    } else
                        Toast.makeText(main.this, djiError.getDescription(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void startRecord() {
        Camera camera = DemoApplication.getCameraInstance();
        camera.startRecordVideo(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null)
                    Toast.makeText(main.this, "开始录像", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(main.this, djiError.getDescription(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void stopRecord() {
        Camera camera = DemoApplication.getCameraInstance();
        camera.stopRecordVideo(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null)
                    Toast.makeText(main.this, "停止录像", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(main.this, djiError.getDescription(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void move_cam() {
        LatLng latLng = new LatLng(aircraft_lat, aircraft_lng);
        float ZoomLevel = 18.0f;
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, ZoomLevel);
        aMap.moveCamera(cameraUpdate);
    }

    @Override
    public void onMapLoaded() {

    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {

    }

    @Override
    public void onInfoWindowClick(Marker marker) {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }


    private class move_to extends Thread {
        public void run() {
            while (sat_num < 9) {
                try {
                    sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            move_cam();
        }
    }

    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    private void update_fly_marker() {
        LatLng latLng = new LatLng(aircraft_lat, aircraft_lng);
        final MarkerOptions markerOption = new MarkerOptions();
        markerOption.position(latLng);
//        markerOption.title("西安市").snippet("西安市：34.341568, 108.940174");

//        markerOption.draggable(true);//设置Marker可拖动
        markerOption.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));
        // 将Marker设置为贴地显示，可以双指下拉地图查看效果
//        markerOption.setFlat(true);//设置marker平贴地图效果
        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              if (fly_mark != null)
                                  fly_mark.remove();
                              if (checkGpsCoordination(aircraft_lat, aircraft_lng))
                                  fly_mark = aMap.addMarker(markerOption);
                          }
                      }
        );
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (isadd) {
            markpoint_onmap(latLng);
            Waypoint waypoint = new Waypoint(latLng.latitude, latLng.longitude, waypoint_alt);
            if (waypointMissionBuilder == null) {
                waypointMissionBuilder = new WaypointMission.Builder();
                waypoint_list.add(waypoint);
                waypointMissionBuilder.waypointList(waypoint_list).waypointCount(waypoint_list.size());
            } else {
                waypoint_list.add(waypoint);
                waypointMissionBuilder.waypointList(waypoint_list).waypointCount(waypoint_list.size());
            }
        }
    }

    private void markpoint_onmap(LatLng latLng) {

        MarkerOptions mark_option = new MarkerOptions();
        mark_option.position(latLng);
        mark_option.draggable(true);
        mark_option.title("" + ++i);
        mark_option.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));


        Marker marker = aMap.addMarker(mark_option);
        marker.showInfoWindow();
        markers.put(markers.size(), marker);
    }


    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    private void waypoint_setting() {
        if (waypointMissionBuilder == null) {
            waypointMissionBuilder = new WaypointMission.Builder()
                    .finishedAction(WaypointMissionFinishedAction.NO_ACTION)
                    .headingMode(WaypointMissionHeadingMode.AUTO)
                    .autoFlightSpeed(3.0f)
                    .maxFlightSpeed(3.0f)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        } else {
            waypointMissionBuilder.finishedAction(WaypointMissionFinishedAction.NO_ACTION)
                    .headingMode(WaypointMissionHeadingMode.AUTO)
                    .autoFlightSpeed(3.0f)
                    .maxFlightSpeed(3.0f)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        }
        if ((waypointMissionBuilder.getWaypointList().size()) > 0) {
            for (int i = 0; i < waypointMissionBuilder.getWaypointList().size(); i++) {
                waypointMissionBuilder.getWaypointList().get(i).altitude = 25.0f;
            }
        }

        DJIError error = WaypointMissionOperator().loadMission(waypointMissionBuilder.build());
        if (error == null) {
//            Toast.makeText(main.this, "设置完成", Toast.LENGTH_SHORT).show();
            waypoint_upload_mission();
        } else {
            Toast.makeText(main.this, "设置失败" + error.getDescription(), Toast.LENGTH_SHORT).show();
        }
    }

    private void waypoint_upload_mission() {
        WaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
//                if (djiError == null) {
//                    Toast.makeText(main.this, "上传完成", Toast.LENGTH_SHORT).show();
//
//                } else {
//                    Toast.makeText(main.this, "上传失败" + djiError.getDescription(), Toast.LENGTH_SHORT).show();
//                    WaypointMissionOperator().retryUploadMission( null );
//                }

                if (djiError == null) {
                    setResultToToast( "任务已经上传完成!" );
//                    new start_in_10_s().start();
                } else {
                    setResultToToast( "任务上传失败: " + djiError.getDescription() + "  请重新设置." );
                    WaypointMissionOperator().retryUploadMission( null );
                }
            }
        });
    }


    private void waypoint_start_mission() {
        WaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
//                if (djiError == null) {
//                    Toast.makeText(main.this, "开始飞行", Toast.LENGTH_SHORT).show();
//
//                } else {
//                    Toast.makeText(main.this, "执行失败" + djiError.getDescription(), Toast.LENGTH_SHORT).show();
//                }

                setResultToToast( "开始执行任务: " + (djiError == null ? " " : djiError.getDescription()) );
            }
        });
    }

    private void waypoint_stop_mission() {
        WaypointMissionOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
//                if (djiError == null) {
//                    Toast.makeText(main.this, "停止飞行", Toast.LENGTH_SHORT).show();
//
//                } else {
//                    Toast.makeText(main.this, "执行失败" + djiError.getDescription(), Toast.LENGTH_SHORT).show();
//                }

                setResultToToast( "飞行任务停止" + (djiError == null ? " " : djiError.getDescription()) );
            }
        });
    }

    private void setResultToToast(final String string) {
        main.this.runOnUiThread( new Runnable() {
            @Override
            public void run() {
                Toast.makeText( main.this, string, Toast.LENGTH_SHORT ).show();
            }
        } );
    }
}
