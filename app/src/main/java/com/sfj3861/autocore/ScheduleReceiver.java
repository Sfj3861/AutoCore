package com.sfj3861.autocore;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.media.AudioManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ScheduleReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String action = intent.getStringExtra("action");
        if (action == null) return;
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            if (nm.isNotificationPolicyAccessGranted()) {
                nm.setInterruptionFilter("silent".equals(action)
                        ? NotificationManager.INTERRUPTION_FILTER_NONE
                        : NotificationManager.INTERRUPTION_FILTER_ALL);
                AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (audio != null) audio.setRingerMode("silent".equals(action)
                        ? AudioManager.RINGER_MODE_SILENT : AudioManager.RINGER_MODE_NORMAL);
            } else {
                String channel = "autocore_schedule";
                if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(
                        new NotificationChannel(channel, "AutoCore schedules", NotificationManager.IMPORTANCE_DEFAULT));
                Notification n = new Notification.Builder(context, channel)
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setContentTitle("AutoCore: falta un permiso")
                        .setContentText("Concede acceso a No molestar para ejecutar el cambio de sonido programado.")
                        .setAutoCancel(true).build();
                nm.notify(action.equals("silent") ? 801 : 802, n);
            }
        }
        ScheduleUtils.scheduleNext(context, action);
    }
}