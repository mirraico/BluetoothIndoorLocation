package com.mirraico.bluetoothindoorlocation.sensor;

public class SensorInfo {
    public double ud;
    public double ns;
    public double we;
    public double angle;

    public SensorInfo(double ud, double ns, double we, double angle) {
        this.ud = ud;
        this.ns = ns;
        this.we = we;
        this.angle = angle;
    }
}
