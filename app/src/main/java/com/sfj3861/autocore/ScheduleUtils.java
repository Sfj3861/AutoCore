package com.sfj3861.autocore;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;

public final class ScheduleUtils {
    private ScheduleUtils(){}
    public static void scheduleNext(Context c,String action){
        var p=c.getSharedPreferences("autocore",Context.MODE_PRIVATE);
        Intent i=new Intent(c,ScheduleReceiver.class);i.putExtra("action",action);
        int id=action.equals("silent")?501:502;
        PendingIntent pi=PendingIntent.getBroadcast(c,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(am==null)return;
        if(!p.getBoolean(action+"_enabled",false)){am.cancel(pi);return;}
        int hour=p.getInt(action+"_hour",7),minute=p.getInt(action+"_minute",55),mask=p.getInt(action+"_days",0);
        Calendar now=Calendar.getInstance(),next=null;
        for(int offset=0;offset<8;offset++){
            Calendar candidate=(Calendar)now.clone();candidate.add(Calendar.DAY_OF_YEAR,offset);
            candidate.set(Calendar.HOUR_OF_DAY,hour);candidate.set(Calendar.MINUTE,minute);candidate.set(Calendar.SECOND,0);candidate.set(Calendar.MILLISECOND,0);
            int day=(candidate.get(Calendar.DAY_OF_WEEK)+5)%7;
            if((mask&(1<<day))!=0&&candidate.getTimeInMillis()>now.getTimeInMillis()){next=candidate;break;}
        }
        if(next==null)return;
        long when=next.getTimeInMillis();
        try{
            if(Build.VERSION.SDK_INT>=31&&am.canScheduleExactAlarms())am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);
            else if(Build.VERSION.SDK_INT>=23)am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);
            else am.setExact(AlarmManager.RTC_WAKEUP,when,pi);
        }catch(SecurityException e){am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);}
    }
}