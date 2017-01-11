package com.mirraico.bluetoothindoorlocation.info;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class TimerThread extends Thread {
    @Override
    public void run() {
        super.run();
        Handler handler = InfoThread.instance().getHandler();
        while(true) {
            try {
                Thread.sleep(300);

                Message sendMsg = Message.obtain();
                Bundle data = new Bundle();
                data.putInt("type", InfoThread.INFO_TIMER);
                sendMsg.setData(data);
                handler.sendMessage(sendMsg);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
