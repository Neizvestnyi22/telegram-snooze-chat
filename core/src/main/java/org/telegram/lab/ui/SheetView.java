package org.telegram.lab.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;

/** In-window bottom sheet with a dimmed backdrop, like Telegram's BottomSheet. */
public class SheetView extends FrameLayout {

    public final LinearLayout panel;
    private final View dim;
    private Runnable onDismiss;
    private boolean dismissing;

    public SheetView(Context context) {
        super(context);
        dim = new View(context);
        dim.setBackgroundColor(0x66000000);
        dim.setOnClickListener(v -> dismiss());
        addView(dim, new LayoutParams(-1, -1));

        panel = new LinearLayout(context) {
            private final Paint handle = new Paint(Paint.ANTI_ALIAS_FLAG);

            {
                handle.setColor(0xFFD7D8DC);
                setWillNotDraw(false);
            }

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                float w = AndroidUtilities.dp(36), h = AndroidUtilities.dp(4);
                canvas.drawRoundRect((getWidth() - w) / 2f, AndroidUtilities.dp(8), (getWidth() + w) / 2f, AndroidUtilities.dp(8) + h, h / 2, h / 2, handle);
            }

            @Override
            public boolean onTouchEvent(MotionEvent event) {
                return true;
            }
        };
        panel.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        float r = AndroidUtilities.dp(24);
        bg.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        panel.setBackground(bg);
        panel.setPadding(0, AndroidUtilities.dp(20), 0, AndroidUtilities.dp(16));
        addView(panel, new LayoutParams(-1, -2, Gravity.BOTTOM));
    }

    /** Title (20sp medium) and an optional gray description, as in Telegram's BottomSheet header. */
    public SheetView header(CharSequence title, CharSequence description) {
        android.widget.TextView t = org.telegram.lab.Tg.text(getContext(), 20, 0xFF222222, true);
        t.setText(title);
        t.setPadding(AndroidUtilities.dp(21), AndroidUtilities.dp(6), AndroidUtilities.dp(21), AndroidUtilities.dp(description != null ? 6 : 12));
        panel.addView(t);
        if (description != null) {
            android.widget.TextView d = org.telegram.lab.Tg.text(getContext(), 14, org.telegram.lab.Tg.textGray2, false);
            d.setText(description);
            d.setLineSpacing(AndroidUtilities.dp(2), 1);
            d.setPadding(AndroidUtilities.dp(21), 0, AndroidUtilities.dp(21), AndroidUtilities.dp(10));
            panel.addView(d);
        }
        return this;
    }

    public SheetView setOnDismiss(Runnable r) {
        onDismiss = r;
        return this;
    }

    public void show(ViewGroup parent) {
        parent.addView(this, new FrameLayout.LayoutParams(-1, -1));
        dim.setAlpha(0);
        dim.animate().alpha(1).setDuration(250).start();
        panel.setTranslationY(AndroidUtilities.dp(600));
        panel.post(() -> {
            panel.setTranslationY(panel.getHeight());
            panel.animate().translationY(0).setDuration(320).setInterpolator(new PathInterpolator(0.2f, 0.9f, 0.25f, 1)).start();
        });
    }

    public void dismiss() {
        if (dismissing) {
            return;
        }
        dismissing = true;
        dim.animate().alpha(0).setDuration(220).start();
        panel.animate().translationY(panel.getHeight()).setDuration(240).setInterpolator(new PathInterpolator(0.4f, 0, 0.8f, 0.6f)).withEndAction(() -> {
            if (getParent() != null) {
                ((ViewGroup) getParent()).removeView(this);
            }
            if (onDismiss != null) {
                onDismiss.run();
            }
        }).start();
    }
}
