package org.telegram.lab.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.lab.LabActivity;
import org.telegram.lab.R;
import org.telegram.lab.Tg;
import org.telegram.messenger.AndroidUtilities;

/**
 * Telegram 12.4 chat list: blue "Telegram" title, search capsule, folder pills,
 * the list and the floating glass tab bar (Chats, Contacts, Settings, Profile).
 */
public class ChatListScreen extends FrameLayout {

    public final ScrollView scroll;
    public final LinearLayout list;
    public final LinearLayout top;
    public final TextView title;
    public final ImageView menu;
    public final LinearLayout folders;
    public final LinearLayout tabBar;
    public final FrameLayout fab;
    public final FrameLayout overlay;

    public ChatListScreen(Activity activity) {
        super(activity);
        Context context = activity;
        setBackgroundColor(Color.WHITE);

        scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(OVER_SCROLL_NEVER);
        scroll.setClipToPadding(false);
        list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        addView(scroll, new LayoutParams(-1, -1));

        top = new LinearLayout(context);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setBackgroundColor(0xF7FFFFFF);
        top.addView(LabActivity.statusBar(activity, false));

        FrameLayout bar = new FrameLayout(context);
        title = Tg.text(context, 22, Tg.accent, true);
        title.setText("Telegram");
        FrameLayout.LayoutParams tl = new FrameLayout.LayoutParams(-2, -1, Gravity.LEFT);
        tl.leftMargin = AndroidUtilities.dp(18);
        bar.addView(title, tl);
        menu = ChatHeaderView.iconButton(context, R.drawable.tg_ic_ab_other);
        menu.setImageDrawable(Tg.icon(context, R.drawable.tg_ic_ab_other, 0xFF222428));
        FrameLayout.LayoutParams ml = new FrameLayout.LayoutParams(AndroidUtilities.dp(48), AndroidUtilities.dp(48), Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        ml.rightMargin = AndroidUtilities.dp(6);
        bar.addView(menu, ml);
        top.addView(bar, new LinearLayout.LayoutParams(-1, AndroidUtilities.dp(56)));

        LinearLayout search = new LinearLayout(context);
        search.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable sb = new GradientDrawable();
        sb.setColor(0xFFF0F1F4);
        sb.setCornerRadius(AndroidUtilities.dp(20));
        search.setBackground(sb);
        ImageView si = new ImageView(context);
        si.setImageDrawable(Tg.icon(context, R.drawable.tg_msg_search, 0xFF8A8E94));
        si.setScaleType(ImageView.ScaleType.CENTER);
        search.addView(si, new LinearLayout.LayoutParams(AndroidUtilities.dp(44), AndroidUtilities.dp(40)));
        TextView st = Tg.text(context, 16, 0xFF8A8E94, false);
        st.setText("Search Chats");
        search.addView(st);
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(-1, AndroidUtilities.dp(40));
        sl.leftMargin = sl.rightMargin = AndroidUtilities.dp(12);
        top.addView(search, sl);

        HorizontalScrollView fs = new HorizontalScrollView(context);
        fs.setHorizontalScrollBarEnabled(false);
        folders = new LinearLayout(context);
        folders.setGravity(Gravity.CENTER_VERTICAL);
        folders.setPadding(AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12), 0);
        fs.addView(folders);
        LinearLayout.LayoutParams fl = new LinearLayout.LayoutParams(-1, AndroidUtilities.dp(48));
        top.addView(fs, fl);
        addView(top, new LayoutParams(-1, -2, Gravity.TOP));

        tabBar = new LinearLayout(context);
        tabBar.setGravity(Gravity.CENTER_VERTICAL);
        tabBar.setBackground(ChatHeaderView.glass(AndroidUtilities.dp(32)));
        tabBar.setElevation(AndroidUtilities.dp(6));
        tabBar.setPadding(AndroidUtilities.dp(5), AndroidUtilities.dp(5), AndroidUtilities.dp(5), AndroidUtilities.dp(5));
        addTab(R.raw.tg_tab_chats, "Chats", true, 8);
        addTab(R.raw.tg_tab_contacts, "Contacts", false, 0);
        addTab(R.raw.tg_tab_settings, "Settings", false, 0);
        addTab(0, "Profile", false, 0);
        LayoutParams tb = new LayoutParams(-1, AndroidUtilities.dp(64), Gravity.BOTTOM);
        tb.leftMargin = tb.rightMargin = AndroidUtilities.dp(28);
        tb.bottomMargin = AndroidUtilities.dp(14);
        addView(tabBar, tb);

        fab = new FrameLayout(context);
        GradientDrawable fb = new GradientDrawable();
        fb.setShape(GradientDrawable.OVAL);
        fb.setColor(Tg.accent);
        fab.setBackground(fb);
        fab.setElevation(AndroidUtilities.dp(5));
        ImageView fi = new ImageView(context);
        fi.setImageDrawable(Tg.icon(context, R.drawable.tg_floating_pencil, Color.WHITE));
        fi.setScaleType(ImageView.ScaleType.CENTER);
        fab.addView(fi, new LayoutParams(-1, -1));
        LayoutParams fp = new LayoutParams(AndroidUtilities.dp(52), AndroidUtilities.dp(52), Gravity.BOTTOM | Gravity.RIGHT);
        fp.rightMargin = AndroidUtilities.dp(18);
        fp.bottomMargin = AndroidUtilities.dp(92);
        addView(fab, fp);

        overlay = new FrameLayout(context);
        overlay.setElevation(AndroidUtilities.dp(24));
        addView(overlay, new LayoutParams(-1, -1));

        top.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (scroll.getPaddingTop() != v.getHeight()) {
                scroll.setPadding(0, v.getHeight(), 0, AndroidUtilities.dp(96));
            }
        });
    }

    private void addTab(int icon, String label, boolean selected, int badge) {
        Context context = getContext();
        FrameLayout tab = new FrameLayout(context);
        if (selected) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0x1F3390EC);
            bg.setCornerRadius(AndroidUtilities.dp(27));
            tab.setBackground(bg);
        }
        LinearLayout col = new LinearLayout(context);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        int color = selected ? Tg.accent : 0xFF55585E;
        // The tab icons are Telegram's own Lottie files (res/raw/tab_*.json).
        if (icon != 0) {
            com.airbnb.lottie.LottieAnimationView lv = new com.airbnb.lottie.LottieAnimationView(context);
            lv.setAnimation(icon);
            // Last frame is the filled "selected" state, the first one is the outline.
            lv.setProgress(selected ? 1f : 0f);
            lv.addValueCallback(new com.airbnb.lottie.model.KeyPath("**"), com.airbnb.lottie.LottieProperty.COLOR_FILTER,
                    new com.airbnb.lottie.value.LottieValueCallback<>(new android.graphics.PorterDuffColorFilter(color, android.graphics.PorterDuff.Mode.SRC_ATOP)));
            col.addView(lv, new LinearLayout.LayoutParams(AndroidUtilities.dp(28), AndroidUtilities.dp(28)));
        } else {
            ImageView iv = new ImageView(context);
            iv.setImageDrawable(new AvatarDrawable("Max Lee", 5));
            LinearLayout.LayoutParams al = new LinearLayout.LayoutParams(AndroidUtilities.dp(24), AndroidUtilities.dp(24));
            al.topMargin = al.bottomMargin = AndroidUtilities.dp(2);
            col.addView(iv, al);
        }
        TextView tv = Tg.text(context, 11.5f, color, true);
        tv.setText(label);
        tv.setGravity(Gravity.CENTER);
        col.addView(tv, new LinearLayout.LayoutParams(-2, -2));
        tab.addView(col, new LayoutParams(-1, -1));
        if (badge > 0) {
            TextView b = Tg.text(context, 10.5f, Color.WHITE, true);
            b.setText(String.valueOf(badge));
            b.setGravity(Gravity.CENTER);
            GradientDrawable bb = new GradientDrawable();
            bb.setColor(Tg.accent);
            bb.setCornerRadius(AndroidUtilities.dp(9));
            bb.setStroke(AndroidUtilities.dp(1.5f), Color.WHITE);
            b.setBackground(bb);
            LayoutParams bl = new LayoutParams(AndroidUtilities.dp(18), AndroidUtilities.dp(18), Gravity.CENTER_HORIZONTAL | Gravity.TOP);
            bl.leftMargin = AndroidUtilities.dp(18);
            bl.topMargin = AndroidUtilities.dp(1);
            tab.addView(b, bl);
        }
        tabBar.addView(tab, new LinearLayout.LayoutParams(0, -1, 1f));
    }

    /** Folder pill: name plus a round counter (accent when selected, gray otherwise). */
    public LinearLayout addFolder(String name, int count, boolean selected) {
        LinearLayout pill = new LinearLayout(getContext());
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setPadding(AndroidUtilities.dp(13), 0, AndroidUtilities.dp(count > 0 ? 7 : 13), 0);
        if (selected) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xFFE4F0FD);
            bg.setCornerRadius(AndroidUtilities.dp(16));
            pill.setBackground(bg);
        }
        TextView t = Tg.text(getContext(), 14.5f, selected ? Tg.accent : 0xFF6D7076, true);
        t.setText(name);
        pill.addView(t);
        if (count > 0) {
            TextView c = Tg.text(getContext(), 11.5f, Color.WHITE, true);
            c.setText(String.valueOf(count));
            c.setGravity(Gravity.CENTER);
            GradientDrawable cb = new GradientDrawable();
            cb.setColor(selected ? Tg.accent : 0xFFB3B7BE);
            cb.setCornerRadius(AndroidUtilities.dp(9));
            c.setBackground(cb);
            c.setMinWidth(AndroidUtilities.dp(18));
            c.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
            LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(-2, AndroidUtilities.dp(18));
            cl.leftMargin = AndroidUtilities.dp(6);
            pill.addView(c, cl);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, AndroidUtilities.dp(32));
        lp.rightMargin = AndroidUtilities.dp(4);
        folders.addView(pill, lp);
        return pill;
    }

    public DialogCell addDialog(String name, String sender, String message, String time, int unread) {
        DialogCell c = new DialogCell(getContext());
        c.set(name, sender, message, time, unread);
        list.addView(c, new LinearLayout.LayoutParams(-1, -2));
        return c;
    }
}
