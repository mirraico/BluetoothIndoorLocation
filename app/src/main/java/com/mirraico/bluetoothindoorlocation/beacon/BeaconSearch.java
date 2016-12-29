package com.mirraico.bluetoothindoorlocation.beacon;

import android.app.Activity;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.aprilbrother.aprilbrothersdk.Beacon;
import com.aprilbrother.aprilbrothersdk.BeaconManager;
import com.aprilbrother.aprilbrothersdk.BeaconManager.RangingListener;
import com.aprilbrother.aprilbrothersdk.Region;
import com.aprilbrother.aprilbrothersdk.utils.AprilL;
import com.mirraico.bluetoothindoorlocation.info.InfoThread;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class BeaconSearch {
    private static String TAG = "ibeacon";

    private static final Region ALL_BEACONS_REGION = new Region("108", null,
            null, null);

    private BeaconManager beaconManager;

    public void init(Activity activity) {
        AprilL.enableDebugLogging(true);
        beaconManager = new BeaconManager(activity.getApplicationContext());
        beaconManager.setForegroundScanPeriod(300, 0);
        beaconManager.setRangingListener(new RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> list) {
                JSONArray jsonArray = new JSONArray();
                JSONObject jsonObject = null;
                if(list != null && list.size() > 0) {
                    for(int i = 0; i < list.size(); i++) {
                        jsonObject = new JSONObject();
                        try {
                            jsonObject.put("MAC", list.get(i).getMacAddress());
                            jsonObject.put("RSS", list.get(i).getRssi());
                            jsonArray.put(jsonObject);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        //Log.e(TAG, list.get(i).getMacAddress() + " " + list.get(i).getRssi());
                    }
                }
                Message msg = Message.obtain();
                Bundle data = new Bundle();
                data.putInt("type", InfoThread.BEACON);
                data.putString("beacon", jsonArray.toString());
                msg.setData(data);
                InfoThread.getHandler().sendMessage(msg);
            }
        });
        connectToService();
    }

    public void destory() {
        try {
            Log.e(TAG, "disconnect to beacon service");
            beaconManager.stopRanging(ALL_BEACONS_REGION);
            beaconManager.disconnect();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void connectToService() {
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    Log.e(TAG, "connect to beacon service");
                    beaconManager.startRanging(ALL_BEACONS_REGION);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
