package com.rzr.pay;

import android.app.Application;
import android.content.Intent;

import com.rzr.pay.MainActivity;

public class SDKTesterApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
//        if (SSMPOSSDK.isRunningOnRemoteProcess(this))
//        {
//            return;
////            routeToMainActivity();
//        }

        // your code goes here
    }

    private void routeToMainActivity()
    {
        Intent intent = new Intent(this, MainActivity.class);

        this.startActivity(intent);
    }
}
