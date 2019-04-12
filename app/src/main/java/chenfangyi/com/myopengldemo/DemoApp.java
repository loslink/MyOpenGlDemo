package chenfangyi.com.myopengldemo;

import android.app.Application;

/**
 * Created by chenfangyi on 17-9-29.
 */

public class DemoApp extends Application{

    private static DemoApp sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    public static DemoApp getApplication(){
        return sInstance;
    }
}
