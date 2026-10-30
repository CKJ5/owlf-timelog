package com.ckj5.owlf;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

/** What "Done" and "Snooze" do, shared by the notification buttons and the full-screen screen. */
final class ReminderOps {
    private ReminderOps() {}

    static int notifId(String id) {
        return 3000 + Math.abs(id.hashCode() % 50000);
    }

    static void done(Context c, String id, String kind, String ref) {
        try {
            JSONObject a = new JSONObject();
            a.put("action", "reminderDone");
            a.put("kind", kind);
            a.put("ref", ref);
            Prefs.addPending(c, a);
        } catch (JSONException ignored) {
        }
        Notifs.nm(c).cancel(notifId(id));
        if (OwlfPlugin.instance != null) OwlfPlugin.instance.pendingChanged();
    }

    static void snooze(Context c, String id, String title, String body, String kind, String ref) {
        Alarms.snooze(c, id, title, body, kind, ref, 10);
        Notifs.nm(c).cancel(notifId(id));
    }
}
