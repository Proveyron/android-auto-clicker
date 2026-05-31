package com.example.autoclicker;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

public class AutoClickerService extends AccessibilityService {

    private WindowManager windowManager;
    private View floatingControl;
    private View floatingTarget;
    private WindowManager.LayoutParams controlParams;
    private WindowManager.LayoutParams targetParams;

    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRunning = false;
    private Runnable clickRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (floatingControl == null) {
            showFloatingViews();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void showFloatingViews() {
        LayoutInflater inflater = LayoutInflater.from(this);
        floatingControl = inflater.inflate(R.layout.floating_control, null);
        floatingTarget = inflater.inflate(R.layout.floating_target, null);

        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        controlParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        controlParams.gravity = Gravity.TOP | Gravity.START;
        controlParams.x = 100;
        controlParams.y = 100;

        targetParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        targetParams.gravity = Gravity.CENTER;

        windowManager.addView(floatingControl, controlParams);
        windowManager.addView(floatingTarget, targetParams);

        setupClickListeners();
        setupDragListener(floatingControl, controlParams);
        setupDragListener(floatingTarget, targetParams);
    }

    private void setupClickListeners() {
        ImageButton btnPlayStop = floatingControl.findViewById(R.id.btnPlayStop);
        btnPlayStop.setOnClickListener(v -> {
            if (isRunning) {
                stopAutoClick();
                btnPlayStop.setImageResource(android.R.drawable.ic_media_play);
            } else {
                startAutoClick();
                btnPlayStop.setImageResource(android.R.drawable.ic_media_pause);
            }
        });

        floatingControl.findViewById(R.id.btnClose).setOnClickListener(v -> {
            stopAutoClick();
            if (floatingControl != null) windowManager.removeView(floatingControl);
            if (floatingTarget != null) windowManager.removeView(floatingTarget);
            floatingControl = null;
            floatingTarget = null;
        });
    }

    private void setupDragListener(View view, WindowManager.LayoutParams params) {
        view.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(view, params);
                        return true;
                }
                return false;
            }
        });
    }

    private void startAutoClick() {
        isRunning = true;
        clickRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    performClick();
                    handler.postDelayed(this, 3000);
                }
            }
        };
        handler.post(clickRunnable);
    }

    private void stopAutoClick() {
        isRunning = false;
        if (clickRunnable != null) {
            handler.removeCallbacks(clickRunnable);
        }
    }

    private void performClick() {
        int[] location = new int[2];
        floatingTarget.getLocationOnScreen(location);
        int x = location[0] + floatingTarget.getWidth() / 2;
        int y = location[1] + floatingTarget.getHeight() / 2;

        Path path = new Path();
        path.moveTo(x, y);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 10));
        dispatchGesture(builder.build(), null, null);
    }

    @Override
    public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}
}
