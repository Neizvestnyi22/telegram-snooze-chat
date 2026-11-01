package org.telegram.lab.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.view.View;

import org.telegram.lab.Tg;
import org.telegram.messenger.AndroidUtilities;

/** Centered pill for dates and service messages. */
public class ServiceCell extends View {

    private String text;
    private final TextPaint paint = Tg.paint(14, 0xFFFFFFFF, true);
    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int bgColor = Tg.serviceBg;

    public ServiceCell(Context context, String text) {
        super(context);
        this.text = text;
        bg.setColor(bgColor);
    }

    public void setText(String t) {
        text = t;
        invalidate();
    }

    public void setBackgroundTint(int color) {
        bgColor = color;
        bg.setColor(color);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), AndroidUtilities.dp(38));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = paint.measureText(text) + AndroidUtilities.dp(18);
        float h = AndroidUtilities.dp(26);
        rect.set((getWidth() - w) / 2f, (getHeight() - h) / 2f, (getWidth() + w) / 2f, (getHeight() + h) / 2f);
        canvas.drawRoundRect(rect, h / 2f, h / 2f, bg);
        canvas.drawText(text, rect.left + AndroidUtilities.dp(9), rect.centerY() + AndroidUtilities.dp(5), paint);
    }
}
