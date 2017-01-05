package com.mirraico.bluetoothindoorlocation.info;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.mirraico.bluetoothindoorlocation.beacon.BeaconInfo;
import com.mirraico.bluetoothindoorlocation.network.TCPClient;
import com.mirraico.bluetoothindoorlocation.sensor.SensorInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class InfoThread extends Thread {

    public final static int SEND = 0;
    public final static int BEACON = 1;
    public final static int SENSOR = 2;
    public final static int TYPE_INFO = 14;

    private static InfoThread infoThread;

    private static String TAG = "info_tag";
    private static Handler mHandler;
    private final static Object mSync = new Object();

    private List<SensorInfo> sensorList;
    private Map<String, BeaconInfo> beaconList;

    public static synchronized InfoThread instance() {
        if ( infoThread == null ) {
            infoThread = new InfoThread();
            infoThread.sensorList = new ArrayList<>() ;
            infoThread.beaconList = new HashMap<>();
        }
        return infoThread;
    }

    @Override
    public void run() {
        Log.i(TAG, "start info thread");

        Looper.prepare();
        synchronized (mSync) {
            mHandler = new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    Bundle data = msg.getData();
                    int type = data.getInt("type");
                    String json;
                    switch(type) {
                        case InfoThread.BEACON:
                            json = data.getString("beacon");
                            //Log.e(TAG, json);
                            try {
                                JSONArray jsonList = new JSONArray(json);
                                for(int i = 0; i < jsonList.length(); i++) {
                                    JSONObject jsonObject = jsonList.getJSONObject(i);
                                    String MAC = jsonObject.getString("MAC");
                                    int RSS = jsonObject.getInt("RSS");
                                    BeaconInfo beaconInfo = beaconList.get(MAC);
                                    if(beaconInfo == null) {
                                        beaconInfo = new BeaconInfo(MAC);
                                        beaconList.put(MAC, beaconInfo);
                                    }
                                    beaconInfo.RSS.add(RSS);
                                    Log.e(TAG, MAC + ".RSS.size(): " + beaconInfo.RSS.size());
                                    //Log.e(TAG, MAC + " " + RSS);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            break;
                        case InfoThread.SENSOR:
                            json = data.getString("sensor");
                            //Log.e(TAG, json);
                            try {
                                JSONObject jsonObject = new JSONObject(json);
                                double ud = jsonObject.getDouble("ud");
                                double ns = jsonObject.getDouble("ns");
                                double we = jsonObject.getDouble("we");
                                double angle = jsonObject.getDouble("angle");
                                sensorList.add(new SensorInfo(ud, ns, we, angle));
                                Log.e(TAG, "sensorList.size(): " + sensorList.size());
                                //Log.e(TAG, ud + " " + ns + " " + we + " " + angle);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            break;
                        case InfoThread.SEND:
                            //Log.e(TAG, "SENDSEND");
                            JSONObject jsonObject;
                            try {
                                jsonObject = new JSONObject();
                                jsonObject.put("type", TYPE_INFO);
                                jsonObject.put("isStep", false);
                                JSONArray rssisArray = new JSONArray();
                                for(Map.Entry<String, BeaconInfo> entry : beaconList.entrySet()) {
                                    String MAC = entry.getKey();
                                    int avgRSS = 0;
                                    BeaconInfo bi = entry.getValue();
                                    if(bi.RSS.size() == 0) continue;
                                    for(int i = 0; i < bi.RSS.size(); i++) {
                                        avgRSS += bi.RSS.get(i);
                                    }
                                    avgRSS /= bi.RSS.size();
                                    bi.RSS.clear();
                                    JSONObject rssiObject = new JSONObject();
                                    rssiObject.put("MAC", MAC);
                                    rssiObject.put("RSS", avgRSS);
                                    rssisArray.put(rssiObject);
                                }
                                jsonObject.put("rssis", rssisArray);
                                //Log.e(TAG, jsonObject.toString());
                                TCPClient.instance().send(jsonObject.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            break;
                    }
                }
            };
            mSync.notifyAll();
        }
        Looper.loop();
    }

    public static Handler getHandler() {
        synchronized (mSync) {
            if (mHandler == null) {
                try {
                    mSync.wait();
                } catch (InterruptedException e) {
                }
            }
            return mHandler;
        }
    }
}
