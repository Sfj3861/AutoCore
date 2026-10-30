package com.sfj3861.autocore;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())||"android.intent.action.TIME_SET".equals(intent.getAction())||"android.intent.action.TIMEZONE_CHANGED".equals(intent.getAction())){
            ScheduleUtils.scheduleNext(context,"silent");
            ScheduleUtils.scheduleNext(context,"normal");
        }
    }
}