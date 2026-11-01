package org.telegram.lab.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.lab.LabActivity;
import org.telegram.lab.R;
import org.telegram.lab.Tg;
import org.telegram.messenger.AndroidUtilities;

/**
 * Telegram 12.4 chat header: no bar, just floating glass capsules over the chat —
 * a round back button, the avatar + title capsule and a capsule with actions.
 * Panels such as the pinned message go into {@link #belowSlot} as glass cards.
 */
public class ChatHeaderView extends LinearLayout {

    public final View statusBar;
    public final FrameLayout row;
    public final ImageView back;
    public final LinearLayout titlePill;
    public final View avatarView;
    public final TextView title, subtitle;
    public final LinearLayout titleBlock;
    public final LinearLayout actions;
    public final LinearLayout belowSlot;
    private Drawable avatar;

    public ChatHeaderView(Activity activity) {
        super(activity);
        Context context = activity;
        setOrientation(VERTICAL);
        setClipChildren(false);
        setClipToPadding(false);
        GradientDrawable fade = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x80FFFFFF, 0x00FFFFFF});
        setBackground(fade);

        statusBar = LabActivity.statusBar(activity, false);
        addView(statusBar, new LayoutParams(-1, -2));

        row = new FrameLayout(context);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        addView(row, new LayoutParams(-1, AndroidUtilities.dp(62)));

        back = iconButton(context, R.drawable.tg_ic_ab_back);
        back.setImageDrawable(Tg.icon(context, R.drawable.tg_ic_ab_back, 0xFF222428));
        back.setBackground(glassRipple(AndroidUtilities.dp(26)));
        back.setElevation(AndroidUtilities.dp(3));
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(AndroidUtilities.dp(52), AndroidUtilities.dp(52), Gravity.LEFT | Gravity.CENTER_VERTICAL);
        bp.leftMargin = AndroidUtilities.dp(10);
        row.addView(back, bp);

        titlePill = new LinearLayout(context);
        titlePill.setGravity(Gravity.CENTER_VERTICAL);
        titlePill.setBackground(glassRipple(AndroidUtilities.dp(26)));
        titlePill.setElevation(AndroidUtilities.dp(3));
        titlePill.setPadding(AndroidUtilities.dp(5), 0, AndroidUtilities.dp(20), 0);
        avatarView = new View(context) {
            @Override
            protected void onDraw(Canvas canvas) {
                if (avatar != null) {
                    avatar.setBounds(0, 0, getWidth(), getHeight());
                    avatar.draw(canvas);
                }
            }
        };
        titlePill.addView(avatarView, new LayoutParams(AndroidUtilities.dp(42), AndroidUtilities.dp(42)));
        titleBlock = new LinearLayout(context);
        titleBlock.setOrientation(VERTICAL);
        title = Tg.text(context, 17, 0xFF1B1B1F, true);
        title.setSingleLine();
        title.setEllipsize(TextUtils.TruncateAt.END);
        subtitle = Tg.text(context, 13.5f, Tg.textGray, false);
        subtitle.setSingleLine();
        subtitle.setEllipsize(TextUtils.TruncateAt.END);
        titleBlock.addView(title, new LayoutParams(-2, -2));
        LayoutParams sp = new LayoutParams(-2, -2);
        sp.topMargin = AndroidUtilities.dp(3);
        titleBlock.addView(subtitle, sp);
        LayoutParams tb = new LayoutParams(-2, -2);
        tb.leftMargin = AndroidUtilities.dp(10);
        titlePill.addView(titleBlock, tb);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-2, AndroidUtilities.dp(52), Gravity.LEFT | Gravity.CENTER_VERTICAL);
        tp.leftMargin = AndroidUtilities.dp(70);
        tp.rightMargin = AndroidUtilities.dp(118);
        row.addView(titlePill, tp);

        actions = new LinearLayout(context);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setBackground(glass(AndroidUtilities.dp(26)));
        actions.setElevation(AndroidUtilities.dp(3));
        actions.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
        actions.setVisibility(GONE);
        FrameLayout.LayoutParams ap = new FrameLayout.LayoutParams(-2, AndroidUtilities.dp(52), Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        ap.rightMargin = AndroidUtilities.dp(10);
        row.addView(actions, ap);

        belowSlot = new LinearLayout(context);
        belowSlot.setOrientation(VERTICAL);
        belowSlot.setClipChildren(false);
        belowSlot.setClipToPadding(false);
        belowSlot.setPadding(AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10), 0);
        addView(belowSlot, new LayoutParams(-1, -2));
    }

    /** Light frosted surface used for every floating control of the 12.4 design. */
    public static GradientDrawable glass(float radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE8FFFFFF);
        bg.setCornerRadius(radius);
        bg.setStroke(AndroidUtilities.dp(0.7f), 0x14000000);
        return bg;
    }

    public static RippleDrawable glassRipple(float radius) {
        return new RippleDrawable(ColorStateList.valueOf(0x14000000), glass(radius), null);
    }

    public static ImageView iconButton(Context context, int res) {
        ImageView v = new ImageView(context);
        v.setImageDrawable(Tg.icon(context, res, Tg.headerIcon));
        v.setScaleType(ImageView.ScaleType.CENTER);
        v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x14000000), null, null));
        return v;
    }

    public ImageView addAction(int res) {
        ImageView v = iconButton(getContext(), res);
        v.setImageDrawable(Tg.icon(getContext(), res, 0xFF222428));
        actions.setVisibility(VISIBLE);
        actions.addView(v, new LayoutParams(AndroidUtilities.dp(44), AndroidUtilities.dp(48)));
        FrameLayout.LayoutParams tp = (FrameLayout.LayoutParams) titlePill.getLayoutParams();
        tp.rightMargin = AndroidUtilities.dp(10 + 8 + 8) + actions.getChildCount() * AndroidUtilities.dp(44);
        titlePill.setLayoutParams(tp);
        return v;
    }

    /** Adds a glass card under the capsules (pinned message, bot panel, progress…). */
    public <T extends View> T addPanel(T panel) {
        LayoutParams lp = new LayoutParams(-1, -2);
        lp.topMargin = AndroidUtilities.dp(6);
        belowSlot.addView(panel, lp);
        return panel;
    }

    public ChatHeaderView setTitle(CharSequence t) {
        title.setText(t);
        return this;
    }

    public ChatHeaderView setSubtitle(CharSequence s, boolean online) {
        subtitle.setText(s);
        subtitle.setTextColor(online ? Tg.accent : Tg.textGray);
        return this;
    }

    public ChatHeaderView setAvatar(String name) {
        avatar = new AvatarDrawable(name, AvatarDrawable.colorFor(name));
        avatarView.invalidate();
        return this;
    }

    public ChatHeaderView setAvatar(Drawable d) {
        avatar = d;
        avatarView.invalidate();
        return this;
    }

    public int barHeight() {
        return LabActivity.statusBarHeight + AndroidUtilities.dp(62);
    }
}
