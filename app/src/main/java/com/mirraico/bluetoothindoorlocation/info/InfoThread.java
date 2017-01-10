package com.mirraico.bluetoothindoorlocation.info;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.mirraico.bluetoothindoorlocation.beacon.BeaconData;
import com.mirraico.bluetoothindoorlocation.network.Protocol;
import com.mirraico.bluetoothindoorlocation.network.SendThread;
import com.mirraico.bluetoothindoorlocation.network.TCPConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InfoThread extends Thread {

    private static String TAG = InfoThread.class.getSimpleName();

    private static InfoThread infoThread; //单例模式
    private static SendThread sendThread; //发送队列实例

    private static Handler handler; //发送队列
    private final static Object sync = new Object(); //同步锁

    public final static int INFO_SENSOR = 0; //传感器类型数据
    public final static int INFO_BEACON = 1; //BEACON类型数据

    private Map<String, BeaconData> beaconList;
    private List<String> macList; //维护一个MAC列表，在有些beacon搜索不到时，从该列表查出哪个搜索不到了，并清除map中对应的BeaconData

    private InfoThread() {}

    public static synchronized InfoThread instance() {
        if ( infoThread == null ) {
            infoThread = new InfoThread();
            sendThread = SendThread.instance();
            infoThread.beaconList = new HashMap<>();
            infoThread.macList = new ArrayList<>();
        }
        return infoThread;
    }

    @Override
    public void run() {
        //Log.e(TAG, "START INFO THREAD");

        Looper.prepare();
        synchronized (sync) {
            handler = new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    Bundle data = msg.getData();
                    int type = data.getInt("type");
                    String json;
                    switch(type) {
                        case InfoThread.INFO_BEACON:
                            json = data.getString("beacons");
                            //Log.e(TAG, "JSON: " json);
                            try {
                                JSONArray jsonArray = new JSONArray(json);
                                for(int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject jsonBeacon = jsonArray.getJSONObject(i);
                                    String MAC = jsonBeacon.getString("MAC");
                                    int RSS = jsonBeacon.getInt("RSS");
                                    BeaconData beaconData = beaconList.get(MAC);
                                    if(beaconData == null) { //没有见过的beacon MAC就新建
                                        beaconData = new BeaconData(MAC);
                                        macList.add(MAC); //维护MAC列表
                                        beaconList.put(MAC, beaconData);
                                    }
                                    beaconData.pushRSS(RSS); //滑动处理RSS
                                    //Log.e(TAG, "MAC: " + MAC + " RSS: " + RSS);
                                }
                                if(jsonArray.length() < macList.size()) { //有MAC没有收到，需要移除
                                    String[] nowMAC = new String[jsonArray.length()];
                                    for(int i = 0; i < jsonArray.length(); i++) {
                                        nowMAC[i] = jsonArray.getJSONObject(i).getString("MAC");
                                    }
                                    for(int i = 0; i < macList.size(); i++) {
                                        boolean exist = false;
                                        String oriMAC = macList.get(i);
                                        for(int j = 0; j < nowMAC.length; j++) {
                                            if(oriMAC.equals(nowMAC[j])) {
                                                exist = true;
                                                break;
                                            }
                                        }
                                        if(!exist) {
                                            beaconList.remove(oriMAC);
                                            macList.remove(oriMAC);
                                        }
                                    }
                                }
                                /*
                                if(jsonArray.length() != macList.size() ||
                                        macList.size() != beaconList.size() ||
                                        jsonArray.length() != beaconList.size()
                                        ) { //调试用
                                    Log.e(TAG, "ERROR IN MACLIST MAINTENANCE");
                                }*/
                                Log.e(TAG, "RECV BEACON NUM: " + jsonArray.length() + ", MAC LIST SIZE: " + macList.size() + ", BEACON MAP SIZE: " + beaconList.size());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            break;
                        case InfoThread.INFO_SENSOR:
                            json = data.getString("sensors"); //这个json是合格的通信格式，准备直接发送
                            //Log.e(TAG, "RECV SENSORS: " + json);

                            JSONObject jsonObject;
                            try {
                                jsonObject = new JSONObject();
                                jsonObject.put("type", Protocol.TYPE_REQ);
                                jsonObject.put("isStep", true);
                                JSONArray rssisArray = new JSONArray();
                                for(Map.Entry<String, BeaconData> entry : beaconList.entrySet()) {
                                    BeaconData beaconData = entry.getValue();
                                    if(beaconData.isEmpty()) continue;
                                    String MAC = beaconData.getMAC();
                                    int avgRSS = beaconData.getAverageRSS(); //取RSS平均值

                                    JSONObject rssiObject = new JSONObject();
                                    rssiObject.put("MAC", MAC);
                                    rssiObject.put("RSS", avgRSS);
                                    rssisArray.put(rssiObject);
                                }
                                if(rssisArray.length() > 0) {
                                    jsonObject.put("hasRSS", true);
                                    jsonObject.put("rssis", rssisArray);
                                } else {
                                    jsonObject.put("hasRSS", false);
                                }
                                jsonObject.put("sensors", json);
                                //Log.e(TAG, "SEND JOSN: " + jsonObject.toString());
                                Message sendMsg = Message.obtain();
                                Bundle sendData = new Bundle();
                                sendData.putString("data", jsonObject.toString());
                                sendMsg.setData(sendData);
                                //把消息递交发送队列
                                sendThread.getHandler().sendMessage(sendMsg);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            break;
                    }
                }
            };
            sync.notifyAll(); //准备好了就可以唤醒拿handler的函数了
        }
        Looper.loop();
    }

    public Handler getHandler() {
        synchronized (sync) {
            if (handler == null) {
                try {
                    sync.wait(); //handler还没好时阻塞
                } catch (InterruptedException e) {
                }
            }
            return handler;
        }
    }
}
