package com.mirraico.bluetoothindoorlocation.map;

import android.app.Application;
import com.fengmap.android.FMMapSDK;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        FMMapSDK.init(this, "8nDxjoveVeOIOMJ3eehu");
        super.onCreate();
    }
}
