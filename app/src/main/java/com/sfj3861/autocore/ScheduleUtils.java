package com.sfj3861.autocore;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;

public final class ScheduleUtils {
    private ScheduleUtils(){}
    public static void scheduleNext(Context c,String action){
        var p=c.getSharedPreferences("autocore",0);
        if(!p.getBoolean(action+"_enabled",false))return;
        int hour=p.getInt(action+"_hour",7), minute=p.getInt(action+"_minute",55), mask=p.getInt(action+"_days",0);
        Calendar now=Calendar.getInstance(), next=Calendar.getInstance(); next.set(Calendar.SECOND,0);next.set(Calendar.MILLISECOND,0);
        boolean found=false;
        for(int offset=0;offset<8;offset++){
            next=(Calendar)now.clone();next.add(Calendar.DAY_OF_YEAR,offset);next.set(Calendar.HOUR_OF_DAY,hour);next.set(Calendar.MINUTE,minute);next.set(Calendar.SECOND,0);next.set(Calendar.MILLISECOND,0);
            int day=(next.get(Calendar.DAY_OF_WEEK)+5)%7;
            if((mask&(1<<day))!=0 && next.getTimeInMillis()>now.getTimeInMillis()){found=true;break;}
        }
        if(!found)return;
        int id=action.equals("silent")?501:502;
        Intent i=new Intent(c,ScheduleReceiver.class);i.putExtra("action",action);
        PendingIntent pi=PendingIntent.getBroadcast(c,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(am!=null)am.setWindow(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),10*60*1000,pi);
    }
}
