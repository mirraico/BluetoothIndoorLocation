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
import com.mirraico.bluetoothindoorlocation.sensor.SensorCollection;


public class MainActivity extends BaseActivity {

    private static String TAG = "ibeacon";
    private FMMapView mapView;
    private FMMap map;
    private BeaconSearch bs;
    private SensorCollection sc;

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

        InfoThread.instance().start();

        bs = new BeaconSearch();
        bs.init(this);

        sc = new SensorCollection();
        sc.init(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bs.destory();
    }
}
