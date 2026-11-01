package org.telegram.lab.snoozechat;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.lab.LabActivity;
import org.telegram.lab.R;
import org.telegram.lab.StatusBarView;
import org.telegram.lab.Tg;
import org.telegram.lab.TouchDriver;
import org.telegram.lab.ui.Bulletin;
import org.telegram.lab.ui.ChatHeaderView;
import org.telegram.lab.ui.ChatListScreen;
import org.telegram.lab.ui.DialogCell;
import org.telegram.lab.ui.MenuRow;
import org.telegram.lab.ui.SheetView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Components.SnoozeController;
import org.telegram.ui.Components.SwipeActionLayout;

import java.util.Calendar;
import java.util.HashMap;

public class MainActivity extends LabActivity {

    static MainActivity instance;

    private ChatListScreen screen;
    private SnoozeController snooze;
    private long clockBase, clockOffset, started;
    private final HashMap<Long, SwipeActionLayout> rows = new HashMap<>();
    private DialogCell snoozedRow;
    private SheetView sheet;
    private MenuRow firstOption;
    private SwipeActionLayout designRow;

    @Override
    protected void onCreateScreen(FrameLayout content) {
        instance = this;
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 9);
        c.set(Calendar.MINUTE, 41);
        c.set(Calendar.SECOND, 0);
        clockBase = c.getTimeInMillis();
        started = android.os.SystemClock.elapsedRealtime();
        snooze = new SnoozeController(this, this::now);
        for (long id : snooze.getSnoozed()) {
            snooze.unsnooze(id);
        }
        snooze.setListener(this::onWake);

        screen = new ChatListScreen(this);
        content.addView(screen);
        screen.addFolder("All", 9, true);
        screen.addFolder("Work", 3, false);
        screen.addFolder("Friends", 0, false);
        screen.addFolder("Channels", 0, false);

