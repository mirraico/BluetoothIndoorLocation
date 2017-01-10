package com.mirraico.bluetoothindoorlocation.sensor;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.mirraico.bluetoothindoorlocation.info.InfoThread;

public class PedometerService extends Service implements StepListener {

    private static String TAG = PedometerService.class.getSimpleName();

    private static InfoThread infoThread; //信息收集队列实例

    private SensorChangeListener sensorChangeListener;
    private SensorManager sensorManager;
    private Sensor accelerateSensor;
    private Sensor magneticSensor;
    private Sensor linearAcceleratorSensor;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        infoThread = InfoThread.instance();

        //获取传感器的服务，初始化传感器
        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        sensorChangeListener = new SensorChangeListener(this);
        accelerateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        linearAcceleratorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        //注册传感器，注册监听器
        //在我手机上测试UI是最精准的，太快太慢都不好
        sensorManager.registerListener(sensorChangeListener, accelerateSensor, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(sensorChangeListener, magneticSensor, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(sensorChangeListener, linearAcceleratorSensor, SensorManager.SENSOR_DELAY_UI);
        Log.e(TAG, "REGISTER SENSOR LISTENER SUCCESSFULLY");
    }

    //可以发送时回调，发送消息
    @Override
    public void onSend(String msg) {
        //Log.e(TAG, "ONSEND: " + msg);
        Message sendMsg = Message.obtain();
        Bundle data = new Bundle();
        data.putInt("type", InfoThread.INFO_SENSOR);
        data.putString("sensors", msg);
        sendMsg.setData(data);
        infoThread.getHandler().sendMessage(sendMsg); //发送给信息收集队列
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(null != sensorManager){
            sensorManager.unregisterListener(sensorChangeListener);
        }
    }
}
