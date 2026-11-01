package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;

/**
 * Swipe-left action behind a chat row (Telegram's DialogsActivity swipe, with a "Snooze" action).
 * The colored action grows as the row slides; past the threshold it triggers on release.
 */
public class SwipeActionLayout extends FrameLayout {

    public interface Listener {
        void onAction(SwipeActionLayout layout);
    }

    private final View row;
    private final int color;
    private final Drawable icon;
    private final String label;
    private final Paint bg = new Paint();
    private final TextPaint text = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
    private Listener listener;
    private float downX, downY;
    private boolean dragging, armed;
    private final int slop;

    public SwipeActionLayout(Context context, View row, int color, Drawable icon, String label) {
        super(context);
        this.row = row;
        this.color = color;
        this.icon = icon;
        this.label = label;
        slop = ViewConfiguration.get(context).getScaledTouchSlop();
        setWillNotDraw(false);
        text.setColor(0xFFFFFFFF);
        text.setTextSize(AndroidUtilities.dp(13));
        text.setFakeBoldText(true);
        text.setTextAlign(Paint.Align.CENTER);
        addView(row, new LayoutParams(-1, -2));
    }

    public void setListener(Listener l) {
        listener = l;
    }

    public View getRow() {
        return row;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float tx = row.getTranslationX();
        if (tx >= 0) {
            return;
        }
        bg.setColor(color);
        canvas.drawRect(getWidth() + tx, 0, getWidth(), getHeight(), bg);
        float cx = getWidth() - AndroidUtilities.dp(48);
        float p = Math.min(1f, -tx / AndroidUtilities.dp(110));
        int s = (int) (AndroidUtilities.dp(26) * (0.6f + 0.4f * p));
        float cy = getHeight() / 2f - AndroidUtilities.dp(8);
        icon.setAlpha((int) (255 * p));
        icon.setBounds((int) (cx - s / 2f), (int) (cy - s / 2f), (int) (cx + s / 2f), (int) (cy + s / 2f));
        icon.draw(canvas);
        text.setAlpha((int) (255 * p));
        canvas.drawText(label, cx, cy + AndroidUtilities.dp(26), text);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                dragging = false;
                armed = false;
                super.dispatchTouchEvent(e);
                return true;
            case MotionEvent.ACTION_MOVE: {
                float dx = e.getX() - downX;
                if (!dragging && dx < -slop && Math.abs(dx) > Math.abs(e.getY() - downY)) {
                    dragging = true;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    MotionEvent cancel = MotionEvent.obtain(e);
                    cancel.setAction(MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(cancel);
                    cancel.recycle();
                }
                if (dragging) {
                    float tx = Math.max(-getWidth() * 0.75f, Math.min(0, dx + slop));
                    row.setTranslationX(tx);
                    boolean nowArmed = -tx > AndroidUtilities.dp(110);
                    if (nowArmed != armed) {
                        armed = nowArmed;
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    }
                    invalidate();
                    return true;
                }
                return super.dispatchTouchEvent(e);
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (dragging) {
                    row.animate().translationX(0).setDuration(260).setUpdateListener(a -> invalidate()).start();
                    if (armed && e.getActionMasked() == MotionEvent.ACTION_UP && listener != null) {
                        listener.onAction(this);
                    }
                    dragging = false;
                    return true;
                }
                return super.dispatchTouchEvent(e);
        }
        return super.dispatchTouchEvent(e);
    }
}
