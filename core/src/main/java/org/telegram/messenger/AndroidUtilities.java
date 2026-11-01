package org.telegram.messenger;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

/**
 * A tiny subset of Telegram's AndroidUtilities with the same signatures,
 * so feature classes compile unchanged inside Telegram for Android.
 */
public class AndroidUtilities {

    public static float density = 1;
    private static final Handler handler = new Handler(Looper.getMainLooper());

    public static void init(Context context) {
        density = context.getResources().getDisplayMetrics().density;
    }

    public static int dp(float value) {
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }

    public static float dpf2(float value) {
        return density * value;
    }

    public static void runOnUIThread(Runnable runnable) {
        runOnUIThread(runnable, 0);
    }

    public static void runOnUIThread(Runnable runnable, long delay) {
        if (delay == 0) {
            handler.post(runnable);
        } else {
            handler.postDelayed(runnable, delay);
        }
    }

    public static void cancelRunOnUIThread(Runnable runnable) {
        handler.removeCallbacks(runnable);
    }

    public static float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }

    public static float clamp(float value, float max, float min) {
        return Math.max(Math.min(value, max), min);
    }
}
