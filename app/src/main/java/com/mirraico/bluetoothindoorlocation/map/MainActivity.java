package com.mirraico.bluetoothindoorlocation.map;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.fengmap.android.map.FMMap;
import com.fengmap.android.map.FMMapView;
import com.fengmap.android.map.event.OnFMMapInitListener;

import com.mirraico.bluetoothindoorlocation.BaseActivity;
import com.mirraico.bluetoothindoorlocation.beacon.BeaconService;
import com.mirraico.bluetoothindoorlocation.R;
import com.mirraico.bluetoothindoorlocation.info.InfoThread;
import com.mirraico.bluetoothindoorlocation.network.SendThread;
import com.mirraico.bluetoothindoorlocation.network.TCPConnection;
import com.mirraico.bluetoothindoorlocation.sensor.PedometerService;


public class MainActivity extends BaseActivity {

    private static String TAG = MainActivity.class.getSimpleName();

    //地图
    private FMMapView mapView;
    private FMMap map;
    private String mapId = "1561101080390313";

    //服务器设定
    private String serverIp = "123.207.9.36";
    private int serverPort = 8888;

    private BeaconService beaconService; //beacon服务
    Intent pedometerServiceIntent; //计步服务

    //Handler，用于更新地图、状态显示
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //TODO: 更新地图、状态显示
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Log.e(TAG, "CREATE MAP");
        //创建并显示地图
        mapView = (FMMapView) findViewById(R.id.mapview);
        map = mapView.getFMMap();
        map.openMapById(mapId);
        //地图事件回调
        map.setOnFMMapInitListener(new OnFMMapInitListener() {
            @Override
            public void onMapInitSuccess(String path) {}
            @Override
            public void onMapInitFailure(String path, int errCode) {}
        });

        //网络连接
        TCPConnection conn = TCPConnection.instance();
        conn.setHandler(handler);
        conn.setServerAddr(serverIp, serverPort);
        conn.connect();

        //初始化发送队列
        SendThread.instance().start();

        //初始化信息收集队列
        InfoThread.instance().start();

        //启动beacon服务
        beaconService = new BeaconService();
        beaconService.initService(this);

        //启动计步服务
        pedometerServiceIntent = new Intent(this, PedometerService.class);
        startService(pedometerServiceIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconService.destory();
        stopService(pedometerServiceIntent);
    }
}
