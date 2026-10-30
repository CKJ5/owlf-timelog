package com.ckj5.owlf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Re-arms reminders after reboot, app update, or a clock/time-zone change. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        Alarms.rescheduleFromPrefs(c);
        WidgetProvider.refreshAll(c);
    }
}
