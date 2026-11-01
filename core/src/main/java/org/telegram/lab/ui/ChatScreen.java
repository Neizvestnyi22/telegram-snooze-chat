package org.telegram.lab.ui;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.telegram.messenger.AndroidUtilities;

/** Wallpaper + scrolling message list + header + composer, like Telegram's ChatActivity. */
public class ChatScreen extends FrameLayout {

    public final ChatHeaderView header;
    public final ScrollView scroll;
    public final LinearLayout list;
    public final InputBarView input;
    public final FrameLayout overlay;

    public ChatScreen(Activity activity, int[] wallpaper) {
        super(activity);
        setBackground(new WallpaperDrawable(wallpaper));
        scroll = new ScrollView(activity);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(OVER_SCROLL_NEVER);
        scroll.setClipToPadding(false);
        scroll.setFillViewport(true);
        list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setGravity(Gravity.BOTTOM);
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        addView(scroll, new LayoutParams(-1, -1));

        header = new ChatHeaderView(activity);
        addView(header, new LayoutParams(-1, -2, Gravity.TOP));

        input = new InputBarView(activity);
        addView(input, new LayoutParams(-1, -2, Gravity.BOTTOM));
        if (activity instanceof org.telegram.lab.LabActivity && ((org.telegram.lab.LabActivity) activity).drawsStatusBar()) {
            input.setKeyboardEnabled(false);
        }

        overlay = new FrameLayout(activity);
        overlay.setElevation(AndroidUtilities.dp(24));
        addView(overlay, new LayoutParams(-1, -1));

        header.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> updatePadding());
        input.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> updatePadding());
    }

    /** Lets a feature take over gestures before the list (two-finger pinch, etc.). */
    public interface TouchHook {
        boolean dispatch(android.view.MotionEvent e);
    }

    public TouchHook touchHook;
    private boolean hooked;

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent e) {
        if (touchHook != null && touchHook.dispatch(e)) {
            if (!hooked) {
                hooked = true;
                android.view.MotionEvent c = android.view.MotionEvent.obtain(e);
                c.setAction(android.view.MotionEvent.ACTION_CANCEL);
                super.dispatchTouchEvent(c);
                c.recycle();
            }
            int a = e.getActionMasked();
            if (a == android.view.MotionEvent.ACTION_UP || a == android.view.MotionEvent.ACTION_CANCEL) {
                hooked = false;
            }
            return true;
        }
        if (e.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) {
            hooked = false;
        }
        return super.dispatchTouchEvent(e);
    }

    private void updatePadding() {
        int top = header.getHeight() + AndroidUtilities.dp(4);
        int bottom = input.getHeight() + AndroidUtilities.dp(4);
        if (scroll.getPaddingTop() != top || scroll.getPaddingBottom() != bottom) {
            boolean atBottom = isAtBottom();
            scroll.setPadding(0, top, 0, bottom);
            if (atBottom) {
                scroll.post(() -> scroll.scrollTo(0, Integer.MAX_VALUE / 4));
            }
        }
    }

    public boolean isAtBottom() {
        View child = scroll.getChildAt(0);
        return child == null || scroll.getScrollY() + scroll.getHeight() - scroll.getPaddingTop() - scroll.getPaddingBottom() >= child.getHeight() - AndroidUtilities.dp(8);
    }

    public ServiceCell addDate(String text) {
        ServiceCell c = new ServiceCell(getContext(), text);
        list.addView(c);
        return c;
    }

    public <T extends View> T add(T v) {
        list.addView(v, new LinearLayout.LayoutParams(-1, -2));
        return v;
    }

    /** Adds a view at the bottom with Telegram's slide-up appearance. */
    public <T extends View> T addAnimated(T v) {
        list.addView(v, new LinearLayout.LayoutParams(-1, -2));
        v.setAlpha(0f);
        v.setTranslationY(AndroidUtilities.dp(30));
        v.post(() -> {
            scroll.smoothScrollTo(0, list.getHeight());
            v.animate().alpha(1).translationY(0).setDuration(240).setInterpolator(new PathInterpolator(0.2f, 0.9f, 0.3f, 1)).start();
        });
        return v;
    }

    public void scrollToBottom() {
        scroll.post(() -> scroll.scrollTo(0, list.getHeight()));
    }

    public void scrollToView(View v, boolean highlight) {
        int target = v.getTop() - (scroll.getHeight() - scroll.getPaddingTop() - scroll.getPaddingBottom()) / 2 + v.getHeight() / 2;
        scroll.smoothScrollTo(0, Math.max(0, target));
        if (highlight && v instanceof MessageCell) {
            v.postDelayed(((MessageCell) v)::highlight, 250);
        }
    }
}
