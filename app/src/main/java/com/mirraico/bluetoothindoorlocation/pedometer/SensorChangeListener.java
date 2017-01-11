package com.mirraico.bluetoothindoorlocation.pedometer;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SensorChangeListener implements SensorEventListener {

    private static String TAG = SensorChangeListener.class.getSimpleName();

    /* 以下部分用于计步 */
    //存放三轴加速度数据
    private float[] acceleratorValues = new float[3];
    //是否上升
    private boolean isUp = false;
    //上一点的状态，是否上升
    private boolean lastIsUp = false;
    //持续上升次数
    private int continueUpCount = 0;
    //上一点的持续上升次数，为了记录波峰的上升次数(在下降时使用)
    private int lastContinueUpCount = 0;
    //波峰值
    private float peakValue = 0;
    //波谷值
    private float valleyValue = 0;
    //此次波峰的时间
    private long timeOfThisPeak = 0;
    //上次波峰的时间
    private long timeOfLastPeak = 0;
    //当前的时间
    private long timeOfNow = 0;
    //当前传感器的值
    private float sensorValue = 0;
    //上次传感器的值
    private float lastSensorValue = 0;
    //峰谷阀值，该值动态计算，大于该值才记为一步，初始2.0
    private float thresholdValue = (float) 2.0;
    //能够纳入峰谷阀值计算的阀值，大于该值的峰谷差值才参与上述峰谷阀值的计算
    private final float judgeValue = (float) 1.3;
    //用于存放计算峰谷阀值的峰谷差值
    private final int arrayNum = 4;
    private float[] thresholdArray = new float[arrayNum];
    private int arrayCount = 0;
    //步数累计，用于过滤无意义抖动
    private int stepCount = 0;
    //已经在行走的标记
    private boolean stepFlag = false;

    /* 以下部分用于sensor采集并处理加速度 */
    //存放三轴地磁强度数据
    private float[] magneticValues = new float[3];
    //存放三轴线性加速度数据
    private float[] linearAcceleratorValues = new float[3];

    //存放待发送的数据，从静止到走动START_STEP次一发，走动中2次一发
    private final static int START_STEP = 6;
    private PedometerData[] pedometerDatas = new PedometerData[START_STEP];
    private int sendCnt = 0;

    //回调接口
    private StepListener stepListener;

    //构造函数，传递回调接口进来
    public SensorChangeListener(StepListener stepListener) {
        this.stepListener = stepListener;
    }

    /*
     * 原理
     * 1.波峰波谷的判定(连续上升、上升阀值等)
     * 2.峰谷差值需要大于峰谷阀值
     * 3.波峰时间差需要大于某个值
     * 4.峰谷差值是动态改变的
     *
     *
     * 可以调节的参数
     * 1.波峰判定中的连续上升次数lastContinueUpCount  2
     * 2.波峰判定中，没达到连续上升次数但可能是一次很大的上升值，判定该上升值的阀值  20
     * 3.波峰时间差判定  250ms
     * 4.计步累计启动步数  5
     * 5.参与峰谷阀值计算的阀值judgeValue  1.3
     * 6.重新计算峰谷阀值的峰谷差值累计个数arrayNum  4
     * 7.梯度化阀值中的梯度值和阀值设定averageValue()
     *   多判断了就尝试提高阀值，少判断了就尝试降低阀值
         测试走路、跑步、手机位置等多种场景，设定好不同场景的梯度，再设定阀值
     */



    /*
     * 注册服务后一直会调用这个函数(sensor是一直会变化的)
     * 如果是TYPE_ACCELEROMETER数据先对三轴数据进行平方和开根号的处理
     * 然后调用DetectorNewStep检测步子
     * 检测到步子后会利用其它数据一起计算一些信息，并回调onSend()函数
     * 如果是其它数据仅做收集
     */
    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                acceleratorValues = event.values.clone();
                sensorValue = (float) Math.sqrt(acceleratorValues[0] * acceleratorValues[0]
                        + acceleratorValues[1] * acceleratorValues[1] + acceleratorValues[2] * acceleratorValues[2]);
                detectNewStep(sensorValue);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magneticValues = event.values.clone();
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                linearAcceleratorValues = event.values.clone();
                break;
            default:
                break;
        }
    }

    /*
     * 检测步子，并开始计步
     * 1.传入sersor中的数据
     * 2.如果检测到了波峰，并且符合时间差以及阈值的条件，则判定为1步
     * 3.符合时间差条件，波峰波谷差值大于initialValue，则将该差值纳入阈值的计算中
     */
    public void detectNewStep(float sensorValue) {
        if (lastSensorValue == 0) { //第一次值
            lastSensorValue = sensorValue;
        } else {
            if (detectPeak(sensorValue, lastSensorValue)) { //如果检测到了波峰
                timeOfLastPeak = timeOfThisPeak;
                timeOfNow = System.currentTimeMillis();
                if (timeOfNow - timeOfLastPeak >= 250 //过滤短时间的连续波峰
                        && (peakValue - valleyValue >= thresholdValue) //峰谷阀值判定，峰谷差值大于该阀值才有效
                        ) {
                    timeOfThisPeak = timeOfNow;

                    //超过3步开始计步，防止无意义抖动，前3步会在第3步的时候一起显示，同时3步一起发送
                    if(detectValidStep()) {
                        pedometerDatas[sendCnt] = new PedometerData(sendCnt + 1, timeOfNow - timeOfLastPeak);
                        pedometerDatas[sendCnt].pushSensorData(generateSensorData());
                        sendCnt += 1;
                        Log.e(TAG, "STEP COUNT: " + sendCnt);
                    } else {
                        sendCnt = 0;
                        stepFlag = false;
                        Log.e(TAG, "STEP COUNT: " + sendCnt);
                    }
                    //2次(行动中)或START_STEP次(静止)调用回调函数
                    if ((stepFlag && sendCnt == 2) || (!stepFlag && sendCnt == START_STEP)) {
                        stepFlag = true;
                        JSONArray pedometerArray = new JSONArray();
                        try {
                            for(int i = 0; i < sendCnt; i++) {
                                JSONObject jsonObject;
                                jsonObject = new JSONObject();
                                jsonObject.put("stepNo", pedometerDatas[i].getStepNo());
                                jsonObject.put("timeDiff", pedometerDatas[i].getTimeDiff());
                                JSONArray sensorArray = new JSONArray();
                                SensorData[] sensorDatas = pedometerDatas[i].getSensorArray();
                                for(int j = 0; j < sensorDatas.length; j++) {
                                    JSONObject sensorObject = new JSONObject();
                                    sensorObject.put("azimuthAngle", sensorDatas[j].getAngle());
                                    sensorObject.put("weAcce", sensorDatas[j].getWE());
                                    sensorObject.put("nsAcce", sensorDatas[j].getNS());
                                    sensorObject.put("udAcce", sensorDatas[j].getUD());
                                    sensorArray.put(sensorObject);
                                }
                                jsonObject.put("sensorInfo", sensorArray);
                                pedometerArray.put(jsonObject);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        //Log.e(TAG, "ONSEND: " + sensorsArray.toString());
                        stepListener.onSend(pedometerArray.toString());
                        sendCnt = 0;
                    }
                }
                if (timeOfNow - timeOfLastPeak >= 250
                        && (peakValue - valleyValue >= judgeValue) //参与峰谷阀值计算的阀值，峰谷差值大于该阀值才参与峰谷阀值的计算
                        ) {
                    timeOfThisPeak = timeOfNow; //即使没过阀值，但过了修正阀值这个波峰就可以被记录
                    thresholdValue = calculateThreshold(peakValue - valleyValue); //峰谷阀值计算
                }
            }
            lastSensorValue = sensorValue;
        }
    }

    /*
     * 检测波峰
     * 以下四个条件判断为波峰：
     * 1.目前点为下降的趋势：isDirectionUp为false
     * 2.之前的点为上升的趋势：lastStatus为true
     * 3.到波峰为止，持续上升大于等于2次
     * 4.波峰值大于20
     * 记录波谷值
     * 1.观察波形图，可以发现在出现步子的地方，波谷的下一个就是波峰，有比较明显的特征以及差值
     * 2.所以要记录每次的波谷值，为了和下次的波峰做对比
     */
    public boolean detectPeak(float newValue, float oldValue) {
        lastIsUp = isUp; //记录上次状态
        if (newValue >= oldValue) { //本次上升
            isUp = true;
            continueUpCount++;
        } else { //本次下降
            lastContinueUpCount = continueUpCount;
            isUp = false;
            continueUpCount = 0;
        }

        if (!isUp && lastIsUp //波峰判定
                && (lastContinueUpCount >= 2 || oldValue >= 20) //防止抖动，持续上升了几次或一次上升了很大，才算作有效波峰
                ) {
            peakValue = oldValue; //波峰值记录
            return true; //该点是一个波峰
        } else if (!lastIsUp && isUp) { //波谷判定
            valleyValue = oldValue; //波谷值记录
            return false;
        } else { //正在持续上升或下降
            return false;
        }
    }

    /*
     * 1.连续记录3才开始计步
     * 2.例如记录的2步用户停住超过3秒，则前面的记录失效，下次从头开始
     * 3.连续记录了2步用户还在运动，之前的数据才有效
     */
    private boolean detectValidStep() {
        if (timeOfThisPeak - timeOfLastPeak < 3000) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * 阈值的计算
     * 1.通过合法的峰谷差值计算阈值
     * 2.记录4个值，存入tempValue[]数组中，累计4个值后进行计算
     * 3.在将数组传入函数averageValue中计算阈值
     */
    public float calculateThreshold(float value) {
        if (arrayCount < arrayNum) {
            thresholdArray[arrayCount++] = value;
            return thresholdValue; //刚开始的前4个累积过程中阀值不变
        } else {
            float newThreshold;
            newThreshold = averageValue(thresholdArray, arrayNum); //计算新阀值
            //计算用过的差值前移(不清空)
            for (int i = 1; i < arrayNum; i++) {
                thresholdArray[i-1] = thresholdArray[i];
            }
            thresholdArray[arrayNum-1] = value; //刚开始4个累计后，数组就永远是满的了，新的就直接放到数组最后
            return newThreshold;
        }
    }

    /*
     * 梯度化阈值
     * 1.计算数组的均值
     * 2.通过均值将阈值梯度化在一个范围里
     */
    public float averageValue(float value[], int n) {
        float ave = 0;
        for (int i = 0; i < n; i++) {
            ave += value[i];
        }
        ave = ave / n;

        //梯度化阀值的参数，还需要再调整
        //多判断了就尝试提高阀值，少判断了就尝试降低阀值
        //测试走路、跑步多场景，用梯度设定阀值
        if (ave >= 8)
            ave = (float) 4.3;
        else if (ave >= 7 && ave < 8)
            ave = (float) 3.3;
        else if (ave >= 4 && ave < 7)
            ave = (float) 2.3;
        else if (ave >= 3 && ave < 4)
            ave = (float) 2.0;
        else {
            ave = (float) 1.3;
        }
        return ave;
    }

    public SensorData generateSensorData() {

        float[] R = new float[9];
        float[] values = new float[3];
        SensorManager.getRotationMatrix(R, null, acceleratorValues, magneticValues);
        /**
         * values[0]对应手机绕Z轴的旋转弧度，也就是航向角
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

        //Log.e(TAG, "UD:" + a0 + " NS:" + a1 + " WE:" + a2 + " ANGLE:" + Math.toDegrees(values[0]) + " TIME:" + timeDiff);

        return new SensorData(values[0], a2, a1, a0);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}

