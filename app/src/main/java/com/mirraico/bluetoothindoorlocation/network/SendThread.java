package com.mirraico.bluetoothindoorlocation.network;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class SendThread extends Thread {

    private static SendThread sendThread;

    private static String TAG = "send";
    private static Handler mHandler;
    private final static Object mSync = new Object();
    private static TCPClient client;

    public static synchronized SendThread instance() {
        if ( sendThread == null ) {
            sendThread = new SendThread();
            client = TCPClient.instance();
        }
        return sendThread;
    }

    @Override
    public void run() {
        Log.i(TAG, "start send thread");

        Looper.prepare();
        synchronized (mSync) {
            mHandler = new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    client.send(msg.getData().getString("send"));
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
