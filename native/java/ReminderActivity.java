package com.ckj5.owlf;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

/** Full-screen, alarm-style reminder. Shows over the lock screen. */
public class ReminderActivity extends Activity {
    private String id, title, body, kind, ref;
    private long at;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        read(getIntent());
        build();
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        read(i);
        build();
    }

    private void read(Intent i) {
        id = i.getStringExtra("id");
        if (id == null) id = "x";
        title = i.getStringExtra("title");
        body = i.getStringExtra("body");
        kind = i.getStringExtra("kind");
        ref = i.getStringExtra("ref");
        at = i.getLongExtra("at", System.currentTimeMillis());
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private Button button(String text, int bg, int fg, int heightDp) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setTextSize(18);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setTextColor(fg);
        GradientDrawable g = new GradientDrawable();
        g.setColor(bg);
        g.setCornerRadius(dp(16));
        btn.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(heightDp));
        lp.topMargin = dp(12);
        btn.setLayoutParams(lp);
        return btn;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.parseColor("#0D0D0F"));
        root.setPadding(dp(28), dp(48), dp(28), dp(48));

        TextView tag = text("⏰  REMINDER", 13, Color.parseColor("#9AA0A6"), true);
        tag.setLetterSpacing(0.2f);
        root.addView(tag);

        TextView time = text(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(at)), 56, Color.parseColor("#FFB347"), true);
        time.setPadding(0, dp(16), 0, dp(8));
        root.addView(time);

        TextView t = text(title == null ? "Reminder" : title, 28, Color.WHITE, true);
        root.addView(t);

        if (body != null && !body.isEmpty()) {
            TextView bd = text(body, 17, Color.parseColor("#C4C7C5"), false);
            bd.setPadding(0, dp(12), 0, dp(24));
            root.addView(bd);
        }

        Button done = button("plan".equals(kind) ? "▶  Start now" : "✓  Done", Color.parseColor("#2E9E5B"), Color.WHITE, 64);
        done.setOnClickListener((View v) -> {
            ReminderOps.done(this, id, kind, ref);
            finishAndRemoveTask();
        });
        root.addView(done);

        Button snooze = button("💤  Snooze 10 min", Color.parseColor("#26262B"), Color.WHITE, 56);
        snooze.setOnClickListener((View v) -> {
            ReminderOps.snooze(this, id, title, body, kind, ref);
            finishAndRemoveTask();
        });
        root.addView(snooze);

        Button open = button("Open OWLF", Color.TRANSPARENT, Color.parseColor("#9AA0A6"), 48);
        open.setTextSize(15);
        open.setOnClickListener((View v) -> {
            Notifs.nm(this).cancel(ReminderOps.notifId(id));
            Intent i = new Intent(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            finishAndRemoveTask();
        });
        root.addView(open);

        setContentView(root);
    }
}
