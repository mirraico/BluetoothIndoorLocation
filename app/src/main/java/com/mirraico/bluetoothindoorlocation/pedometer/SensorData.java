package com.mirraico.bluetoothindoorlocation.pedometer;

public class SensorData {
    private float azimuthAngle;
    private double weAcce;
    private double nsAcce;
    private double udAcce;

    public SensorData(float angle, double we, double ns, double ud) {
        this.azimuthAngle = angle;
        this.weAcce = we;
        this.nsAcce = ns;
        this.udAcce = ud;
    }

    public float getAngle() {
        return azimuthAngle;
    }
    public double getWE() {
        return weAcce;
    }
    public double getNS() {
        return nsAcce;
    }
    public double getUD() {
        return udAcce;
    }
}
