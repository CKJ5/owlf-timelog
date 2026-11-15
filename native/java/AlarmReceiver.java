package com.ckj5.owlf;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.provider.Settings;

import org.json.JSONException;
import org.json.JSONObject;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        String id = i.getStringExtra("id");
        if (id == null) {
            Prefs.log(c, "alarm fired but had no id");
            return;
        }
        fire(c, id, i.getStringExtra("title"), i.getStringExtra("body"), i.getStringExtra("kind"),
                i.getStringExtra("ref"), i.getLongExtra("at", System.currentTimeMillis()));
    }

    /** Shows the alarm-style notification + full-screen reminder. Also used by the "Show now" test button. */
    static void fire(Context c, String id, String title, String body, String kind, String ref, long at) {
        Prefs.log(c, "fired " + id);
        try {
            Notifs.ensureChannels(c);

            int nid = ReminderOps.notifId(id);
            Intent full = new Intent(c, ReminderActivity.class);
            full.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
            full.putExtra("id", id).putExtra("title", title).putExtra("body", body)
                    .putExtra("kind", kind).putExtra("ref", ref).putExtra("at", at);
            PendingIntent fullPi = PendingIntent.getActivity(c, nid, full, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            Notification.Action done = new Notification.Action.Builder(
                    Icon.createWithResource(c, R.drawable.ic_stat_owl), "plan".equals(kind) ? "Start" : "Done",
                    ReminderActionReceiver.pending(c, ReminderActionReceiver.ACTION_DONE, id, title, body, kind, ref, nid + 1)).build();
            Notification.Action snooze = new Notification.Action.Builder(
                    Icon.createWithResource(c, R.drawable.ic_stat_owl), "Snooze 10 min",
                    ReminderActionReceiver.pending(c, ReminderActionReceiver.ACTION_SNOOZE, id, title, body, kind, ref, nid + 2)).build();

            Notification n = new Notification.Builder(c, Notifs.CH_REMINDER)
                    .setSmallIcon(R.drawable.ic_stat_owl)
                    .setContentTitle(title == null ? "Reminder" : title)
                    .setContentText(body == null ? "" : body)
                    .setStyle(new Notification.BigTextStyle().bigText(body == null ? "" : body))
                    .setCategory(Notification.CATEGORY_ALARM)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setFullScreenIntent(fullPi, true)
                    .setContentIntent(fullPi)
                    .setAutoCancel(true)
                    .setTimeoutAfter(180_000)
                    .addAction(done)
                    .addAction(snooze)
                    .build();
            n.flags |= Notification.FLAG_INSISTENT; // keep ringing until the user reacts
            Notifs.nm(c).notify(nid, n);
            Prefs.log(c, "notification posted (enabled=" + Notifs.nm(c).areNotificationsEnabled() + ")");

            // With "display over other apps" granted, open the full-screen reminder even when the phone is unlocked.
            if (Settings.canDrawOverlays(c)) {
                try {
                    c.startActivity(full);
                } catch (Exception e) {
                    Prefs.log(c, "full-screen start failed: " + e);
                }
            }
        } catch (Throwable t) {
            // Never end up with nothing: fall back to a plain notification and record why.
            Prefs.log(c, "alarm notification failed: " + t);
            try {
                Notifs.postAlert(c, title == null ? "Reminder" : title, body == null ? "" : body, "fallback");
            } catch (Throwable t2) {
                Prefs.log(c, "fallback failed: " + t2);
            }
        }

        try {
            JSONObject a = new JSONObject();
            a.put("action", "reminderFired");
            a.put("kind", kind);
            a.put("ref", ref);
            a.put("at", at);
            Prefs.addPending(c, a);
        } catch (JSONException ignored) {
        }
        if (OwlfPlugin.instance != null) OwlfPlugin.instance.pendingChanged();
    }
}
