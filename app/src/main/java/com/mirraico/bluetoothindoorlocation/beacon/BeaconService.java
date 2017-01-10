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

public class BeaconService {

    private static String TAG = BeaconService.class.getSimpleName();

    private static InfoThread infoThread; //信息收集队列实例

    private static final Region ALL_BEACONS_REGION = new Region("108", null, //beacon sdk的默认参数
            null, null);
    private BeaconManager beaconManager;

    public void initService(Activity activity) {
        infoThread = InfoThread.instance();
        //AprilL.enableDebugLogging(true);
        beaconManager = new BeaconManager(activity.getApplicationContext());
        beaconManager.setForegroundScanPeriod(200, 0); //扫描间隔200ms
        beaconManager.setRangingListener(new RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> list) {
                JSONArray jsonArray = new JSONArray();
                JSONObject jsonObject;
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
                        //Log.e(TAG, "MAC: " + list.get(i).getMacAddress() + " RSS: " + list.get(i).getRssi());
                    }
                    Message msg = Message.obtain();
                    Bundle data = new Bundle();
                    data.putInt("type", InfoThread.INFO_BEACON);
                    data.putString("beacons", jsonArray.toString());
                    msg.setData(data);
                    infoThread.getHandler().sendMessage(msg); //发送给信息收集队列
                }
            }
        });
        startService(); //启动beacon service
    }

    public void startService() {
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    //Log.e(TAG, "START BEACON SERVICE");
                    beaconManager.startRanging(ALL_BEACONS_REGION);
                    Log.e(TAG, "CONNECT TO BEACON SERVICE SUCCESSFULLY");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void destory() {
        try {
            beaconManager.stopRanging(ALL_BEACONS_REGION);
            beaconManager.disconnect();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
