package com.sfj3861.autocore;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.content.Intent;
import android.media.*;
import android.os.*;
import android.speech.*;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class SoundMonitorService extends Service {
    private static final String CHANNEL="autocore_audio";
    private final AtomicBoolean running=new AtomicBoolean(false);
    private Thread worker;
    private SpeechRecognizer speech;
    private Handler main;
    private long lastTrigger=0;
    @Override public void onCreate(){super.onCreate();main=new Handler(Looper.getMainLooper());NotificationChannel c=new NotificationChannel(CHANNEL,"AutoCore · detector",NotificationManager.IMPORTANCE_LOW);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent!=null&&"STOP".equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
        Notification n=new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_autocore).setContentTitle("AutoCore está activo").setContentText("Detector en funcionamiento. Abre AutoCore para detenerlo.").setOngoing(true).build();
        if(Build.VERSION.SDK_INT>=29)startForeground(701,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);else startForeground(701,n);
        if(running.compareAndSet(false,true)){
            int mode=getSharedPreferences("autocore",0).getInt("trigger_mode",0);
            if(mode==1)startSpeechRecognition();else{worker=new Thread(this::listenForSound,"AutoCore-Audio");worker.start();}
        }
        return START_STICKY;
    }
    private void listenForSound(){
        int rate=16000, frame=160, min=AudioRecord.getMinBufferSize(rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord recorder=new AudioRecord(MediaRecorder.AudioSource.MIC,rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(min,frame*8));
        try{
            if(recorder.getState()!=AudioRecord.STATE_INITIALIZED)throw new IllegalStateException("No se pudo inicializar el micrófono");
            short[] data=new short[frame];recorder.startRecording();int activeMs=0;
            while(running.get()){
                int count=recorder.read(data,0,frame);
                if(count==frame){
                    double sum=0;for(short s:data)sum+=(double)s*s;double rms=Math.sqrt(sum/count);
                    int sensitivity=getSharedPreferences("autocore",0).getInt("sensitivity",55);
                    double threshold=900+(100-sensitivity)*75.0;
                    if(rms>threshold)activeMs+=10;else activeMs=0;
                    int needed=getSharedPreferences("autocore",0).getInt("min_sound_ms",30);
                    if(activeMs>=needed){activeMs=0;triggerAction("Sonido detectado");}
                }
            }
        }catch(Exception e){notifyUser("Detector detenido",e.getMessage()==null?"Error de audio":e.getMessage());}
        finally{try{recorder.stop();}catch(Exception ignored){}recorder.release();finishService();}
    }
    private void startSpeechRecognition(){
        main.post(()->{
            if(!running.get())return;
            if(!SpeechRecognizer.isRecognitionAvailable(this)){notifyUser("Voz no disponible","No hay un reconocedor de voz en este dispositivo.");stopSelf();return;}
            try{
                speech=SpeechRecognizer.createSpeechRecognizer(this);
                speech.setRecognitionListener(new RecognitionListener(){
                    public void onReadyForSpeech(android.os.Bundle p){}
                    public void onBeginningOfSpeech(){}
                    public void onRmsChanged(float r){}
                    public void onBufferReceived(byte[] b){}
                    public void onEndOfSpeech(){}
                    public void onError(int e){restartSpeech();}
                    public void onResults(android.os.Bundle b){checkSpeech(b);restartSpeech();}
                    public void onPartialResults(android.os.Bundle b){checkSpeech(b);}
                    public void onEvent(int t,android.os.Bundle p){}
                });
                listenSpeech();
            }catch(Exception e){notifyUser("Error de reconocimiento",e.getMessage()==null?"No se pudo iniciar el reconocedor.":e.getMessage());stopSelf();}
        });
    }
    private void listenSpeech(){if(!running.get()||speech==null)return;Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault().toLanguageTag());i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);try{speech.startListening(i);}catch(Exception e){restartSpeech();}}
    private void checkSpeech(android.os.Bundle b){
        if(b==null)return;String phrase=getSharedPreferences("autocore",0).getString("trigger_phrase","").trim().toLowerCase(Locale.ROOT);if(phrase.isEmpty())return;
        java.util.ArrayList<String> results=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(results==null)return;
        for(String heard:results){if(heard!=null&&heard.toLowerCase(Locale.ROOT).contains(phrase)){triggerAction("Palabra reconocida: "+phrase);break;}}
    }
    private void restartSpeech(){if(!running.get())return;main.postDelayed(()->{if(running.get()&&speech!=null){try{speech.cancel();}catch(Exception ignored){}listenSpeech();}},350);}
    private void triggerAction(String reason){
        long now=System.currentTimeMillis();if(now-lastTrigger<500)return;lastTrigger=now;
        boolean gesture=getSharedPreferences("autocore",0).getBoolean("gesture_enabled",true);
        boolean sent=gesture?AutoCoreAccessibilityService.tapConfigured(this):AutoCoreIme.sendConfiguredKey(this);
        notifyUser(sent?"Acción ejecutada":"Detección realizada",sent?reason+(gesture?" · toque enviado":" · tecla enviada"):"Activa AutoCore en Accesibilidad o configura el teclado.");
    }
    private void notifyUser(String title,String message){
        Notification n=new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_autocore).setContentTitle(title).setContentText(message).setAutoCancel(true).build();
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify((int)(System.currentTimeMillis()%100000),n);
    }
    private void finishService(){running.set(false);getSharedPreferences("autocore",0).edit().putBoolean("monitor_ui",false).apply();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}
    @Override public void onDestroy(){running.set(false);if(worker!=null)worker.interrupt();if(speech!=null){speech.cancel();speech.destroy();speech=null;}getSharedPreferences("autocore",0).edit().putBoolean("monitor_ui",false).apply();super.onDestroy();}
    @Override public android.os.IBinder onBind(Intent intent){return null;}
}
