package com.etag.ble;

import android.os.Handler;

public class HandlerHelper {
    private static Handler handler;
    static {
        handler = new Handler();
    }


    public static void postDelayed(Runnable r, long delayMillis){
        removeCallbacks(r);
        handler.postDelayed(r,delayMillis);
    }


    public static void removeCallbacks(Runnable r){
        handler.removeCallbacks(r);
    }

    public static void startTask(Runnable r){
        Thread thread=new Thread(r);
        thread.start();
    }
}
