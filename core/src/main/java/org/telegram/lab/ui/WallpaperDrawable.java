package org.telegram.lab.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import org.telegram.messenger.AndroidUtilities;

/**
 * Four-color gradient wallpaper in the spirit of Telegram's default one,
 * with a faint doodle pattern on top.
 */
public class WallpaperDrawable extends Drawable {

    public static final int[] GREEN = {0xFFDCDEBD, 0xFF94C49A, 0xFFDFE29F, 0xFFA3C78F};
    public static final int[] BLUE = {0xFFB6C9EE, 0xFFE3D4F0, 0xFF8FB5E8, 0xFFC6B6EC};
    public static final int[] DARK = {0xFF1E2A38, 0xFF2B3A55, 0xFF1A2230, 0xFF34405A};

    private final int[] colors;
    private Bitmap gradient;
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint patternPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path doodle = new Path();

    public WallpaperDrawable(int[] colors) {
        this.colors = colors;
        patternPaint.setStyle(Paint.Style.STROKE);
        patternPaint.setStrokeWidth(AndroidUtilities.dp(1.6f));
        patternPaint.setStrokeCap(Paint.Cap.ROUND);
        patternPaint.setColor(0x14000000);
    }

    private Bitmap gradient() {
        if (gradient == null) {
            int n = 24;
            gradient = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
            float[][] pts = {{0.25f, 0.1f}, {0.8f, 0.3f}, {0.2f, 0.75f}, {0.85f, 0.95f}};
            for (int y = 0; y < n; y++) {
                for (int x = 0; x < n; x++) {
                    float fx = x / (n - 1f), fy = y / (n - 1f);
                    float r = 0, g = 0, b = 0, sum = 0;
                    for (int i = 0; i < 4; i++) {
                        float dx = fx - pts[i][0], dy = fy - pts[i][1];
                        float w = 1f / (0.06f + dx * dx + dy * dy);
                        w = (float) Math.pow(w, 1.5f);
                        r += ((colors[i] >> 16) & 0xFF) * w;
                        g += ((colors[i] >> 8) & 0xFF) * w;
                        b += (colors[i] & 0xFF) * w;
                        sum += w;
                    }
                    gradient.setPixel(x, y, 0xFF000000 | ((int) (r / sum) << 16) | ((int) (g / sum) << 8) | (int) (b / sum));
                }
            }
        }
        return gradient;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        canvas.drawBitmap(gradient(), null, b, bitmapPaint);
        int step = AndroidUtilities.dp(64);
        int row = 0;
        for (int y = b.top - step / 2; y < b.bottom + step; y += step, row++) {
            int col = 0;
            for (int x = b.left - step / 2 + (row % 2) * step / 2; x < b.right + step; x += step, col++) {
                drawDoodle(canvas, x, y, (row * 7 + col * 3) % 5);
            }
        }
    }

    private void drawDoodle(Canvas canvas, float x, float y, int kind) {
        float s = AndroidUtilities.dp(9);
        switch (kind) {
            case 0:
                canvas.drawCircle(x, y, s * 0.8f, patternPaint);
                break;
            case 1:
                doodle.reset();
                doodle.moveTo(x - s, y + s * 0.6f);
                doodle.lineTo(x, y - s * 0.8f);
                doodle.lineTo(x + s, y + s * 0.6f);
                doodle.close();
                canvas.drawPath(doodle, patternPaint);
                break;
            case 2:
                doodle.reset();
                doodle.moveTo(x - s, y);
                doodle.quadTo(x - s / 2, y - s, x, y);
                doodle.quadTo(x + s / 2, y + s, x + s, y);
                canvas.drawPath(doodle, patternPaint);
                break;
            case 3:
                canvas.drawLine(x - s * 0.7f, y, x + s * 0.7f, y, patternPaint);
                canvas.drawLine(x, y - s * 0.7f, x, y + s * 0.7f, patternPaint);
                break;
            default:
                doodle.reset();
                doodle.moveTo(x, y + s * 0.8f);
                doodle.cubicTo(x - s * 1.2f, y - s * 0.2f, x - s * 0.4f, y - s * 1.1f, x, y - s * 0.3f);
                doodle.cubicTo(x + s * 0.4f, y - s * 1.1f, x + s * 1.2f, y - s * 0.2f, x, y + s * 0.8f);
                canvas.drawPath(doodle, patternPaint);
                break;
        }
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
