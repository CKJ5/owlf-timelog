package com.ckj5.owlf;

import android.content.Intent;
import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(OwlfPlugin.class);
        super.onCreate(savedInstanceState);
        handle(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handle(intent);
    }

    /** Widget buttons and notification actions arrive here as intent extras. */
    private void handle(Intent i) {
        if (i == null) return;
        String action = i.getStringExtra("owlf_action");
        if (action == null) return;
        try {
            JSONObject o = new JSONObject();
            o.put("action", action);
            o.put("ref", i.getStringExtra("owlf_ref"));
            Prefs.addPending(this, o);
        } catch (JSONException ignored) {
        }
        i.removeExtra("owlf_action");
        i.removeExtra("owlf_ref");
        if (OwlfPlugin.instance != null) OwlfPlugin.instance.pendingChanged();
    }
}
