package com.sfj3861.autocore;

import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SoundMonitorService extends Service {
    private static final String CHANNEL="autocore_audio";
    private final AtomicBoolean running=new AtomicBoolean(false);
    private Thread worker;
    @Override public void onCreate(){super.onCreate();NotificationChannel c=new NotificationChannel(CHANNEL,"AutoCore audio monitor",NotificationManager.IMPORTANCE_LOW);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null&&"STOP".equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
        Notification n=new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("AutoCore está escuchando").setContentText("Detector de sonido activo. Toca para detenerlo desde AutoCore.").setOngoing(true).build();
        startForeground(701,n);
        if(running.compareAndSet(false,true)){worker=new Thread(this::listen,"AutoCore-Audio");worker.start();}
        return START_STICKY;
    }
    private void listen(){
        float[] target=SoundFingerprint.decode(getSharedPreferences("autocore",0).getString("fingerprint",""));
        if(target==null){running.set(false);getSharedPreferences("autocore",0).edit().putBoolean("monitor_ui",false).apply();stopSelf();return;}
        int rate=16000, frame=1024, min=AudioRecord.getMinBufferSize(rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord recorder=new AudioRecord(MediaRecorder.AudioSource.MIC,rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(min,frame*4));
        try{
            if(recorder.getState()!=AudioRecord.STATE_INITIALIZED)return;
            short[] data=new short[frame];recorder.startRecording();long last=0;
            while(running.get()){
                int count=recorder.read(data,0,frame);
                if(count==frame){
                    float[] current=SoundFingerprint.feature(data);
                    float score=SoundFingerprint.similarity(target,current);
                    if(score>=getSharedPreferences("autocore",0).getFloat("match_threshold",0.91f)&&System.currentTimeMillis()-last>1800){
                        last=System.currentTimeMillis();
                        boolean sent=AutoCoreIme.sendConfiguredKey(this);
                        if(!sent)showNotice("Sonido reconocido","Activa AutoCore Keyboard y enfoca un campo de texto para enviar la tecla configurada.");
                        else showNotice("Automatización ejecutada","Sonido reconocido: tecla "+getSharedPreferences("autocore",0).getString("sound_key","[SPACE]"));
                    }
                }
            }
        }catch(Exception e){showNotice("Detector detenido",e.getMessage()==null?"Error de audio":e.getMessage());}
        finally{try{recorder.stop();}catch(Exception ignored){}recorder.release();running.set(false);getSharedPreferences("autocore",0).edit().putBoolean("monitor_ui",false).apply();stopForeground(STOP_FOREGROUND_REMOVE);}
    }
    private void showNotice(String title,String message){
        Notification n=new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(message).setAutoCancel(true).build();
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify((int)(System.currentTimeMillis()%100000),n);
    }
    @Override public void onDestroy(){running.set(false);getSharedPreferences("autocore",0).edit().putBoolean("monitor_ui",false).apply();if(worker!=null)worker.interrupt();super.onDestroy();}
    @Override public android.os.IBinder onBind(Intent intent){return null;}
}