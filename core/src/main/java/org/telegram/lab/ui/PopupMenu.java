package org.telegram.lab.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
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

/**
 * Telegram's context menu (ActionBarPopupWindow + ActionBarMenuSubItem): a rounded card with
 * 48dp rows, a gray 24dp icon and 16sp text. Shown inside the window, anchored to a point.
 */
public class PopupMenu extends LinearLayout {

    private final FrameLayout host;
    private final View catcher;
    private Runnable onDismiss;

    public PopupMenu(FrameLayout host) {
        super(host.getContext());
        this.host = host;
        setOrientation(VERTICAL);
        setBackground(ChatHeaderView.glass(AndroidUtilities.dp(16)));
        setElevation(AndroidUtilities.dp(10));
        setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6));
        setMinimumWidth(AndroidUtilities.dp(200));
        catcher = new View(host.getContext());
        catcher.setOnClickListener(v -> dismiss());
    }

    public TextView add(int iconRes, CharSequence text, View.OnClickListener l) {
        return add(iconRes, text, 0, l);
    }

    public TextView add(int iconRes, CharSequence text, int color, View.OnClickListener l) {
        Context c = getContext();
        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(18), 0, AndroidUtilities.dp(22), 0);
        row.setBackground(new RippleDrawable(ColorStateList.valueOf(0x12000000), null, null));
        if (iconRes != 0) {
            ImageView iv = new ImageView(c);
            iv.setImageDrawable(Tg.icon(c, iconRes, color != 0 ? color : MenuRow.ICON_GRAY));
            iv.setScaleType(ImageView.ScaleType.CENTER);
            row.addView(iv, new LayoutParams(AndroidUtilities.dp(24), AndroidUtilities.dp(24)));
        }
        TextView t = Tg.text(c, 16, color != 0 ? color : 0xFF222222, false);
        t.setText(text);
        LayoutParams tl = new LayoutParams(-2, -2);
        tl.leftMargin = iconRes != 0 ? AndroidUtilities.dp(16) : 0;
        row.addView(t, tl);
        row.setOnClickListener(v -> {
            dismiss();
            if (l != null) {
                l.onClick(v);
            }
        });
        addView(row, new LayoutParams(-1, AndroidUtilities.dp(48)));
        return t;
    }

    public PopupMenu setOnDismiss(Runnable r) {
        onDismiss = r;
        return this;
    }

    /** Shows the menu with its top-left near (x, y) in host coordinates, kept on screen. */
    public void show(float x, float y) {
        host.addView(catcher, new FrameLayout.LayoutParams(-1, -1));
        host.addView(this, new FrameLayout.LayoutParams(-2, -2));
        measure(MeasureSpec.makeMeasureSpec(host.getWidth(), MeasureSpec.AT_MOST), MeasureSpec.UNSPECIFIED);
        float w = getMeasuredWidth(), h = getMeasuredHeight();
        float px = Math.max(AndroidUtilities.dp(8), Math.min(host.getWidth() - w - AndroidUtilities.dp(8), x));
        float py = Math.max(AndroidUtilities.dp(8), Math.min(host.getHeight() - h - AndroidUtilities.dp(8), y));
        setTranslationX(px);
        setTranslationY(py);
        setPivotX(Math.max(0, Math.min(w, x - px)));
        setPivotY(y > py + h / 2 ? h : 0);
        setScaleX(0.6f);
        setScaleY(0.6f);
        setAlpha(0);
        animate().scaleX(1).scaleY(1).alpha(1).setDuration(220).setInterpolator(new PathInterpolator(0.2f, 0.9f, 0.3f, 1)).start();
    }

    public void dismiss() {
        if (getParent() == null) {
            return;
        }
        host.removeView(catcher);
        animate().alpha(0).scaleX(0.85f).scaleY(0.85f).setDuration(140).withEndAction(() -> {
            if (getParent() != null) {
                ((ViewGroup) getParent()).removeView(this);
            }
            if (onDismiss != null) {
                onDismiss.run();
            }
        }).start();
    }
}
