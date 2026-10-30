package com.ckj5.owlf;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import org.json.JSONObject;

/** Keeps a pinned notification (with a live chronometer) while any timer or workout is running. */
public class TimerService extends Service {
    private static final int NID = 1001;
    private static boolean running = false;

    /** Called whenever the web app pushes fresh state. */
    static void apply(Context c) {
        JSONObject st = Prefs.state(c);
        JSONObject run = st.optJSONObject("running");
        if (run == null) {
            if (running) c.stopService(new Intent(c, TimerService.class));
            Notifs.nm(c).cancel(NID);
            return;
        }
        if (running) {
            Notifs.nm(c).notify(NID, build(c, st, run));
            return;
        }
        try {
            c.startForegroundService(new Intent(c, TimerService.class));
        } catch (Exception e) {
            // Started from the background: fall back to a plain ongoing notification.
            Notifs.ensureChannels(c);
            Notifs.nm(c).notify(NID, build(c, st, run));
        }
    }

    static Notification build(Context c, JSONObject st, JSONObject run) {
        Notifs.ensureChannels(c);
        String label = run.optString("label", "Timer");
        long start = run.optLong("start", System.currentTimeMillis());
        int count = st.optInt("count", 1);
        StringBuilder sb = new StringBuilder();
        sb.append("Work ").append(fmt(st.optInt("workMin", 0)));
        if (st.optInt("workTarget", 0) > 0) sb.append(" / ").append(fmt(st.optInt("workTarget", 0)));
        if (st.optInt("exTarget", 0) > 0) sb.append("  ·  Ex ").append(st.optInt("exTotal", 0)).append("/").append(st.optInt("exTarget", 0));
        if (count > 1) sb.append("  ·  +").append(count - 1).append(" more");
        String ref = run.optString("ref", "");

        Notification.Builder b = new Notification.Builder(c, Notifs.CH_TIMER)
                .setSmallIcon(R.drawable.ic_stat_owl)
                .setContentTitle("⏱ " + label)
                .setContentText(sb.toString())
                .setUsesChronometer(true)
                .setWhen(start)
                .setShowWhen(true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(Notifs.openApp(c, null, null, 11))
                .addAction(new Notification.Action.Builder(
                        android.graphics.drawable.Icon.createWithResource(c, R.drawable.ic_stat_owl), "Stop",
                        Notifs.openApp(c, "stop", ref, 12)).build())
                .addAction(new Notification.Action.Builder(
                        android.graphics.drawable.Icon.createWithResource(c, R.drawable.ic_stat_owl), "Open",
                        Notifs.openApp(c, null, null, 13)).build());
        if (Build.VERSION.SDK_INT >= 31) b.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        return b.build();
    }

    static String fmt(int min) {
        return min >= 60 ? (min / 60) + "h " + (min % 60) + "m" : min + "m";
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        JSONObject st = Prefs.state(this);
        JSONObject run = st.optJSONObject("running");
        if (run == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        Notification n = build(this, st, run);
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NID, n);
        }
        running = true;
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
