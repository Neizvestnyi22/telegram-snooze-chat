package org.telegram.lab.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import org.telegram.lab.Tg;
import org.telegram.messenger.AndroidUtilities;

/** Message bubble background with Telegram's tail on the bottom corner. */
public final class Bubble {

    private static final Path path = new Path();
    private static final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final float[] radii = new float[8];

    static {
        shadow.setColor(0x16000000);
    }

    private Bubble() {
    }

    public static Path path(RectF r, boolean out, boolean tail) {
        float R = AndroidUtilities.dp(17), small = AndroidUtilities.dp(5);
        for (int i = 0; i < 8; i++) {
            radii[i] = R;
        }
        if (tail) {
            if (out) {
                radii[4] = radii[5] = small;
            } else {
                radii[6] = radii[7] = small;
            }
        }
        path.reset();
        path.addRoundRect(r, radii, Path.Direction.CW);
        if (tail) {
            float t = AndroidUtilities.dp(6.5f), h = AndroidUtilities.dp(13);
            Path p = new Path();
            if (out) {
                p.moveTo(r.right - AndroidUtilities.dp(4), r.bottom - h);
                p.quadTo(r.right, r.bottom - AndroidUtilities.dp(2), r.right + t, r.bottom);
                p.lineTo(r.right - AndroidUtilities.dp(8), r.bottom);
                p.close();
            } else {
                p.moveTo(r.left + AndroidUtilities.dp(4), r.bottom - h);
                p.quadTo(r.left, r.bottom - AndroidUtilities.dp(2), r.left - t, r.bottom);
                p.lineTo(r.left + AndroidUtilities.dp(8), r.bottom);
                p.close();
            }
            path.op(p, Path.Op.UNION);
        }
        return path;
    }

    public static void draw(Canvas canvas, RectF r, boolean out, boolean tail) {
        draw(canvas, r, out, tail, out ? Tg.bubbleOut : Tg.bubbleIn, 1f);
    }

    public static void draw(Canvas canvas, RectF r, boolean out, boolean tail, int color, float alpha) {
        Path p = path(r, out, tail);
        canvas.save();
        canvas.translate(0, AndroidUtilities.dpf2(1));
        shadow.setAlpha((int) (0x16 * alpha));
        canvas.drawPath(p, shadow);
        canvas.restore();
        paint.setColor(color);
        paint.setAlpha((int) (Color.alpha(color) * alpha));
        canvas.drawPath(p, paint);
    }

    private static final class Color {
        static int alpha(int c) {
            return (c >>> 24) & 0xFF;
        }
    }
}
