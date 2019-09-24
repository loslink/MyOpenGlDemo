package com.loslink.myopengldemo;

import android.app.Application;

public class MyApp extends Application{

    private static MyApp sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    public static MyApp getApplication(){
        return sInstance;
    }
}
