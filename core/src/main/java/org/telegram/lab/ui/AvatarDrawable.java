package org.telegram.lab.ui;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;

import org.telegram.lab.Tg;

/** Telegram-style placeholder avatar: a gradient circle with initials or an emoji. */
public class AvatarDrawable extends Drawable {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
    private final String letters;
    private final int color;
    private int cachedSize;

    public AvatarDrawable(String name, int colorIndex) {
        this.color = Math.abs(colorIndex) % Tg.avatarTop.length;
        this.letters = initials(name);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTypeface(Tg.medium());
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public static String initials(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        int cp = name.codePointAt(0);
        if (!Character.isLetterOrDigit(cp)) {
            return new String(Character.toChars(cp));
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        sb.appendCodePoint(parts[0].codePointAt(0));
        if (parts.length > 1 && Character.isLetter(parts[parts.length - 1].codePointAt(0))) {
            sb.appendCodePoint(parts[parts.length - 1].codePointAt(0));
        }
        return sb.toString().toUpperCase();
    }

    public static int colorFor(String name) {
        return name == null ? 0 : Math.abs(name.hashCode()) % Tg.avatarTop.length;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        int size = b.width();
        if (size != cachedSize) {
            cachedSize = size;
            paint.setShader(new LinearGradient(0, b.top, 0, b.bottom, Tg.avatarTop[color], Tg.avatarBottom[color], Shader.TileMode.CLAMP));
        }
        canvas.drawCircle(b.exactCenterX(), b.exactCenterY(), size / 2f, paint);
        textPaint.setTextSize(size * (letters.length() > 1 ? 0.38f : 0.42f));
        float y = b.exactCenterY() - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(letters, b.exactCenterX(), y, textPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        textPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
