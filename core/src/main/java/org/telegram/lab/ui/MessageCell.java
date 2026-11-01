package org.telegram.lab.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.Layout;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import org.telegram.lab.Tg;
import org.telegram.messenger.AndroidUtilities;

/** A text message bubble laid out like Telegram's ChatMessageCell. */
public class MessageCell extends View {

    public static final int STATUS_NONE = 0, STATUS_CLOCK = 1, STATUS_SENT = 2, STATUS_READ = 3;

    public interface StatusDrawer {
        /** Draws in place of the checks; (x, cy) is the left edge and vertical center of the status slot. */
        void draw(Canvas canvas, float x, float cy, MessageCell cell);
    }

    public interface SpanListener {
        void onSpanClick(MessageCell cell, ClickableSpan span, RectF bounds);
    }

    public final boolean out;
    public Object tag2;
    /** Room kept free at the right of the quote for a badge drawn in onDrawReplyExtra. */
    public int replyExtraWidth;
    private CharSequence text;
    private String time = "12:00";
    private String info;
    private int status;
    private String sender;
    private int senderColor;
    private String replyName, replyText;
    private int replyColor;
    private boolean tail = true;
    private boolean showAvatar;
    private AvatarDrawable avatar;
    private boolean groupChat;
    private float textSizeSp = 16;

    private StaticLayout layout;
    private final TextPaint textPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
    private final TextPaint timePaint = Tg.paint(12, 0, false);
    private final TextPaint senderPaint = Tg.paint(14, 0, true);
    private final TextPaint replyNamePaint = Tg.paint(14, 0, true);
    private final TextPaint replyTextPaint = Tg.paint(14, Tg.text, false);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint checkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path tmpPath = new Path();

    public final RectF bubble = new RectF();
    public final RectF replyRect = new RectF();
    private float textX, textY, timeX, timeY, senderY, replyY;
    private int contentWidth;
    private CharSequence replyTextShown;

    private StatusDrawer statusDrawer;
    private SpanListener spanListener;
    private OnClickListener replyListener;
    private ClickableSpan pressedSpan;
    private boolean pressedReply;
    private float downX, downY;
    private final Runnable longPress = this::performLongClick;
    private float highlight;
    private ValueAnimator highlightAnim;
    private int topPad = AndroidUtilities.dp(2), bottomPad = AndroidUtilities.dp(2);

    public MessageCell(Context context, boolean out) {
        super(context);
        this.out = out;
        textPaint.setColor(Tg.text);
        textPaint.linkColor = out ? Tg.linkOut : Tg.linkIn;
        timePaint.setColor(out ? Tg.timeOut : Tg.timeIn);
        checkPaint.setStyle(Paint.Style.STROKE);
        checkPaint.setStrokeWidth(AndroidUtilities.dp(1.5f));
        checkPaint.setStrokeCap(Paint.Cap.ROUND);
        checkPaint.setStrokeJoin(Paint.Join.ROUND);
        checkPaint.setColor(Tg.checkOut);
        setTextSizeSp(16);
    }

    public MessageCell setText(CharSequence t) {
        text = t;
        requestLayout();
        invalidate();
        return this;
    }

    public CharSequence getText() {
        return text;
    }

    public MessageCell setTime(String t) {
        time = t;
        requestLayout();
        return this;
    }

    /** Extra footer text before the time (views, reading time, "edited"). */
    public MessageCell setInfo(String i) {
        info = i;
        requestLayout();
        invalidate();
        return this;
    }

    public MessageCell setStatus(int s) {
        status = s;
        invalidate();
        return this;
    }

    public int getStatus() {
        return status;
    }

    public MessageCell setSender(String name, int color) {
        sender = name;
        senderColor = color;
        senderPaint.setColor(color);
        requestLayout();
        return this;
    }

    public MessageCell setReply(String name, String text, int color) {
        replyName = name;
        replyText = text;
        replyColor = color;
        replyNamePaint.setColor(color);
        requestLayout();
        return this;
    }

    public boolean hasReply() {
        return replyName != null;
    }

    public MessageCell setTail(boolean t) {
        tail = t;
        invalidate();
        return this;
    }

