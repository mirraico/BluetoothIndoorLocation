package com.mirraico.bluetoothindoorlocation.sensor;

import android.os.Bundle;
import android.os.Message;

import com.mirraico.bluetoothindoorlocation.info.InfoThread;

public class Pedometer implements Runnable {

    @Override
    public void run() {
        while(true) {
            try {
                Thread.sleep(1000);
                Message msg = Message.obtain();
                Bundle data = new Bundle();
                data.putInt("type", InfoThread.SEND);
                msg.setData(data);
                InfoThread.getHandler().sendMessage(msg);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
