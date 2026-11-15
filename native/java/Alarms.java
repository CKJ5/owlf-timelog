package com.ckj5.owlf;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Exact reminders. Uses setAlarmClock: exact even in Doze and needs no special permission. */
final class Alarms {
    static final String ACTION = "com.ckj5.owlf.ALARM";
    private Alarms() {}

    static PendingIntent pending(Context c, String id, JSONObject alarm, int flags) {
        Intent i = new Intent(c, AlarmReceiver.class);
        i.setAction(ACTION);
        i.setData(Uri.parse("owlf://alarm/" + Uri.encode(id)));
        if (alarm != null) {
            i.putExtra("id", id);
            i.putExtra("title", alarm.optString("title", "Reminder"));
            i.putExtra("body", alarm.optString("body", ""));
            i.putExtra("kind", alarm.optString("kind", ""));
            i.putExtra("ref", alarm.optString("ref", ""));
            i.putExtra("at", alarm.optLong("at", 0));
        }
        return PendingIntent.getBroadcast(c, Math.abs(id.hashCode()), i, PendingIntent.FLAG_IMMUTABLE | flags);
    }

    static boolean schedule(Context c, JSONObject alarm) {
        String id = alarm.optString("id", "");
        long at = alarm.optLong("at", 0);
        if (id.isEmpty() || at <= System.currentTimeMillis()) return false;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent show = Notifs.openApp(c, null, null, 9);
        try {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, show), pending(c, id, alarm, PendingIntent.FLAG_UPDATE_CURRENT));
            Prefs.log(c, "scheduled " + id + " for " + new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(new java.util.Date(at)));
            return true;
        } catch (SecurityException e) {
            Prefs.log(c, "schedule refused: " + e);
            return false;
        }
    }

    /** Number of reminders still in the future, and the time of the next one (0 = none). */
    static int count(Context c) {
        JSONArray arr = Prefs.alarms(c);
        int n = 0;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null && o.optLong("at", 0) > System.currentTimeMillis()) n++;
        }
        return n;
    }

    static long nextAt(Context c) {
        JSONArray arr = Prefs.alarms(c);
        long best = 0;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            long at = o.optLong("at", 0);
            if (at > System.currentTimeMillis() && (best == 0 || at < best)) best = at;
        }
        return best;
    }

    static void cancel(Context c, String id) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = pending(c, id, null, PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(pi);
        pi.cancel();
    }

    /** Replace the whole set of scheduled reminders with a new list from the web app. */
    static void replaceAll(Context c, JSONArray fresh) {
        JSONArray old = Prefs.alarms(c);
        JSONArray keep = new JSONArray();
        long now = System.currentTimeMillis();
        for (int i = 0; i < old.length(); i++) {
            JSONObject o = old.optJSONObject(i);
            if (o == null) continue;
            String id = o.optString("id", "");
            if (id.endsWith("~snooze") && o.optLong("at", 0) > now) {
                keep.put(o); // a snooze the user asked for on the lock screen survives list refreshes
            } else {
                cancel(c, id);
            }
        }
        for (int i = 0; i < fresh.length(); i++) {
            JSONObject o = fresh.optJSONObject(i);
            if (o == null || o.optLong("at", 0) <= now) continue;
            if (schedule(c, o)) keep.put(o);
        }
        Prefs.putAlarms(c, keep);
    }

    /** After reboot / app update / clock change. */
    static void rescheduleFromPrefs(Context c) {
        JSONArray arr = Prefs.alarms(c);
        JSONArray keep = new JSONArray();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null || o.optLong("at", 0) <= System.currentTimeMillis()) continue;
            schedule(c, o);
            keep.put(o);
        }
        Prefs.putAlarms(c, keep);
    }

    static void snooze(Context c, String id, String title, String body, String kind, String ref, int minutes) {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id + "~snooze");
            o.put("title", title);
            o.put("body", body);
            o.put("kind", kind);
            o.put("ref", ref);
            o.put("at", System.currentTimeMillis() + minutes * 60_000L);
            schedule(c, o);
            JSONArray arr = Prefs.alarms(c);
            arr.put(o);
            Prefs.putAlarms(c, arr);
        } catch (JSONException ignored) {
        }
    }
}
