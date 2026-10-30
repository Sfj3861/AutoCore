package com.sfj3861.autocore;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

/** Sends a single, user-authorized tap gesture at a configurable screen percentage. */
public class AutoCoreAccessibilityService extends AccessibilityService {
    private static volatile AutoCoreAccessibilityService instance;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override protected void onServiceConnected() { super.onServiceConnected(); instance = this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { }
    @Override public void onDestroy() { if (instance == this) instance = null; super.onDestroy(); }

    public static boolean tapConfigured(Context context) {
        AutoCoreAccessibilityService service = instance;
        if (service == null) return false;
        int xPercent = context.getSharedPreferences("autocore", 0).getInt("gesture_x", 50);
        int yPercent = context.getSharedPreferences("autocore", 0).getInt("gesture_y", 50);
        Point size = new Point();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) return false;
        Display display = wm.getDefaultDisplay();
        display.getRealSize(size);
        final float x = Math.max(1, Math.min(size.x - 1, size.x * xPercent / 100f));
        final float y = Math.max(1, Math.min(size.y - 1, size.y * yPercent / 100f));
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, 65);
        GestureDescription gesture = new GestureDescription.Builder().addStroke(stroke).build();
        return service.dispatchGesture(gesture, null, null);
    }
}
