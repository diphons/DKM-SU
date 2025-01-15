package com.diphons.dkmsu.ui.services;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.widget.ImageViewCompat;

import com.diphons.dkmsu.ui.store.SpfConfig;
import com.diphons.dkmsu.ui.util.Utils;
import com.diphons.dkmsu.R;

/**
 * Created by brianplummer on 9/12/15.
 */
public class FPSTouchListener implements View.OnTouchListener {

    private float initialTouchX;
    private float initialTouchY;

    private final WindowManager.LayoutParams params;
    private final WindowManager windowManager;
    private final GestureDetector gestureDetector;
    int clickCount = 0;
    /*variable for storing the time of first click*/
    long startTime;
    /* variable for calculating the total time*/
    long vstart;
    Context context;
    public FPSTouchListener(WindowManager.LayoutParams params,
                               WindowManager windowManager, GestureDetector gestureDetector) {
        this.windowManager = windowManager;
        this.params = params;
        this.gestureDetector = gestureDetector;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        context = v.getContext();
        gestureDetector.onTouchEvent(event);
        SharedPreferences prefs = context.getSharedPreferences(SpfConfig.SETTINGS, Context.MODE_PRIVATE);
        int getMode = prefs.getInt(SpfConfig.MONITOR_MINI_MODE, 0);
        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                prefs.edit().putInt(SpfConfig.MONITOR_MINI_INITIAL_X, params.x).apply();
                prefs.edit().putInt(SpfConfig.MONITOR_MINI_INITIAL_Y, params.y).apply();
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();

                if (getMode == 0)
                    vstart = System.currentTimeMillis();

                break;
            case MotionEvent.ACTION_UP:
                if (getMode == 2) {
                    clickCount++;

                    if (clickCount==1) {
                        startTime = System.currentTimeMillis();
                    } else if(clickCount == 2) {
                        long duration =  System.currentTimeMillis() - startTime;
                        if(duration <= 500) {
                            prefs.edit().putBoolean(SpfConfig.MONITOR_MINI_EXPAND, !prefs.getBoolean(SpfConfig.MONITOR_MINI_EXPAND, false)).apply();
                            clickCount = 0;
                        } else {
                            clickCount = 1;
                            startTime = System.currentTimeMillis();
                        }
                        break;
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                params.x = prefs.getInt(SpfConfig.MONITOR_MINI_INITIAL_X, 0) + (int) (event.getRawX() - initialTouchX);
                params.y = prefs.getInt(SpfConfig.MONITOR_MINI_INITIAL_Y, 0) + (int) (event.getRawY() - initialTouchY);
                windowManager.updateViewLayout(v, params);
                break;
        }
        return false;
    }

}