        DialogCell saved = add(1, "Saved Messages", null, "Boarding pass.pdf", "9:12", 0);
        saved.pinned = true;
        saved.avatar = new IconCircle(0xFF52A8F5, Tg.icon(this, R.drawable.tg_chats_saved, 0xFFFFFFFF));
        add(2, "Emma Torres", null, "Sounds good, talk later!", "9:38", 0).online = true;
        designRow = row(3);
        DialogCell design = add(3, "Design Team", "Mia", "New icon set is ready for review", "9:40", 14);
        design.muted = false;
        add(4, "Roberto", null, "Say hello to Emma 👋", "9:31", 2);
        add(5, "Digital Nomads", "Jennie", "We just reached 2,500 members!", "9:22", 0).muted = true;
        add(6, "Mom", null, "Call me when you're free ❤️", "8:57", 1);
        add(7, "Book Club", "Leo", "Next meeting is on Thursday", "Mon", 0);
        add(8, "Lucy Garner", null, "Photo", "Sun", 0);
        add(9, "Weekend Hikes", "Sam", "Weather looks great for Saturday", "Sat", 0);
        designRow = rows.get(3L);
        updateClock();
        tick();
    }

    private long now() {
        return clockBase + clockOffset + (android.os.SystemClock.elapsedRealtime() - started) / 60;
    }

    private void tick() {
        updateClock();
        phone.postDelayed(this::tick, 1000);
    }

    private void updateClock() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(now());
        StatusBarView.clock = c.get(Calendar.HOUR_OF_DAY) + ":" + String.format(java.util.Locale.US, "%02d", c.get(Calendar.MINUTE));
        screen.top.getChildAt(0).invalidate();
    }

    private SwipeActionLayout row(long id) {
        return rows.get(id);
    }

    private DialogCell add(long id, String name, String sender, String msg, String time, int unread) {
        DialogCell cell = new DialogCell(this);
        cell.id = id;
        cell.set(name, sender, msg, time, unread);
        SwipeActionLayout swipe = new SwipeActionLayout(this, cell, Tg.orange, Tg.icon(this, R.drawable.tg_msg_mute_period, 0xFFFFFFFF), "Snooze");
        swipe.setListener(l -> openSheet((DialogCell) l.getRow()));
        rows.put(id, swipe);
        screen.list.addView(swipe, new LinearLayout.LayoutParams(-1, -2));
        return cell;
    }

    private void openSheet(DialogCell cell) {
        sheet = new SheetView(this);
        sheet.header("Snooze " + cell.name, "The chat leaves your list and comes back with a reminder. Notifications stay silent until then.");
        firstOption = null;
        for (SnoozeController.Option o : snooze.options()) {
            MenuRow r = new MenuRow(this, R.drawable.tg_msg_mute_period, 0, o.title, null);
            r.value.setText(snooze.format(o.until));
            r.setOnClickListener(v -> {
                sheet.dismiss();
                doSnooze(cell, o.until);
            });
            sheet.panel.addView(r);
            if (firstOption == null) {
                firstOption = r;
            }
        }
        MenuRow custom = new MenuRow(this, R.drawable.tg_menu_premium_clock, 0, "Pick date and time", null);
        sheet.panel.addView(custom);
        sheet.show(screen.overlay);
    }

    private void doSnooze(DialogCell cell, long until) {
        snooze.snooze(cell.id, until);
        snooze.scheduleAlarm(this, SnoozeReceiver.class);
        SwipeActionLayout row = rows.get(cell.id);
        phone.postDelayed(() -> collapse(row, () -> screen.list.removeView(row)), 260);
        phone.postDelayed(() -> {
            showSnoozedRow();
            Bulletin.make(screen.overlay, R.drawable.tg_msg_mute_period, cell.name + " is snoozed until " + snooze.format(until))
                    .button("Undo", v -> {
                        snooze.unsnooze(cell.id);
                        restore(cell, false);
                    }).show(3500);
        }, 620);
    }

    private void showSnoozedRow() {
        StringBuilder names = new StringBuilder();
        long next = snooze.nextWake();
        for (long id : snooze.getSnoozed()) {
            DialogCell c = (DialogCell) rows.get(id).getRow();
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(c.name);
        }
        if (snoozedRow == null) {
            snoozedRow = new DialogCell(this);
            snoozedRow.avatar = new IconCircle(Tg.orange, Tg.icon(this, R.drawable.tg_msg_mute_period, 0xFFFFFFFF));
            snoozedRow.set("Snoozed", null, "", "", 0);
            screen.list.addView(snoozedRow, 0, new LinearLayout.LayoutParams(-1, -2));
            expand(snoozedRow);
        }
        snoozedRow.message = names + " · back at " + snooze.format(next);
        snoozedRow.invalidate();
    }

    void checkSnoozed() {
        snooze.check();
    }

    private void onWake(long id, long since) {
        DialogCell cell = (DialogCell) rows.get(id).getRow();
        if (snooze.getSnoozed().isEmpty() && snoozedRow != null) {
            View r = snoozedRow;
            snoozedRow = null;
            collapse(r, () -> screen.list.removeView(r));
        } else {
            showSnoozedRow();
        }
        restore(cell, true);
        long mins = Math.max(1, (now() - since) / 60000);
        showBanner(cell.name + " is back", "You snoozed it " + (mins >= 60 ? (mins / 60) + " hour" + (mins >= 120 ? "s" : "") : mins + " min") + " ago · " + cell.unread + " new messages");
    }

    private void restore(DialogCell cell, boolean woke) {
        SwipeActionLayout row = rows.get(cell.id);
        if (row.getParent() != null) {
            ((ViewGroup) row.getParent()).removeView(row);
        }
        row.getRow().setTranslationX(0);
        if (woke) {
            cell.unread += 7;
            cell.sender = "Alex";
            cell.message = "Shipping the build at 11, any blockers?";
            cell.time = StatusBarView.clock;
            cell.badgeIcon = Tg.icon(this, R.drawable.tg_msg_mute_period, 0xFFFFFFFF);
            cell.badgeColor = Tg.orange;
            cell.badgeText = "Reminder";
        }
        int index = snoozedRow != null ? 1 : 0;
        screen.list.addView(row, Math.min(index + 1, screen.list.getChildCount()), new LinearLayout.LayoutParams(-1, -2));
        expand(row);
        if (woke) {
            ValueAnimator p = ValueAnimator.ofFloat(0, 1, 0, 1, 0);
            p.setDuration(1600);
            p.setStartDelay(300);
            p.addUpdateListener(a -> cell.setPulse((float) a.getAnimatedValue()));
            p.start();
        }
    }

    private void showBanner(String title, String text) {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(ChatHeaderView.glass(AndroidUtilities.dp(22)));
        card.setElevation(AndroidUtilities.dp(8));
        card.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(new IconCircle(Tg.orange, Tg.icon(this, R.drawable.tg_msg_mute_period, 0xFFFFFFFF)));
        card.addView(icon, new LinearLayout.LayoutParams(AndroidUtilities.dp(40), AndroidUtilities.dp(40)));
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView t = Tg.text(this, 15.5f, Tg.text, true);
        t.setText(title);
        TextView s = Tg.text(this, 13.5f, Tg.textGray2, false);
        s.setText(text);
        texts.addView(t);
        texts.addView(s);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, -2, 1f);
        tl.leftMargin = AndroidUtilities.dp(12);
        card.addView(texts, tl);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        lp.leftMargin = lp.rightMargin = AndroidUtilities.dp(10);
        lp.topMargin = LabActivity.statusBarHeight + AndroidUtilities.dp(6);
        screen.overlay.addView(card, lp);
        card.setTranslationY(-AndroidUtilities.dp(140));
        card.animate().translationY(0).setDuration(420).setInterpolator(new OvershootInterpolator(0.9f)).start();
        card.postDelayed(() -> card.animate().translationY(-AndroidUtilities.dp(160)).setDuration(300).withEndAction(() -> screen.overlay.removeView(card)).start(), 3800);
    }

    private void collapse(View v, Runnable end) {
        int h = v.getHeight();
        ValueAnimator va = ValueAnimator.ofInt(h, 0);
        va.setDuration(280);
        va.setInterpolator(new PathInterpolator(0.4f, 0, 0.2f, 1));
        va.addUpdateListener(a -> {
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            lp.height = (int) a.getAnimatedValue();
            v.setLayoutParams(lp);
            v.setAlpha(h == 0 ? 0 : lp.height / (float) h);
        });
        va.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                end.run();
                ViewGroup.LayoutParams lp = v.getLayoutParams();
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                v.setLayoutParams(lp);
                v.setAlpha(1);
            }
        });
        va.start();
    }

    private void expand(View v) {
        int target = AndroidUtilities.dp(74);
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        lp.height = 0;
        v.setLayoutParams(lp);
        ValueAnimator va = ValueAnimator.ofInt(0, target);
        va.setDuration(320);
        va.setInterpolator(new PathInterpolator(0.2f, 0.9f, 0.3f, 1));
        va.addUpdateListener(a -> {
            ViewGroup.LayoutParams p = v.getLayoutParams();
            p.height = (int) a.getAnimatedValue();
            v.setLayoutParams(p);
        });
        va.start();
    }

    /** Demo only: jump the clock forward so the snooze can end on camera. */
    private void timeLapse(long ms, int duration, Runnable done) {
        TextView chip = Tg.text(this, 15, 0xFFFFFFFF, true);
        chip.setText("⏩  1 hour later");
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(10), AndroidUtilities.dp(18), AndroidUtilities.dp(10));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xCC1F2023);
        bg.setCornerRadius(AndroidUtilities.dp(22));
        chip.setBackground(bg);
        screen.overlay.addView(chip, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        chip.setAlpha(0);
        chip.animate().alpha(1).setDuration(200).start();
        long from = clockOffset;
        ValueAnimator va = ValueAnimator.ofFloat(0, 1);
        va.setDuration(duration);
        va.setInterpolator(new PathInterpolator(0.5f, 0, 0.5f, 1));
        va.addUpdateListener(a -> {
            clockOffset = from + (long) (ms * (float) a.getAnimatedValue());
            updateClock();
        });
        va.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                chip.animate().alpha(0).setDuration(200).withEndAction(() -> screen.overlay.removeView(chip)).start();
                done.run();
            }
        });
        va.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        snooze.check();
    }

    @Override
    protected void onAutoplay(TouchDriver d) {
        d.wait(800)
                .zoom(1.55f, () -> designRow, 900)
                .wait(300)
                .press(() -> designRow, 0.8f, 0.5f)
                .moveBy(-AndroidUtilities.dp(190), 0, 650)
                .hold(250)
                .release()
                .wait(700)
                .zoom(1.45f, () -> sheet.panel, 800)
                .wait(900)
                .tap(() -> firstOption)
                .wait(900)
                .resetZoom(900)
                .wait(2600)
                .zoom(1.5f, () -> snoozedRow, 800)
                .wait(1600)
                .resetZoom(800)
                .step(done -> timeLapse(60 * 60_000L, 2600, done))
                .run(this::checkSnoozed)
                .wait(600)
                .zoom(1.5f, () -> designRow, 900)
                .wait(3200)
                .resetZoom(900)
                .wait(1200);
    }

    /** Round colored avatar with an icon, used for the "Snoozed" row. */
    static class IconCircle extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Drawable icon;

        IconCircle(int color, Drawable icon) {
            paint.setColor(color);
            this.icon = icon;
        }

        @Override
        public void draw(Canvas canvas) {
            android.graphics.Rect b = getBounds();
            canvas.drawCircle(b.exactCenterX(), b.exactCenterY(), b.width() / 2f, paint);
            int p = b.width() / 4;
            icon.setBounds(b.left + p, b.top + p, b.right - p, b.bottom - p);
            icon.draw(canvas);
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
