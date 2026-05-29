package com.diphons.dkmsu.ui.services;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import com.diphons.dkmsu.ui.store.SpfConfig;

public class FPSTouchListener implements View.OnTouchListener {

    private float initialTouchX;
    private float initialTouchY;

    private final WindowManager.LayoutParams params;
    private final WindowManager windowManager;
    private final GestureDetector gestureDetector;

    private long lastTapTime = 0;
    private static final long DOUBLE_TAP_DELAY = 300L;

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
                break;

            case MotionEvent.ACTION_UP:
                if (getMode == 2) {
                    handleDoubleTap(prefs);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                params.x = prefs.getInt(SpfConfig.MONITOR_MINI_INITIAL_X, 0) + (int) (event.getRawX() - initialTouchX);
                if (getMode == 1)
                    params.y = 0;
                else
                    params.y = prefs.getInt(SpfConfig.MONITOR_MINI_INITIAL_Y, 0) + (int) (event.getRawY() - initialTouchY);
                windowManager.updateViewLayout(v, params);
                break;
        }
        return false;
    }

    private void handleDoubleTap(SharedPreferences prefs) {
        long currentTime = System.currentTimeMillis();

        if (lastTapTime != 0 && (currentTime - lastTapTime) <= DOUBLE_TAP_DELAY) {
            boolean currentExpandState = prefs.getBoolean(SpfConfig.MONITOR_MINI_EXPAND, false);
            prefs.edit().putBoolean(SpfConfig.MONITOR_MINI_EXPAND, !currentExpandState).apply();
            lastTapTime = 0;
        } else {
            lastTapTime = currentTime;
        }
    }
}