package com.mirraico.bluetoothindoorlocation.map;

import android.app.Application;
import com.fengmap.android.FMMapSDK;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        //地图开发者密钥设定
        FMMapSDK.init(this, "8nDxjoveVeOIOMJ3eehu");
        super.onCreate();
    }
}
