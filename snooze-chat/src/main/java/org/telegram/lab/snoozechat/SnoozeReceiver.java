package org.telegram.lab.snoozechat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** AlarmManager target: in Telegram it would call SnoozeController.check() and post a local notification. */
public class SnoozeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        MainActivity a = MainActivity.instance;
        if (a != null) {
            a.runOnUiThread(a::checkSnoozed);
        }
    }
}
