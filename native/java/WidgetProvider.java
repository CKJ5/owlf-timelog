package com.ckj5.owlf;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONObject;

public class WidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) m.updateAppWidget(id, build(c));
    }

    static void refreshAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, WidgetProvider.class));
        for (int id : ids) m.updateAppWidget(id, build(c));
    }

    static RemoteViews build(Context c) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_owlf);
        JSONObject st = Prefs.state(c);
        JSONObject run = st.optJSONObject("running");

        int workMin = st.optInt("workMin", 0), workTarget = st.optInt("workTarget", 0);
        StringBuilder prog = new StringBuilder("Work " + TimerService.fmt(workMin));
        if (workTarget > 0) prog.append(" / ").append(TimerService.fmt(workTarget));
        if (st.optInt("exTarget", 0) > 0) prog.append("  ·  Ex ").append(st.optInt("exTotal", 0)).append("/").append(st.optInt("exTarget", 0));
        v.setTextViewText(R.id.w_progress, prog.toString());

        if (run != null) {
            long start = run.optLong("start", System.currentTimeMillis());
            v.setTextViewText(R.id.w_label, run.optString("label", "Timer"));
            v.setViewVisibility(R.id.w_chrono, View.VISIBLE);
            v.setChronometer(R.id.w_chrono, SystemClock.elapsedRealtime() - (System.currentTimeMillis() - start), null, true);
            v.setTextViewText(R.id.w_btn1, "■ Stop");
            v.setOnClickPendingIntent(R.id.w_btn1, Notifs.openApp(c, "stop", run.optString("ref", ""), 21));
        } else {
            v.setTextViewText(R.id.w_label, "No timer running");
            v.setViewVisibility(R.id.w_chrono, View.GONE);
            v.setChronometer(R.id.w_chrono, SystemClock.elapsedRealtime(), null, false);
            v.setTextViewText(R.id.w_btn1, "▶ Work");
            v.setOnClickPendingIntent(R.id.w_btn1, Notifs.openApp(c, "work", null, 22));
        }
        String next = st.optString("next", "");
        v.setTextViewText(R.id.w_next, next.isEmpty() ? "" : "Next · " + next);
        v.setViewVisibility(R.id.w_next, next.isEmpty() ? View.GONE : View.VISIBLE);

        v.setOnClickPendingIntent(R.id.w_btn2, Notifs.openApp(c, "workout", null, 23));
        v.setOnClickPendingIntent(R.id.w_btn3, Notifs.openApp(c, "plan", null, 24));
        v.setOnClickPendingIntent(R.id.w_btn4, Notifs.openApp(c, "todo", null, 25));
        v.setOnClickPendingIntent(R.id.w_top, Notifs.openApp(c, null, null, 26));
        return v;
    }
}
