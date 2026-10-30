package com.ckj5.owlf;

import android.app.AlarmManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.provider.Settings;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@CapacitorPlugin(
        name = "Owlf",
        permissions = {@Permission(alias = "notifications", strings = {"android.permission.POST_NOTIFICATIONS"})}
)
public class OwlfPlugin extends Plugin {
    static OwlfPlugin instance;

    @Override
    public void load() {
        instance = this;
        Notifs.ensureChannels(getContext());
    }

    @Override
    protected void handleOnDestroy() {
        if (instance == this) instance = null;
    }

    void pendingChanged() {
        notifyListeners("pending", new JSObject());
    }

    /** Snapshot for the widget and the pinned notification. */
    @PluginMethod
    public void syncState(PluginCall call) {
        Context c = getContext();
        Prefs.putState(c, call.getData().toString());
        WidgetProvider.refreshAll(c);
        TimerService.apply(c);
        call.resolve();
    }

    /** Replace every scheduled full-screen reminder with this list: [{id,title,body,at,kind,ref}] */
    @PluginMethod
    public void setAlarms(PluginCall call) {
        JSArray arr = call.getArray("alarms");
        Alarms.replaceAll(getContext(), arr == null ? new JSONArray() : arr);
        call.resolve();
    }

    @PluginMethod
    public void notify(PluginCall call) {
        Notifs.postAlert(getContext(), call.getString("title", "OWLF"), call.getString("body", ""), call.getString("tag", "owlf"));
        call.resolve();
    }

    @PluginMethod
    public void getPending(PluginCall call) {
        JSObject r = new JSObject();
        r.put("actions", Prefs.takePending(getContext()));
        call.resolve(r);
    }

    @PluginMethod
    public void getStatus(PluginCall call) {
        Context c = getContext();
        JSObject r = new JSObject();
        r.put("notifications", Notifs.nm(c).areNotificationsEnabled());
        r.put("overlay", Settings.canDrawOverlays(c));
        r.put("fullScreen", Build.VERSION.SDK_INT < 34 || Notifs.nm(c).canUseFullScreenIntent());
        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        r.put("battery", pm.isIgnoringBatteryOptimizations(c.getPackageName()));
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        r.put("exactAlarm", Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms());
        call.resolve(r);
    }

    @PluginMethod
    public void requestNotifications(PluginCall call) {
        if (Build.VERSION.SDK_INT >= 33 && getPermissionState("notifications") != PermissionState.GRANTED) {
            requestPermissionForAlias("notifications", call, "notifPermResult");
        } else {
            JSObject r = new JSObject();
            r.put("granted", Notifs.nm(getContext()).areNotificationsEnabled());
            call.resolve(r);
        }
    }

    @PermissionCallback
    private void notifPermResult(PluginCall call) {
        JSObject r = new JSObject();
        r.put("granted", getPermissionState("notifications") == PermissionState.GRANTED);
        call.resolve(r);
    }

    @PluginMethod
    public void openSettings(PluginCall call) {
        Context c = getContext();
        String kind = call.getString("kind", "notifications");
        String pkg = c.getPackageName();
        Intent i;
        switch (kind) {
            case "overlay":
                i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + pkg));
                break;
            case "fullScreen":
                i = Build.VERSION.SDK_INT >= 34
                        ? new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:" + pkg))
                        : new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pkg);
                break;
            case "battery":
                i = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                break;
            default:
                i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pkg);
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            c.startActivity(i);
            call.resolve();
        } catch (Exception e) {
            call.reject("Cannot open settings: " + e.getMessage());
        }
    }

    @PluginMethod
    public void pinWidget(PluginCall call) {
        Context c = getContext();
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        if (m.isRequestPinAppWidgetSupported()) {
            m.requestPinAppWidget(new ComponentName(c, WidgetProvider.class), null, null);
            call.resolve();
        } else {
            call.reject("This launcher cannot pin widgets. Long-press the home screen → Widgets → OWLF.");
        }
    }

    @PluginMethod
    public void saveFile(PluginCall call) {
        Context c = getContext();
        String name = call.getString("name", "owlf-file.txt");
        String mime = call.getString("mime", "application/octet-stream");
        String text = call.getString("text", "");
        try {
            byte[] data = text.getBytes(StandardCharsets.UTF_8);
            JSObject r = new JSObject();
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues v = new ContentValues();
                v.put(MediaStore.Downloads.DISPLAY_NAME, name);
                v.put(MediaStore.Downloads.MIME_TYPE, mime);
                v.put(MediaStore.Downloads.IS_PENDING, 1);
                Uri u = c.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                if (u == null) throw new Exception("Could not create file");
                try (OutputStream os = c.getContentResolver().openOutputStream(u)) {
                    os.write(data);
                }
                v.clear();
                v.put(MediaStore.Downloads.IS_PENDING, 0);
                c.getContentResolver().update(u, v, null, null);
                r.put("uri", u.toString());
                r.put("where", "Downloads/" + name);
            } else {
                File dir = c.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                File f = new File(dir, name);
                try (FileOutputStream os = new FileOutputStream(f)) {
                    os.write(data);
                }
                r.put("uri", Uri.fromFile(f).toString());
                r.put("where", f.getAbsolutePath());
            }
            call.resolve(r);
        } catch (Exception e) {
            call.reject("Save failed: " + e.getMessage());
        }
    }

    @PluginMethod
    public void openUri(PluginCall call) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.parse(call.getString("uri", "")), call.getString("mime", "*/*"));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            getContext().startActivity(i);
            call.resolve();
        } catch (Exception e) {
            call.reject("No app can open this file: " + e.getMessage());
        }
    }

    @PluginMethod
    public void minimize(PluginCall call) {
        getActivity().moveTaskToBack(true);
        call.resolve();
    }
}
