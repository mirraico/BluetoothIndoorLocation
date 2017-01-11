package com.mirraico.bluetoothindoorlocation.pedometer;

public class PedometerData {
    private int stepNo;
    private long timeDiff;
    private SensorData[] sensorArray = new SensorData[1];

    public int getStepNo() {
        return stepNo;
    }

    public long getTimeDiff() {
        return timeDiff;
    }

    public SensorData[] getSensorArray() {
        return sensorArray;
    }

    public PedometerData(int stepNo, long timeDiff) {
        this.stepNo = stepNo;
        this.timeDiff = timeDiff;
    }

    public void pushSensorData(SensorData sd) {
        sensorArray[0] = sd;
    }
}

