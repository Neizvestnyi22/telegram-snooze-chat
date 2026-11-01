package org.telegram.lab.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.telegram.lab.R;
import org.telegram.lab.Tg;
import org.telegram.messenger.AndroidUtilities;

/**
 * Telegram 12.4 composer: one floating glass pill with emoji, text, attach
 * and the round accent mic button that turns into send.
 */
public class InputBarView extends LinearLayout {

    public interface OnSendListener {
        void onSend(String text);
    }

    public final FrameLayout topSlot;
    public final LinearLayout pill;
    public final FrameLayout leftSlot;
    public final ImageView emoji, gift, attach;
    public final EditText field;
    public final FrameLayout sendButton;
    private final ImageView mic, send;
    private final GradientDrawable sendBg;
    private OnSendListener listener;
    private boolean hasText;

    public InputBarView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setClipChildren(false);
        setClipToPadding(false);
        setPadding(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8), AndroidUtilities.dp(10));

        topSlot = new FrameLayout(context);
        topSlot.setClipChildren(false);
        addView(topSlot, new LayoutParams(-1, -2));

        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.BOTTOM);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        addView(row, new LayoutParams(-1, -2));

        pill = new LinearLayout(context);
        pill.setGravity(Gravity.BOTTOM);
        pill.setBackground(ChatHeaderView.glass(AndroidUtilities.dp(25)));
        pill.setElevation(AndroidUtilities.dp(3));
        pill.setMinimumHeight(AndroidUtilities.dp(50));
        row.addView(pill, new LayoutParams(0, -2, 1f));

        leftSlot = new FrameLayout(context);
        pill.addView(leftSlot, new LayoutParams(-2, AndroidUtilities.dp(48)));

        emoji = ChatHeaderView.iconButton(context, R.drawable.tg_input_smile);
        emoji.setImageDrawable(Tg.icon(context, R.drawable.tg_input_smile, 0xFF7E8288));
        pill.addView(emoji, new LayoutParams(AndroidUtilities.dp(46), AndroidUtilities.dp(48)));

        field = new EditText(context);
        field.setBackground(null);
        field.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        field.setTextColor(Tg.text);
        field.setHint("Message");
        field.setHintTextColor(0xFF9DA1A7);
        field.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
        field.setMaxLines(6);
        field.setTextCursorDrawable(cursor());
        pill.addView(field, new LayoutParams(0, -2, 1f));

        gift = ChatHeaderView.iconButton(context, R.drawable.tg_msg_input_gift);
        gift.setImageDrawable(Tg.icon(context, R.drawable.tg_msg_input_gift, 0xFF7E8288));
        pill.addView(gift, new LayoutParams(AndroidUtilities.dp(40), AndroidUtilities.dp(50)));

        attach = ChatHeaderView.iconButton(context, R.drawable.tg_input_attach);
        attach.setImageDrawable(Tg.icon(context, R.drawable.tg_input_attach, 0xFF7E8288));
        pill.addView(attach, new LayoutParams(AndroidUtilities.dp(44), AndroidUtilities.dp(50)));

        sendButton = new FrameLayout(context);
        sendBg = new GradientDrawable();
        sendBg.setShape(GradientDrawable.OVAL);
        sendBg.setColor(Tg.accent);
        sendButton.setBackground(sendBg);
        mic = new ImageView(context);
        mic.setImageDrawable(Tg.icon(context, R.drawable.tg_input_mic, Color.WHITE));
        mic.setScaleType(ImageView.ScaleType.CENTER);
        send = new ImageView(context);
        send.setImageDrawable(Tg.icon(context, R.drawable.tg_ic_send, Color.WHITE));
        send.setScaleType(ImageView.ScaleType.CENTER);
        send.setScaleX(0.8f);
        send.setScaleY(0.8f);
        send.setAlpha(0f);
        sendButton.addView(mic, new FrameLayout.LayoutParams(-1, -1));
        sendButton.addView(send, new FrameLayout.LayoutParams(-1, -1));
        LayoutParams sp = new LayoutParams(AndroidUtilities.dp(42), AndroidUtilities.dp(42));
        sp.setMargins(0, 0, AndroidUtilities.dp(4), AndroidUtilities.dp(4));
        pill.addView(sendButton, sp);

        sendButton.setOnClickListener(v -> {
            String t = field.getText().toString().trim();
            if (!t.isEmpty() && listener != null) {
                listener.onSend(t);
            }
        });
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                setHasText(s.toString().trim().length() > 0);
            }
        });
    }

    private GradientDrawable cursor() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Tg.accent);
        d.setSize(AndroidUtilities.dp(2), AndroidUtilities.dp(20));
        return d;
    }

    private void setHasText(boolean has) {
        if (hasText == has) {
            return;
        }
        hasText = has;
        send.animate().alpha(has ? 1 : 0).scaleX(has ? 1 : 0.8f).scaleY(has ? 1 : 0.8f).rotation(has ? 0 : -30).setDuration(170).start();
        mic.animate().alpha(has ? 0 : 1).scaleX(has ? 0.8f : 1).scaleY(has ? 0.8f : 1).setDuration(170).start();
    }

    public void setOnSendListener(OnSendListener l) {
        listener = l;
    }

    /** Demo recordings type through TouchDriver, so the soft keyboard stays hidden there. */
    public void setKeyboardEnabled(boolean enabled) {
        field.setShowSoftInputOnFocus(enabled);
        if (!enabled) {
            field.setOnTouchListener((v, e) -> {
                if (e.getAction() == android.view.MotionEvent.ACTION_UP) {
                    field.requestFocus();
                    field.setSelection(field.length());
                }
                return true;
            });
        }
    }
}
