package com.example.hankwu.hometodolist_detectversion;

import android.app.Activity;
import android.content.ContentResolver;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.view.Window;
import android.view.WindowManager;

/**
 * Created by HankWu on 2016/10/16.
 */
public class BrightnessController {
    //Variable to store brightness value
    private int brightness;
    //Content resolver used as a handle to the system's settings
    private ContentResolver cResolver;
    //Window object, that will store a reference to the current window
    private Window window;
    private int mode = 0;
    private Object mLock = new Object();
    private static HandlerThread mHandlerThread = null;
    private static Handler mHandler = null;
    private static boolean bStop = false;
    private Activity mActivity = null;
    private int mLightTime = 1000;



    public boolean isDark() {
        return mode == -1;
    }

    public void setLightTime(int time) {
        mLightTime = time;
    }

    public BrightnessController(Activity act) {
        mActivity = act;
        cResolver = mActivity.getContentResolver();
        window = mActivity.getWindow();

        mHandlerThread = new HandlerThread("BrightnessController");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mHandler.post(beLightAmoment);
    }

    public void release() {
        bStop = true;
        synchronized (mLock) {
            mLock.notifyAll();
        }
        mHandler.removeCallbacks(beLightAmoment);
        mHandlerThread.quit();
    }

    Runnable beLightAmoment = new Runnable() {
        @Override
        public void run() {
            beDark();

            while(!bStop) {
                synchronized (mLock) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if(bStop) {
                        return;
                    }
                }
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        beLight();
                    }
                });
                try {
                    Thread.sleep(mLightTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        beDark();
                    }
                });
            }
        }
    };


    public void beDark() {
        //Set the system brightness using the brightness variable value
        Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, 0);
        //Get the current window attributes
        WindowManager.LayoutParams layoutpars = window.getAttributes();
        //Set the brightness of this window
        layoutpars.screenBrightness = -1f;
        //Apply attribute changes to this window
        window.setAttributes(layoutpars);
        mode = -1;
    }


    public void lightThenDark() {
        synchronized (mLock) {
            mLock.notifyAll();
        }
    }



    public void beLight() {
        //Set the system brightness using the brightness variable value
        Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, 255);
        //Get the current window attributes
        WindowManager.LayoutParams layoutpars = window.getAttributes();
        //Set the brightness of this window
        layoutpars.screenBrightness = 1f;
        //Apply attribute changes to this window
        window.setAttributes(layoutpars);
        mode = 1;
    }

}