    /** Group chats leave room for the avatar column on incoming messages. */
    public MessageCell setGroup(boolean group, String avatarName) {
        groupChat = group;
        if (avatarName != null) {
            avatar = new AvatarDrawable(avatarName, AvatarDrawable.colorFor(avatarName));
            showAvatar = true;
        } else {
            showAvatar = false;
        }
        requestLayout();
        return this;
    }

    public MessageCell setSpacing(int topDp, int bottomDp) {
        topPad = AndroidUtilities.dp(topDp);
        bottomPad = AndroidUtilities.dp(bottomDp);
        requestLayout();
        return this;
    }

    public void setTextSizeSp(float sp) {
        textSizeSp = sp;
        textPaint.setTextSize(AndroidUtilities.dpf2(sp));
        requestLayout();
        invalidate();
    }

    public float getTextSizeSp() {
        return textSizeSp;
    }

    public void setStatusDrawer(StatusDrawer d) {
        statusDrawer = d;
        invalidate();
    }

    public void setSpanListener(SpanListener l) {
        spanListener = l;
    }

    public void setOnReplyClickListener(OnClickListener l) {
        replyListener = l;
    }

    public StaticLayout getLayout() {
        return layout;
    }

    public float getTextX() {
        return textX;
    }

    public float getTextY() {
        return textY;
    }

    public TextPaint getTextPaint() {
        return textPaint;
    }

    private int leftInset() {
        return groupChat && !out ? AndroidUtilities.dp(48) : AndroidUtilities.dp(10);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int maxBubble = width - leftInset() - AndroidUtilities.dp(out ? 64 : 54);
        int padL = AndroidUtilities.dp(12), padR = AndroidUtilities.dp(11);
        int maxText = maxBubble - padL - padR;
        CharSequence t = text == null ? "" : text;
        layout = StaticLayout.Builder.obtain(t, 0, t.length(), textPaint, Math.max(1, maxText))
                .setLineSpacing(AndroidUtilities.dp(1), 1f)
                .setIncludePad(false)
                .build();
        float textW = 0;
        for (int i = 0; i < layout.getLineCount(); i++) {
            textW = Math.max(textW, layout.getLineRight(i) - layout.getLineLeft(i));
        }
        String foot = footer();
        float timeW = timePaint.measureText(foot) + (out ? AndroidUtilities.dp(22) : 0) + AndroidUtilities.dp(8);
        float lastLine = layout.getLineCount() > 0 ? layout.getLineRight(layout.getLineCount() - 1) - layout.getLineLeft(layout.getLineCount() - 1) : 0;
        boolean timeInline = lastLine + timeW <= maxText;
        float w = timeInline ? Math.max(textW, lastLine + timeW) : Math.max(textW, timeW);
        int h = layout.getHeight() + (timeInline ? 0 : AndroidUtilities.dp(15));

        int top = AndroidUtilities.dp(7);
        if (sender != null) {
            w = Math.max(w, senderPaint.measureText(sender));
            senderY = top + AndroidUtilities.dp(14);
            top += AndroidUtilities.dp(20);
        }
        if (replyName != null) {
            float rw = Math.min(maxText, Math.max(replyNamePaint.measureText(replyName), replyTextPaint.measureText(replyText)) + AndroidUtilities.dp(20));
            w = Math.max(w, Math.max(rw, AndroidUtilities.dp(120)));
            replyY = top;
            top += AndroidUtilities.dp(46);
        }
        contentWidth = (int) Math.ceil(w);
        int bubbleW = contentWidth + padL + padR;
        int bubbleH = top + h + AndroidUtilities.dp(7);
        float left = out ? width - AndroidUtilities.dp(10) - bubbleW : leftInset();
        bubble.set(left, topPad, left + bubbleW, topPad + bubbleH);
        textX = left + padL;
        textY = topPad + top;
        timeX = bubble.right - padR - timeW + AndroidUtilities.dp(8);
        timeY = bubble.bottom - AndroidUtilities.dp(7);
        if (replyName != null) {
            replyRect.set(textX - AndroidUtilities.dp(3), topPad + replyY, textX + contentWidth + AndroidUtilities.dp(3), topPad + replyY + AndroidUtilities.dp(40));
            replyTextShown = TextUtils.ellipsize(replyText, replyTextPaint, replyRect.width() - AndroidUtilities.dp(16) - replyExtraWidth, TextUtils.TruncateAt.END);
        }
        setMeasuredDimension(width, (int) bubble.bottom + bottomPad);
    }

