package com.ckj5.owlf;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Small JSON-in-SharedPreferences store shared by the plugin, widget, service and receivers. */
final class Prefs {
    private static final String FILE = "owlf_native";
    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static void putState(Context c, String json) {
        sp(c).edit().putString("state", json).apply();
    }

    static JSONObject state(Context c) {
        try {
            String s = sp(c).getString("state", null);
            return s == null ? new JSONObject() : new JSONObject(s);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    static void putAlarms(Context c, JSONArray arr) {
        sp(c).edit().putString("alarms", arr.toString()).apply();
    }

    static JSONArray alarms(Context c) {
        try {
            String s = sp(c).getString("alarms", null);
            return s == null ? new JSONArray() : new JSONArray(s);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    /** Last few native events (scheduled / fired / errors), shown by the app's "Diagnose" button. */
    static synchronized void log(Context c, String msg) {
        try {
            JSONArray arr = new JSONArray(sp(c).getString("log", "[]"));
            JSONArray out = new JSONArray();
            for (int i = Math.max(0, arr.length() - 11); i < arr.length(); i++) out.put(arr.get(i));
            out.put(new java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.US).format(new java.util.Date()) + " " + msg);
            sp(c).edit().putString("log", out.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    static synchronized JSONArray logLines(Context c) {
        try {
            return new JSONArray(sp(c).getString("log", "[]"));
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    /** Actions the user took outside the WebView (widget, notification, alarm screen). JS applies them. */
    static synchronized void addPending(Context c, JSONObject action) {
        try {
            JSONArray arr = new JSONArray(sp(c).getString("pending", "[]"));
            arr.put(action);
            sp(c).edit().putString("pending", arr.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    static synchronized JSONArray takePending(Context c) {
        try {
            JSONArray arr = new JSONArray(sp(c).getString("pending", "[]"));
            sp(c).edit().putString("pending", "[]").apply();
            return arr;
        } catch (JSONException e) {
            return new JSONArray();
        }
    }
}
