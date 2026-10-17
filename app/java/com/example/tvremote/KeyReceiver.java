package com.example.tvremote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/** Handles the Up/Down/Left/Right/OK/Back buttons of the notification. */
public final class KeyReceiver extends BroadcastReceiver {
    public static final String ACTION = "com.remote13.mirroring.KEY";
    @Override public void onReceive(Context c, Intent i) {
        int code = i.getIntExtra("code", 0);
        if (code == 0) return;
        TvBridge b = TvBridge.getInstance();
        if (b == null) { Toast.makeText(c, "Open the Remote app first", Toast.LENGTH_SHORT).show(); return; }
        b.key(code, 3);
    }
}
