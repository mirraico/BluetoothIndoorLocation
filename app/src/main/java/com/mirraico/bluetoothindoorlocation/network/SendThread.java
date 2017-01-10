package com.mirraico.bluetoothindoorlocation.network;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class SendThread extends Thread {

    private static String TAG = SendThread.class.getSimpleName();

    private static SendThread sendThread; //单例模式

    private static TCPConnection conn; //TCP连接的实例
    private static Handler handler; //发送队列
    private final static Object sync = new Object(); //同步锁

    private SendThread() {}

    public static synchronized SendThread instance() {
        if(sendThread == null) {
            sendThread = new SendThread();
            conn = TCPConnection.instance();
        }
        return sendThread;
    }

    @Override
    public void run() {
        //Log.e(TAG, "START SEND THREAD");
        Looper.prepare();
        synchronized (sync) {
            handler = new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    Log.e(TAG, "TRY TO SEND(" + msg.getData().getString("data").length() + " BYTES): " + msg.getData().getString("data"));
                    int ret = conn.send(msg.getData().getString("data")); //发送队列中的数据
                    if(ret == -1) {
                        Log.e(TAG, "SERVER IS NOT CONNECTED");
                    } else {
                        Log.e(TAG, "SENDED " + ret + " BYTES");
                    }
                }
            };
            sync.notifyAll(); //准备好了就可以唤醒拿handler的函数了
        }
        Looper.loop();
    }

    public Handler getHandler() {
        synchronized (sync) {
            if(handler == null) {
                try {
                    sync.wait(); //handler还没好时阻塞
                } catch (InterruptedException e) {
                }
            }
            return handler;
        }
    }
}
