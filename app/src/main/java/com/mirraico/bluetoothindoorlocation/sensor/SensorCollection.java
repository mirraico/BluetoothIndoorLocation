package com.mirraico.bluetoothindoorlocation.sensor;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.mirraico.bluetoothindoorlocation.info.InfoThread;

import org.json.JSONException;
import org.json.JSONObject;

public class SensorCollection {

    private SensorManager sensorManager;
    private Sensor accelerateSensor;
    private Sensor magneticSensor;
    private Sensor linearAcceleratorSensor;
    private Sensor rotateVectorSensor;

    private static int interval = 0;
    public final static int MAX_INTERVAL = 24; //约300毫秒
    private static String TAG = "isensor";

    public void init(Activity activity) {

        Log.e(TAG, "sensor collection init");
        sensorManager = (SensorManager)activity.getSystemService(Context.SENSOR_SERVICE);
        accelerateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        linearAcceleratorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        rotateVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        sensorManager.registerListener(listener, magneticSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(listener, accelerateSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(listener, linearAcceleratorSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(listener, rotateVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private SensorEventListener listener = new SensorEventListener() {

        private float[] acceleratorValues = new float[3];
        private float[] magneticValues = new float[3];
        private float[] linearAcceleratorValues = new float[3];
        private float[] rotateVectorValues = new float[3];

        @Override
        public void onSensorChanged(SensorEvent event) {

            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    acceleratorValues = event.values.clone();
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    magneticValues = event.values.clone();
                    break;
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    linearAcceleratorValues = event.values.clone();
                    break;
                case Sensor.TYPE_ROTATION_VECTOR:
                    rotateVectorValues = event.values.clone();
                    break;
                default:
                    break;
            }

            float[] R = new float[9];
            float[] values = new float[3];
            SensorManager.getRotationMatrix(R, null, acceleratorValues, magneticValues);
            /**
             * values[0]对应手机绕Z轴的旋转弧度
             * values[1]对应手机绕X轴的旋转弧度
             * values[2]对应手机绕Y轴的旋转弧度
             *
             * values[0]取值范围是-180度到180度，
             * 其中+180(-180)度表示正南方向，
             *   0度表示正北方向
             * -90度表示正西方向， 90度表示正东方向
             */
            SensorManager.getOrientation(R, values);

            /**
             * 考虑翻转倾斜
             */
            double theta0 = values[0];
            double theta1 = values[1];
            double theta2 = values[2];

            double ax = acceleratorValues[0];
            double ay = acceleratorValues[1];
            double az = acceleratorValues[2];

            //对应y轴
            double y0 = -Math.sin(theta1);
            double y1 = Math.cos(theta1)*Math.cos(theta0);
            double y2 = Math.cos(theta1)*Math.sin(theta0);

            /**
             * 为避免出现NAN问题，作以下处理
             */
            if (theta1 == 0) theta1 = 0.0001;
            if (theta2 == 0) theta2 = 0.0001;

            double val1 = Math.tan(theta1);
            double val2 = Math.tan(theta2);
            double mul = -val1*val2;
            if (mul > 1 || mul < -1) {
                mul = mul > 0 ? 0.999 : -0.999;
            }

            //对应x轴
            double tmp = Math.acos(mul);
            double x0  = -Math.sin(theta2);
            double x1  = Math.cos(theta2)*Math.cos(theta0+tmp);
            double x2  = Math.cos(theta2)*Math.sin(theta0+tmp);

            //对应z轴
            double z0 = x2*y1 - x1*y2;
            double z1 = x0*y2 - x2*y0;
            double z2 = x1*y0 - x0*y1;

            //加速度 z轴,朝上
            double a0 = linearAcceleratorValues[0]*x0 + linearAcceleratorValues[1]*y0 +
                    linearAcceleratorValues[2]*z0;
            //北
            double a1 = linearAcceleratorValues[0]*x1 + linearAcceleratorValues[1]*y1 +
                    linearAcceleratorValues[2]*z1;
            //东
            double a2 = linearAcceleratorValues[0]*x2 + linearAcceleratorValues[1]*y2 +
                    linearAcceleratorValues[2]*z2;

            if (++interval == MAX_INTERVAL) {
                //Log.e(TAG, "ud:" + a0 + "  ns:" + a1 + "  we:" + a2 + "  angle:" + Math.toDegrees(values[0]));
                Message msg = Message.obtain();
                Bundle data = new Bundle();
                data.putInt("type", InfoThread.SENSOR);
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("ud", a0);
                    jsonObject.put("ns", a1);
                    jsonObject.put("we", a2);
                    jsonObject.put("angle", Math.toDegrees(values[0]));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                data.putString("sensor", jsonObject.toString());
                Log.e(TAG, jsonObject.toString());
                msg.setData(data);
                InfoThread.getHandler().sendMessage(msg);
                interval = 0;
            }

        }



        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
}
