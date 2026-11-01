package org.telegram.ui.Components;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

/**
 * "Snooze chat": the chat leaves the list and comes back at a chosen time.
 *
 * Telegram for Android integration:
 *  - DialogsActivity / DialogsAdapter skip dialogs where {@link #isSnoozed(long)} is true
 *    and show one "Snoozed" row (like the archive row) while any exist;
 *  - NotificationsController treats a snoozed dialog as muted until it wakes;
 *  - {@link #scheduleAlarm(Context, Class)} wakes the app at the right time, and
 *    {@link #check()} runs from the receiver and from DialogsActivity.onResume.
 * Everything is local to the device, nothing is sent to the server.
 */
public class SnoozeController {

    public interface Clock {
        long now();
    }

    public interface Listener {
        void onWake(long dialogId, long snoozedAt);
    }

    public static class Option {
        public final String title;
        public final long until;

        Option(String title, long until) {
            this.title = title;
            this.until = until;
        }
    }

    private static final String KEY_UNTIL = "until_", KEY_SINCE = "since_";

    private final SharedPreferences prefs;
    private final Clock clock;
    private Listener listener;

    public SnoozeController(Context context, Clock clock) {
        prefs = context.getSharedPreferences("snoozed_dialogs", Context.MODE_PRIVATE);
        this.clock = clock;
    }

    public void setListener(Listener l) {
        listener = l;
    }

    public void snooze(long dialogId, long until) {
        prefs.edit().putLong(KEY_UNTIL + dialogId, until).putLong(KEY_SINCE + dialogId, clock.now()).apply();
    }

    public void unsnooze(long dialogId) {
        prefs.edit().remove(KEY_UNTIL + dialogId).remove(KEY_SINCE + dialogId).apply();
    }

    public boolean isSnoozed(long dialogId) {
        return getUntil(dialogId) > clock.now();
    }

    public long getUntil(long dialogId) {
        return prefs.getLong(KEY_UNTIL + dialogId, 0);
    }

    public ArrayList<Long> getSnoozed() {
        ArrayList<Long> ids = new ArrayList<>();
        for (Map.Entry<String, ?> e : prefs.getAll().entrySet()) {
            if (e.getKey().startsWith(KEY_UNTIL)) {
                ids.add(Long.parseLong(e.getKey().substring(KEY_UNTIL.length())));
            }
        }
        return ids;
    }

    /** Wakes every chat whose time has come. */
    public void check() {
        long now = clock.now();
        for (long id : getSnoozed()) {
            long until = getUntil(id);
            if (until <= now) {
                long since = prefs.getLong(KEY_SINCE + id, now);
                unsnooze(id);
                if (listener != null) {
                    listener.onWake(id, since);
                }
            }
        }
    }

    public long nextWake() {
        long next = 0;
        for (long id : getSnoozed()) {
            long u = getUntil(id);
            if (next == 0 || u < next) {
                next = u;
            }
        }
        return next;
    }

    public void scheduleAlarm(Context context, Class<?> receiver) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, new Intent(context, receiver), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        long next = nextWake();
        if (next == 0) {
            am.cancel(pi);
        } else {
            am.setWindow(AlarmManager.RTC_WAKEUP, next, 60_000, pi);
        }
    }

    /** Presets shown in the sheet, relative to the current time. */
    public ArrayList<Option> options() {
        ArrayList<Option> list = new ArrayList<>();
        long now = clock.now();
        list.add(new Option("In 1 hour", now + 3600_000L));
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(now);
        if (c.get(Calendar.HOUR_OF_DAY) < 18) {
            list.add(new Option("This evening", at(now, 0, 20)));
        }
        list.add(new Option("Tomorrow morning", at(now, 1, 9)));
        c.setTimeInMillis(now);
        int toMonday = (Calendar.MONDAY - c.get(Calendar.DAY_OF_WEEK) + 7) % 7;
        list.add(new Option("Next week", at(now, toMonday == 0 ? 7 : Math.max(toMonday, 2), 9)));
        return list;
    }

    private static long at(long now, int days, int hour) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(now);
        c.add(Calendar.DAY_OF_YEAR, days);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** "10:41", "Tue 9:00" or "Mon 9:00". */
    public String format(long time) {
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(clock.now());
        Calendar t = Calendar.getInstance();
        t.setTimeInMillis(time);
        String hm = String.format(Locale.US, "%d:%02d", t.get(Calendar.HOUR_OF_DAY), t.get(Calendar.MINUTE));
        if (now.get(Calendar.DAY_OF_YEAR) == t.get(Calendar.DAY_OF_YEAR) && now.get(Calendar.YEAR) == t.get(Calendar.YEAR)) {
            return hm;
        }
        return t.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US) + " " + hm;
    }
}
