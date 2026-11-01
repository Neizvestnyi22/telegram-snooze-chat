package org.telegram.lab.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.lab.Tg;
import org.telegram.messenger.AndroidUtilities;

/**
 * Row of Telegram's BottomSheet / settings lists: 24dp gray icon at 21dp,
 * text starting at 72dp, 48dp tall (60dp with a subtitle), optional value on the right.
 */
public class MenuRow extends LinearLayout {

    public static final int ICON_GRAY = 0xFF6F7377;

    public final ImageView icon;
    public final TextView title;
    public final TextView subtitle;
    public final TextView value;

    public MenuRow(Context context, int iconRes, int iconColor, CharSequence titleText, CharSequence subtitleText) {
        super(context);
        setGravity(Gravity.CENTER_VERTICAL);
        setMinimumHeight(AndroidUtilities.dp(subtitleText != null ? 60 : 48));
        setPadding(AndroidUtilities.dp(21), AndroidUtilities.dp(6), AndroidUtilities.dp(21), AndroidUtilities.dp(6));
        setBackground(new RippleDrawable(ColorStateList.valueOf(0x12000000), null, null));

        icon = new ImageView(context);
        icon.setScaleType(ImageView.ScaleType.CENTER);
        if (iconRes != 0) {
            icon.setImageDrawable(Tg.icon(context, iconRes, iconColor == 0 ? ICON_GRAY : iconColor));
            addView(icon, new LayoutParams(AndroidUtilities.dp(24), AndroidUtilities.dp(24)));
        }
        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(VERTICAL);
        title = Tg.text(context, 16, 0xFF222222, false);
        texts.addView(title, new LayoutParams(-2, -2));
        title.setText(titleText);
        subtitle = Tg.text(context, 13.5f, Tg.textGray, false);
        if (subtitleText != null) {
            subtitle.setText(subtitleText);
            subtitle.setLineSpacing(AndroidUtilities.dp(1), 1);
            LayoutParams sp = new LayoutParams(-2, -2);
            sp.topMargin = AndroidUtilities.dp(3);
            texts.addView(subtitle, sp);
        }
        LayoutParams tp = new LayoutParams(0, -2, 1f);
        tp.leftMargin = iconRes != 0 ? AndroidUtilities.dp(27) : 0;
        addView(texts, tp);
        value = Tg.text(context, 15, Tg.textGray, false);
        addView(value, new LayoutParams(-2, -2));
    }

    /** Blue section header, like Telegram's HeaderCell. */
    public static TextView header(Context context, String text) {
        TextView t = Tg.text(context, 15, Tg.accent, true);
        t.setText(text);
        t.setPadding(AndroidUtilities.dp(21), AndroidUtilities.dp(12), AndroidUtilities.dp(21), AndroidUtilities.dp(6));
        return t;
    }
}