    private String footer() {
        return info != null ? info + "  " + time : time;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (highlight > 0) {
            paint.setColor(Tg.alpha(Tg.accent, 0.22f * highlight));
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        }
        if (showAvatar && avatar != null) {
            int s = AndroidUtilities.dp(36);
            avatar.setBounds(AndroidUtilities.dp(6), (int) bubble.bottom - s, AndroidUtilities.dp(6) + s, (int) bubble.bottom);
            avatar.draw(canvas);
        }
        Bubble.draw(canvas, bubble, out, tail);
        if (sender != null) {
            canvas.drawText(sender, textX, senderY + topPad, senderPaint);
        }
        if (replyName != null) {
            int c = replyColor;
            paint.setColor(Tg.alpha(c, pressedReply ? 0.22f : 0.11f));
            float r = AndroidUtilities.dp(6);
            canvas.drawRoundRect(replyRect, r, r, paint);
            paint.setColor(c);
            canvas.save();
            canvas.clipRect(replyRect.left, replyRect.top, replyRect.left + AndroidUtilities.dp(3), replyRect.bottom);
            canvas.drawRoundRect(replyRect, r, r, paint);
            canvas.restore();
            canvas.drawText(replyName, replyRect.left + AndroidUtilities.dp(10), replyRect.top + AndroidUtilities.dp(17), replyNamePaint);
            canvas.drawText(replyTextShown, 0, replyTextShown.length(), replyRect.left + AndroidUtilities.dp(10), replyRect.top + AndroidUtilities.dp(34), replyTextPaint);
            onDrawReplyExtra(canvas, replyRect);
        }
        canvas.save();
        canvas.translate(textX, textY);
        if (pressedSpan != null && text instanceof Spanned) {
            Spanned sp = (Spanned) text;
            tmpPath.reset();
            layout.getSelectionPath(sp.getSpanStart(pressedSpan), sp.getSpanEnd(pressedSpan), tmpPath);
            paint.setColor(Tg.alpha(textPaint.linkColor, 0.2f));
            canvas.drawPath(tmpPath, paint);
        }
        onDrawUnderText(canvas);
        layout.draw(canvas);
        canvas.restore();

        String foot = footer();
        canvas.drawText(foot, timeX, timeY, timePaint);
        if (out) {
            float sx = timeX + timePaint.measureText(foot) + AndroidUtilities.dp(4);
            float cy = timeY - AndroidUtilities.dp(4);
            if (statusDrawer != null) {
                statusDrawer.draw(canvas, sx, cy, this);
            } else {
                drawStatus(canvas, sx, cy);
            }
        }
    }

    protected void onDrawReplyExtra(Canvas canvas, RectF rect) {
    }

    protected void onDrawUnderText(Canvas canvas) {
    }

    private void drawStatus(Canvas canvas, float x, float cy) {
        if (status == STATUS_CLOCK) {
            float r = AndroidUtilities.dp(5.5f);
            checkPaint.setColor(Tg.timeOut);
            canvas.drawCircle(x + r + AndroidUtilities.dp(2), cy, r, checkPaint);
            canvas.drawLine(x + r + AndroidUtilities.dp(2), cy, x + r + AndroidUtilities.dp(2), cy - AndroidUtilities.dp(3.5f), checkPaint);
            canvas.drawLine(x + r + AndroidUtilities.dp(2), cy, x + r + AndroidUtilities.dp(4.5f), cy, checkPaint);
        } else if (status == STATUS_SENT || status == STATUS_READ) {
            checkPaint.setColor(Tg.checkOut);
            drawCheck(canvas, x + (status == STATUS_READ ? 0 : AndroidUtilities.dp(4)), cy);
            if (status == STATUS_READ) {
                drawCheck(canvas, x + AndroidUtilities.dp(5), cy);
            }
        }
    }

