package com.example.tvremote;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

/** Persistent notification with Up / Down / Left / Right / OK / Back buttons. */
public final class NotifControl {
    private static final int NID = 1313;
    private static final String CH = "remote_keys";
    private static final String[] IDS = {"nb_left", "nb_up", "nb_down", "nb_right", "nb_ok", "nb_back"};
    private static final int[] CODES = {21, 19, 20, 22, 23, 4};

    public static boolean show(Context c) {
        try {
            String pkg = c.getPackageName();
            int layout = c.getResources().getIdentifier("nb_remote", "layout", pkg);
            if (layout == 0) return false;
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel ch = new NotificationChannel(CH, "Remote buttons", NotificationManager.IMPORTANCE_LOW);
                ch.setShowBadge(false);
                nm.createNotificationChannel(ch);
            }
            RemoteViews rv = new RemoteViews(pkg, layout);
            int fl = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
            for (int i = 0; i < IDS.length; i++) {
                int id = c.getResources().getIdentifier(IDS[i], "id", pkg);
                if (id == 0) continue;
                Intent in = new Intent(KeyReceiver.ACTION).setComponent(new ComponentName(c, KeyReceiver.class)).putExtra("code", CODES[i]);
                rv.setOnClickPendingIntent(id, PendingIntent.getBroadcast(c, 100 + i, in, fl));
            }
            Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CH) : new Notification.Builder(c);
            b.setSmallIcon(c.getApplicationInfo().icon).setOngoing(true).setShowWhen(false).setOnlyAlertOnce(true);
            if (Build.VERSION.SDK_INT >= 24) b.setStyle(new Notification.DecoratedCustomViewStyle()).setCustomContentView(rv);
            else b.setContent(rv);
            if (Build.VERSION.SDK_INT < 26) b.setPriority(Notification.PRIORITY_LOW);
            nm.notify(NID, b.build());
            return true;
        } catch (Throwable t) { return false; }
    }

    public static void hide(Context c) {
        try { ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NID); } catch (Throwable ignored) {}
    }
}
