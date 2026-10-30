package com.sfj3861.autocore;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class ScheduleReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        String action=intent.getStringExtra("action");
        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm!=null && nm.isNotificationPolicyAccessGranted()){
            nm.setInterruptionFilter("silent".equals(action)?NotificationManager.INTERRUPTION_FILTER_NONE:NotificationManager.INTERRUPTION_FILTER_ALL);
        }
        ScheduleUtils.scheduleNext(context,action);
    }
}
