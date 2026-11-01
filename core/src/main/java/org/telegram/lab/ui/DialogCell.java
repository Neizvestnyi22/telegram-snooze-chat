package org.telegram.lab.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;

import org.telegram.lab.R;
import org.telegram.lab.Tg;
import org.telegram.messenger.AndroidUtilities;

/** Chat list row, laid out like Telegram's DialogCell. */
public class DialogCell extends View {

    public long id;
    public String name;
    public String sender;
    public String message;
    public String time;
    public int unread;
    public boolean muted;
    public boolean pinned;
    public boolean online;
    public int status = MessageCell.STATUS_NONE;
    /** Small colored label drawn before the message (e.g. "Snoozed until 10:41"). */
    public String badgeText;
    public int badgeColor = Tg.orange;
    public Drawable avatar;
    public Drawable badgeIcon;
    public boolean divider = false;

    private final TextPaint namePaint = Tg.paint(16.5f, 0xFF1B1B1F, true);
    private final TextPaint msgPaint = Tg.paint(15, Tg.textGray2, false);
    private final TextPaint senderPaint = Tg.paint(15, Tg.accent, false);
    private final TextPaint timePaint = Tg.paint(13, Tg.textGray, false);
    private final TextPaint countPaint = Tg.paint(13, 0xFFFFFFFF, true);
    private final TextPaint badgePaint = Tg.paint(13.5f, 0, true);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private Drawable pinIcon;
    private float pulse;

    public DialogCell(Context context) {
        super(context);
        setBackground(new RippleDrawable(ColorStateList.valueOf(0x10000000), new ColorDrawable(0xFFFFFFFF), null));
        countPaint.setTextAlign(Paint.Align.CENTER);
        pinIcon = Tg.icon(context, R.drawable.tg_list_pin, 0xFFB5B8BD);
    }

    public DialogCell set(String name, String sender, String message, String time, int unread) {
        this.name = name;
        this.sender = sender;
        this.message = message;
        this.time = time;
        this.unread = unread;
        if (avatar == null) {
            avatar = new AvatarDrawable(name, AvatarDrawable.colorFor(name));
        }
        invalidate();
        return this;
    }

    public void setPulse(float p) {
        pulse = p;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), AndroidUtilities.dp(74));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int size = AndroidUtilities.dp(56);
        int ax = AndroidUtilities.dp(10), ay = (getHeight() - size) / 2;
        if (pulse > 0) {
            paint.setColor(Tg.alpha(Tg.orange, 0.12f * pulse));
            canvas.drawRect(0, 0, w, getHeight(), paint);
        }
        if (avatar != null) {
            avatar.setBounds(ax, ay, ax + size, ay + size);
            avatar.draw(canvas);
        }
        if (online) {
            paint.setColor(0xFFFFFFFF);
            canvas.drawCircle(ax + size - AndroidUtilities.dp(6), ay + size - AndroidUtilities.dp(6), AndroidUtilities.dp(8), paint);
            paint.setColor(Tg.online);
            canvas.drawCircle(ax + size - AndroidUtilities.dp(6), ay + size - AndroidUtilities.dp(6), AndroidUtilities.dp(5.5f), paint);
        }
        if (badgeIcon != null) {
            int bs = AndroidUtilities.dp(22);
            int bx = ax + size - bs + AndroidUtilities.dp(3), by = ay + size - bs + AndroidUtilities.dp(3);
            paint.setColor(0xFFFFFFFF);
            canvas.drawCircle(bx + bs / 2f, by + bs / 2f, bs / 2f + AndroidUtilities.dp(2), paint);
            paint.setColor(badgeColor);
            canvas.drawCircle(bx + bs / 2f, by + bs / 2f, bs / 2f, paint);
            int p = AndroidUtilities.dp(4);
            badgeIcon.setBounds(bx + p, by + p, bx + bs - p, by + bs - p);
            badgeIcon.draw(canvas);
        }

        int left = ax + size + AndroidUtilities.dp(12);
        int right = w - AndroidUtilities.dp(14);
        float timeW = timePaint.measureText(time == null ? "" : time);
        if (time != null) {
            canvas.drawText(time, right - timeW, AndroidUtilities.dp(29), timePaint);
        }
        CharSequence n = TextUtils.ellipsize(name == null ? "" : name, namePaint, right - timeW - left - AndroidUtilities.dp(26), TextUtils.TruncateAt.END);
        canvas.drawText(n, 0, n.length(), left, AndroidUtilities.dp(30), namePaint);

        float msgRight = right;
        if (unread > 0) {
            String c = String.valueOf(unread);
            float cw = Math.max(AndroidUtilities.dp(23), countPaint.measureText(c) + AndroidUtilities.dp(14));
            rect.set(right - cw, AndroidUtilities.dp(40), right, AndroidUtilities.dp(63));
            paint.setColor(muted ? Tg.unreadMuted : Tg.unread);
            canvas.drawRoundRect(rect, rect.height() / 2, rect.height() / 2, paint);
            canvas.drawText(c, rect.centerX(), rect.centerY() + AndroidUtilities.dp(4.5f), countPaint);
            msgRight = rect.left - AndroidUtilities.dp(8);
        } else if (pinned) {
            int s = AndroidUtilities.dp(20);
            pinIcon.setBounds(right - s, AndroidUtilities.dp(42), right, AndroidUtilities.dp(42) + s);
            pinIcon.draw(canvas);
            msgRight = right - s - AndroidUtilities.dp(8);
        }
        float x = left;
        float y = AndroidUtilities.dp(56);
        if (badgeText != null) {
            badgePaint.setColor(badgeColor);
            canvas.drawText(badgeText, x, y, badgePaint);
            x += badgePaint.measureText(badgeText) + AndroidUtilities.dp(6);
        }
        if (sender != null && message != null) {
            String s = sender + ": ";
            canvas.drawText(s, x, y, senderPaint);
            x += senderPaint.measureText(s);
        }
        if (message != null && x < msgRight) {
            CharSequence m = TextUtils.ellipsize(message, msgPaint, msgRight - x, TextUtils.TruncateAt.END);
            canvas.drawText(m, 0, m.length(), x, y, msgPaint);
        }
        if (divider) {
            paint.setColor(Tg.divider);
            canvas.drawRect(left, getHeight() - AndroidUtilities.dpf2(0.7f), w, getHeight(), paint);
        }
    }
}
