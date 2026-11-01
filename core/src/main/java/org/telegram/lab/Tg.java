package org.telegram.lab;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;

/** Colors and fonts of Telegram for Android's default day theme. */
public final class Tg {

    public static final int accent = 0xFF3390EC;
    public static final int accentDark = 0xFF2A7CD1;
    public static final int text = 0xFF000000;
    public static final int textGray = 0xFF8D8E93;
    public static final int textGray2 = 0xFF6D6D72;
    public static final int divider = 0xFFE8E8EA;
    public static final int windowBg = 0xFFF1F2F6;
    public static final int header = 0xFFFFFFFF;
    public static final int headerIcon = 0xFF676A6F;

    public static final int bubbleIn = 0xFFFFFFFF;
    public static final int bubbleOut = 0xFFEEFFDE;
    public static final int timeIn = 0xFFA1AAB3;
    public static final int timeOut = 0xFF5DAA59;
    public static final int checkOut = 0xFF4FAE4E;
    public static final int linkIn = 0xFF2678B6;
    public static final int linkOut = 0xFF3A8E26;
    public static final int replyIn = 0xFF3A8FDA;
    public static final int replyOut = 0xFF59A44B;
    public static final int serviceBg = 0x55375A48;

    public static final int unread = 0xFF3390EC;
    public static final int unreadMuted = 0xFFBDBEC2;
    public static final int online = 0xFF4BCB1C;
    public static final int red = 0xFFE53935;
    public static final int orange = 0xFFF0A23B;
    public static final int green = 0xFF3FB950;

    public static final int[] avatarTop = {0xFFFF845E, 0xFFFEBB5B, 0xFFB694F9, 0xFF9AD164, 0xFF5BCBE3, 0xFF5CAFFA, 0xFFFF8AAC};
    public static final int[] avatarBottom = {0xFFD45246, 0xFFF68136, 0xFF6C61DF, 0xFF46BA43, 0xFF359AD4, 0xFF408ACF, 0xFFD95574};
    public static final int[] nameColors = {0xFFCC5049, 0xFFD67722, 0xFF955CDB, 0xFF40A920, 0xFF309EBA, 0xFF368AD1, 0xFFC7508B};

    private static Typeface medium;

    private Tg() {
    }

    public static Typeface medium() {
        if (medium == null) {
            medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        }
        return medium;
    }

    public static TextPaint paint(float sp, int color, boolean bold) {
        TextPaint p = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        p.setTextSize(AndroidUtilities.dp(sp));
        p.setColor(color);
        if (bold) {
            p.setTypeface(medium());
        }
        return p;
    }

    public static TextView text(Context context, float sp, int color, boolean bold) {
        TextView v = new TextView(context);
        v.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sp);
        v.setTextColor(color);
        v.setGravity(Gravity.CENTER_VERTICAL);
        v.setIncludeFontPadding(false);
        if (bold) {
            v.setTypeface(medium());
        }
        return v;
    }

    public static Drawable icon(Context context, int res, int color) {
        Drawable d = context.getDrawable(res).mutate();
        d.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        return d;
    }

    public static int alpha(int color, float a) {
        return (color & 0x00FFFFFF) | ((int) (((color >>> 24) & 0xFF) * a) << 24);
    }

    public static int blend(int c1, int c2, float f) {
        int a = (int) (((c1 >>> 24) & 0xFF) + (((c2 >>> 24) & 0xFF) - ((c1 >>> 24) & 0xFF)) * f);
        int r = (int) (((c1 >> 16) & 0xFF) + (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * f);
        int g = (int) (((c1 >> 8) & 0xFF) + (((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * f);
        int b = (int) ((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
