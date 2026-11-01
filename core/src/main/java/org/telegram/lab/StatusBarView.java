package org.telegram.lab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.TextPaint;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;

/** Status bar drawn inside the phone frame in presentation mode; a plain spacer otherwise. */
public class StatusBarView extends View {

    /** Time shown in presentation mode; demos with a time-lapse change it. */
    public static String clock = "9:41";

    private final boolean draw;
    private final TextPaint timePaint;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();

    public StatusBarView(Context context, boolean draw, boolean light) {
        super(context);
        this.draw = draw;
        int color = light ? 0xFFFFFFFF : 0xFF1B1B1F;
        timePaint = Tg.paint(14.5f, color, true);
        paint.setColor(color);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), LabActivity.statusBarHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!draw) {
            return;
        }
        float cy = getHeight() / 2f + AndroidUtilities.dp(2);
        canvas.drawText(clock, AndroidUtilities.dp(26), cy + AndroidUtilities.dp(5), timePaint);

        float x = getWidth() - AndroidUtilities.dp(24);
        // battery
        rect.set(x - AndroidUtilities.dp(11), cy - AndroidUtilities.dp(7), x, cy + AndroidUtilities.dp(7));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(rect, AndroidUtilities.dp(2), AndroidUtilities.dp(2), paint);
        rect.set(x - AndroidUtilities.dp(7.5f), cy - AndroidUtilities.dp(9), x - AndroidUtilities.dp(3.5f), cy - AndroidUtilities.dp(7));
        canvas.drawRect(rect, paint);
        // signal
        x -= AndroidUtilities.dp(19);
        path.reset();
        path.moveTo(x, cy - AndroidUtilities.dp(7));
        path.lineTo(x, cy + AndroidUtilities.dp(7));
        path.lineTo(x - AndroidUtilities.dp(14), cy + AndroidUtilities.dp(7));
        path.close();
        canvas.drawPath(path, paint);
        // wifi
        x -= AndroidUtilities.dp(22);
        path.reset();
        float r = AndroidUtilities.dp(9);
        path.moveTo(x, cy + AndroidUtilities.dp(7));
        rect.set(x - r, cy + AndroidUtilities.dp(7) - r * 1.6f, x + r, cy + AndroidUtilities.dp(7) + r * 0.4f);
        path.lineTo(x - r * 0.95f, cy - AndroidUtilities.dp(2.5f));
        path.quadTo(x, cy - AndroidUtilities.dp(10), x + r * 0.95f, cy - AndroidUtilities.dp(2.5f));
        path.close();
        canvas.drawPath(path, paint);
    }
}
