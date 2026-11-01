package org.telegram.lab.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.lab.Tg;
import org.telegram.messenger.AndroidUtilities;

/** Dark floating notice at the bottom, like Telegram's BulletinFactory layouts. */
public class Bulletin extends LinearLayout {

    public final ImageView icon;
    public final TextView text;
    public final TextView button;
    private final Runnable hide = this::hide;

    public Bulletin(Context context) {
        super(context);
        setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF2262628);
        bg.setCornerRadius(AndroidUtilities.dp(12));
        setBackground(bg);
        setElevation(AndroidUtilities.dp(4));
        setMinimumHeight(AndroidUtilities.dp(48));
        setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(6), AndroidUtilities.dp(6), AndroidUtilities.dp(6));

        icon = new ImageView(context);
        icon.setScaleType(ImageView.ScaleType.CENTER);
        addView(icon, new LayoutParams(AndroidUtilities.dp(28), AndroidUtilities.dp(28)));
        text = Tg.text(context, 14.5f, 0xFFFFFFFF, false);
        text.setLineSpacing(AndroidUtilities.dp(1), 1);
        LayoutParams tp = new LayoutParams(0, -2, 1f);
        tp.leftMargin = AndroidUtilities.dp(12);
        addView(text, tp);
        button = Tg.text(context, 14.5f, 0xFF67B3F5, true);
        button.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8));
        button.setVisibility(GONE);
        addView(button, new LayoutParams(-2, -2));
    }

    public static Bulletin make(ViewGroup parent, int iconRes, CharSequence message) {
        Bulletin b = new Bulletin(parent.getContext());
        if (iconRes != 0) {
            b.icon.setImageDrawable(Tg.icon(parent.getContext(), iconRes, 0xFFFFFFFF));
        } else {
            b.icon.setVisibility(GONE);
        }
        b.text.setText(message);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        lp.leftMargin = lp.rightMargin = AndroidUtilities.dp(8);
        lp.bottomMargin = AndroidUtilities.dp(78);
        parent.addView(b, lp);
        return b;
    }

    public Bulletin button(String label, View.OnClickListener l) {
        button.setText(label);
        button.setVisibility(VISIBLE);
        button.setOnClickListener(v -> {
            l.onClick(v);
            hide();
        });
        return this;
    }

    public Bulletin show(long duration) {
        setAlpha(0);
        setTranslationY(AndroidUtilities.dp(40));
        animate().alpha(1).translationY(0).setDuration(260).setInterpolator(new PathInterpolator(0.2f, 0.9f, 0.3f, 1)).start();
        if (duration > 0) {
            postDelayed(hide, duration);
        }
        return this;
    }

    public void hide() {
        removeCallbacks(hide);
        animate().alpha(0).translationY(AndroidUtilities.dp(40)).setDuration(200).withEndAction(() -> {
            if (getParent() != null) {
                ((ViewGroup) getParent()).removeView(this);
            }
        }).start();
    }
}
