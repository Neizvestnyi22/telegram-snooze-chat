package org.telegram.lab;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;

/**
 * Base for every demo app. A feature builds its screen into {@link #content}.
 * Extras: --ez stage true (presentation frame for videos), --ez autoplay true (scripted demo).
 */
public abstract class LabActivity extends Activity {

    protected FrameLayout phone;
    protected FrameLayout content;
    protected StageView stage;
    protected TouchDriver driver;
    protected boolean isStage;
    protected boolean autoplay;
    public static int statusBarHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidUtilities.init(this);
        isStage = getIntent().getBooleanExtra("stage", false);
        autoplay = getIntent().getBooleanExtra("autoplay", false);

        Window w = getWindow();
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.WHITE);
        w.setDecorFitsSystemWindows(false);
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        phone = new FrameLayout(this);
        content = new FrameLayout(this);
        phone.addView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (isStage) {
            statusBarHeight = AndroidUtilities.dp(34);
            stage = new StageView(this, phone);
            setContentView(stage);
            WindowInsetsController c = w.getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.systemBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
            w.getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        } else {
            int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
            statusBarHeight = id > 0 ? getResources().getDimensionPixelSize(id) : AndroidUtilities.dp(24);
            setContentView(phone);
            w.getInsetsController().setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        }
        onCreateScreen(content);
        driver = new TouchDriver(phone, stage);
        if (autoplay) {
            phone.postDelayed(() -> onAutoplay(driver), 1500);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && isStage) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.systemBars());
            }
        }
    }

    /** Recording starts the script on an already visible screen: am start -f 0x20000000 --ez autoplay true */
    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        if (intent.getBooleanExtra("autoplay", false) && !autoplay) {
            autoplay = true;
            onAutoplayRequested();
            phone.postDelayed(() -> onAutoplay(driver), 700);
        }
    }

    /** Called before a late autoplay starts, e.g. to hide the soft keyboard path. */
    protected void onAutoplayRequested() {
    }

    /** True in presentation mode, where the app draws its own status bar. */
    public boolean drawsStatusBar() {
        return isStage;
    }

    protected abstract void onCreateScreen(FrameLayout content);

    protected void onAutoplay(TouchDriver d) {
    }

    public static View statusBar(Activity activity, boolean light) {
        return new StatusBarView(activity, ((LabActivity) activity).isStage, light);
    }
}
