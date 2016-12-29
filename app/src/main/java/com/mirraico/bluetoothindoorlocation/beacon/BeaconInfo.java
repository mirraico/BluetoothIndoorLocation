package com.mirraico.bluetoothindoorlocation.beacon;

import java.util.ArrayList;
import java.util.List;

public class BeaconInfo {
    public String MAC;
    public List<Integer> RSS;

    public BeaconInfo(String MAC) {
        this.MAC = MAC;
        RSS = new ArrayList<>();
    }
}
