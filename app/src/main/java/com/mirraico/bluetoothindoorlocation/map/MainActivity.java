package com.mirraico.bluetoothindoorlocation.map;

import android.os.Bundle;
import android.util.Log;
import com.fengmap.android.map.FMMap;
import com.fengmap.android.map.FMMapView;
import com.fengmap.android.map.event.OnFMMapInitListener;

import com.mirraico.bluetoothindoorlocation.BaseActivity;
import com.mirraico.bluetoothindoorlocation.beacon.BeaconSearch;
import com.mirraico.bluetoothindoorlocation.R;
import com.mirraico.bluetoothindoorlocation.info.InfoThread;
import com.mirraico.bluetoothindoorlocation.network.SendThread;
import com.mirraico.bluetoothindoorlocation.network.TCPClient;
import com.mirraico.bluetoothindoorlocation.sensor.Pedometer;
import com.mirraico.bluetoothindoorlocation.sensor.SensorCollection;


public class MainActivity extends BaseActivity {

    private static String TAG = "ibeacon";
    private FMMapView mapView;
    private FMMap map;
    private BeaconSearch bs;
    private SensorCollection sc;
    private Pedometer pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.e(TAG, "mainActivity");

        mapView = (FMMapView) findViewById(R.id.mapview);
        map = mapView.getFMMap();
        String bid = "1561101080390313";
        map.openMapById(bid);

        map.setOnFMMapInitListener(new OnFMMapInitListener() {
            @Override
            public void onMapInitSuccess(String path) {}
            @Override
            public void onMapInitFailure(String path, int errCode) {}
        });

        TCPClient.instance().connect("123.207.9.36", 8888);
        SendThread.instance().start();
        InfoThread.instance().start();

        bs = new BeaconSearch();
        bs.init(this);

        sc = new SensorCollection();
        sc.init(this);

        pd = new Pedometer();
        new Thread(pd).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bs.destory();
    }
}
