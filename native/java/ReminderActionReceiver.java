package com.ckj5.owlf;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class ReminderActionReceiver extends BroadcastReceiver {
    static final String ACTION_DONE = "com.ckj5.owlf.REMINDER_DONE";
    static final String ACTION_SNOOZE = "com.ckj5.owlf.REMINDER_SNOOZE";

    static PendingIntent pending(Context c, String action, String id, String title, String body, String kind, String ref, int req) {
        Intent i = new Intent(c, ReminderActionReceiver.class);
        i.setAction(action);
        i.setData(Uri.parse("owlf://reminder/" + Uri.encode(id) + "/" + action));
        i.putExtra("id", id).putExtra("title", title).putExtra("body", body).putExtra("kind", kind).putExtra("ref", ref);
        return PendingIntent.getBroadcast(c, req, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onReceive(Context c, Intent i) {
        String id = i.getStringExtra("id");
        if (id == null) return;
        String kind = i.getStringExtra("kind");
        String ref = i.getStringExtra("ref");
        if (ACTION_DONE.equals(i.getAction())) {
            ReminderOps.done(c, id, kind, ref);
        } else if (ACTION_SNOOZE.equals(i.getAction())) {
            ReminderOps.snooze(c, id, i.getStringExtra("title"), i.getStringExtra("body"), kind, ref);
        }
    }
}