    private void drawCheck(Canvas canvas, float x, float cy) {
        tmpPath.reset();
        tmpPath.moveTo(x, cy);
        tmpPath.lineTo(x + AndroidUtilities.dp(3.5f), cy + AndroidUtilities.dp(3.5f));
        tmpPath.lineTo(x + AndroidUtilities.dp(10), cy - AndroidUtilities.dp(3.5f));
        canvas.drawPath(tmpPath, checkPaint);
    }

    public void highlight() {
        if (highlightAnim != null) {
            highlightAnim.cancel();
        }
        highlightAnim = ValueAnimator.ofFloat(0, 1, 1, 0);
        highlightAnim.setDuration(1600);
        highlightAnim.addUpdateListener(a -> {
            highlight = (float) a.getAnimatedValue();
            invalidate();
        });
        highlightAnim.start();
    }

    private ClickableSpan spanAt(float x, float y) {
        if (!(text instanceof Spanned) || layout == null) {
            return null;
        }
        float lx = x - textX, ly = y - textY;
        if (lx < 0 || ly < 0 || ly > layout.getHeight()) {
            return null;
        }
        int line = layout.getLineForVertical((int) ly);
        if (lx > layout.getLineRight(line) + AndroidUtilities.dp(4)) {
            return null;
        }
        int off = layout.getOffsetForHorizontal(line, lx);
        ClickableSpan[] spans = ((Spanned) text).getSpans(off, off, ClickableSpan.class);
        if (spans.length == 0 && off > 0) {
            spans = ((Spanned) text).getSpans(off - 1, off - 1, ClickableSpan.class);
        }
        return spans.length > 0 ? spans[0] : null;
    }

    /** Bounds of a span in this cell's coordinates (first line of the span). */
    public RectF spanBounds(Object span) {
        Spanned sp = (Spanned) text;
        int s = sp.getSpanStart(span), e = sp.getSpanEnd(span);
        int line = layout.getLineForOffset(s);
        float x1 = layout.getPrimaryHorizontal(s);
        int endLine = layout.getLineForOffset(e);
        float x2 = endLine == line ? layout.getPrimaryHorizontal(e) : layout.getLineRight(line);
        return new RectF(textX + x1, textY + layout.getLineTop(line), textX + x2, textY + layout.getLineBottom(line));
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                if (replyName != null && replyRect.contains(downX, downY)) {
                    pressedReply = true;
                } else {
                    pressedSpan = spanAt(downX, downY);
                }
                if (!pressedReply && pressedSpan == null && !bubble.contains(downX, downY)) {
                    return false;
                }
                postDelayed(longPress, ViewConfiguration.getLongPressTimeout());
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(e.getX() - downX) > AndroidUtilities.dp(8) || Math.abs(e.getY() - downY) > AndroidUtilities.dp(8)) {
                    cancelPress();
                }
                return true;
            case MotionEvent.ACTION_UP:
                boolean wasLong = !hasLongPressPending();
                removeCallbacks(longPress);
                if (!wasLong) {
                    if (pressedReply && replyListener != null) {
                        replyListener.onClick(this);
                    } else if (pressedSpan != null) {
                        if (spanListener != null) {
                            spanListener.onSpanClick(this, pressedSpan, spanBounds(pressedSpan));
                        } else {
                            pressedSpan.onClick(this);
                        }
                    } else {
                        performClick();
                    }
                }
                cancelPress();
                return true;
            case MotionEvent.ACTION_CANCEL:
                cancelPress();
                return true;
        }
        return false;
    }

    private boolean longPending = false;

    private boolean hasLongPressPending() {
        return getHandler() != null && getHandler().hasCallbacks(longPress);
    }

    private void cancelPress() {
        removeCallbacks(longPress);
        pressedSpan = null;
        pressedReply = false;
        invalidate();
    }

    public void setAvatarVisible(boolean v) {
        showAvatar = v && avatar != null;
        invalidate();
    }

    public int bubbleTop() {
        return (int) bubble.top;
    }

    public static Layout.Alignment align() {
        return Layout.Alignment.ALIGN_NORMAL;
    }
}
