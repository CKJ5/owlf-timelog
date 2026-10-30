package com.ckj5.owlf;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

final class Notifs {
    static final String CH_TIMER = "owlf_timer";
    static final String CH_ALERT = "owlf_alert";
    static final String CH_REMINDER = "owlf_reminder";
    private Notifs() {}

    static NotificationManager nm(Context c) {
        return (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    static void ensureChannels(Context c) {
        NotificationManager m = nm(c);
        NotificationChannel timer = new NotificationChannel(CH_TIMER, "Running timer", NotificationManager.IMPORTANCE_LOW);
        timer.setDescription("Pinned notification while a timer or workout is running");
        timer.setShowBadge(false);
        m.createNotificationChannel(timer);

        NotificationChannel alert = new NotificationChannel(CH_ALERT, "Alerts", NotificationManager.IMPORTANCE_DEFAULT);
        alert.setDescription("Rest finished, estimate reached and other short alerts");
        m.createNotificationChannel(alert);

        NotificationChannel rem = new NotificationChannel(CH_REMINDER, "Reminders (alarm style)", NotificationManager.IMPORTANCE_HIGH);
        rem.setDescription("Full-screen reminders for to-dos and planned blocks");
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (sound == null) sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        rem.setSound(sound, new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
        rem.enableVibration(true);
        rem.setVibrationPattern(new long[]{0, 500, 300, 500, 300, 800});
        rem.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        m.createNotificationChannel(rem);
    }

    static PendingIntent openApp(Context c, String action, String ref, int req) {
        Intent i = new Intent(c, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (action != null) i.putExtra("owlf_action", action);
        if (ref != null) i.putExtra("owlf_ref", ref);
        return PendingIntent.getActivity(c, req, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** Simple one-shot alert (rest over, estimate reached, ...). */
    static void postAlert(Context c, String title, String body, String tag) {
        ensureChannels(c);
        Notification n = new Notification.Builder(c, CH_ALERT)
                .setSmallIcon(R.drawable.ic_stat_owl)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(openApp(c, null, null, 7))
                .build();
        nm(c).notify(tag == null ? "owlf" : tag, 2001, n);
    }
}
