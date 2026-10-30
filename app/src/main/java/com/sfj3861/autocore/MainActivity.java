package com.sfj3861.autocore;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;

public class MainActivity extends Activity {
    private static final int BLUE=Color.rgb(53,120,246), BG=Color.rgb(16,18,23), CARD=Color.rgb(29,34,44), MUTED=Color.rgb(165,174,190);
    private LinearLayout root, body;
    private MediaRecorder recorder;
    private File sampleFile;
    private TextView soundStatus;
    private int selectedHour=7, selectedMinute=55, selectedDays=0;
    private String selectedAction="silent";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        showHome();
    }

    private TextView text(String value,int size,int color,boolean bold) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if(bold)t.setTypeface(null,1); t.setPadding(0,dp(4),0,dp(4)); return t;
    }
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void init(String title) {
        root=column(); root.setBackgroundColor(BG);
        TextView brand=text("◈  AutoCore",22,Color.WHITE,true); brand.setPadding(dp(20),dp(20),dp(20),dp(2)); root.addView(brand);
        TextView sub=text(title,13,MUTED,false); sub.setPadding(dp(20),0,dp(20),dp(14)); root.addView(sub);
        ScrollView scroll=new ScrollView(this); body=column(); body.setPadding(dp(16),dp(8),dp(16),dp(20)); scroll.addView(body);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=row(); nav.setPadding(dp(6),dp(8),dp(6),dp(8)); nav.setBackgroundColor(CARD);
        addNav(nav,"Inicio",()->showHome()); addNav(nav,"Crear",()->showCreate()); addNav(nav,"Horarios",()->showSchedule()); addNav(nav,"Sonido",()->showSound()); addNav(nav,"Ajustes",()->showSettings());
        root.addView(nav); setContentView(root);
    }
    private void addNav(LinearLayout nav,String label,Runnable run){
        Button b=new Button(this); b.setText(label); b.setTextSize(10); b.setTextColor(Color.WHITE); b.setAllCaps(false);
        nav.addView(b,new LinearLayout.LayoutParams(0,dp(48),1)); b.setOnClickListener(v->run.run());
    }
    private void card(String title,String detail,String icon){
        LinearLayout c=column(); c.setBackgroundColor(CARD); c.setPadding(dp(16),dp(12),dp(16),dp(12));
        TextView h=text(icon+"  "+title,16,Color.WHITE,true); c.addView(h);
        if(detail!=null)c.addView(text(detail,13,MUTED,false));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(10);body.addView(c,p);
    }
    private Button button(String label,Runnable action){
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(BLUE));
        b.setOnClickListener(v->action.run());return b;
    }
    private void showHome(){
        init("Panel de control");
        card("Motor de automatización","Listo para ejecutar tus reglas locales.","●");
        card("Tus automatizaciones","Los horarios se guardan en el dispositivo. El motor de sonido está disponible para grabar muestras.","⚡");
        body.addView(button("＋  Crear automatización",()->showCreate()));
        body.addView(button("◷  Configurar horario semanal",()->showSchedule()));
        body.addView(button("♫  Configurar sonido → tecla",()->showSound()));
        body.addView(text("Versión 0.1.0 · Compilación inicial",11,MUTED,false));
    }
    private void showCreate(){
        init("Crear automatización · Plantillas");
        card("Sonido → Tecla","Graba una muestra de audio y configura una tecla de destino. La ejecución automática sonido-tecla se incorporará en una siguiente fase.","♫");
        body.addView(button("Configurar muestra de sonido",()->showSound()));
        card("Horario → Acción","Programa silencio o sonido normal para días concretos de la semana.","◷");
        body.addView(button("Crear horario",()->showSchedule()));
        card("Más plantillas","Batería, conectividad, apertura de aplicaciones, notificaciones y sensores se incorporarán progresivamente.","▦");
    }
    private void showSchedule(){
        init("Programación semanal");
        card("Horario → Acción","Crea una regla semanal. La precisión depende de las restricciones de ahorro de batería de Android.","◷");
        Spinner action=new Spinner(this);
        String[] actions={"Silencio (No molestar)","Sonido normal"};
        action.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,actions));
        body.addView(text("Acción",14,Color.WHITE,true));body.addView(action);
        TextView time=text("Hora seleccionada: 07:55",18,Color.WHITE,true);body.addView(time);
        body.addView(button("Elegir hora",()->{
            new TimePickerDialog(this,(v,h,m)->{selectedHour=h;selectedMinute=m;time.setText(String.format("Hora seleccionada: %02d:%02d",h,m));},selectedHour,selectedMinute,true).show();
        }));
        body.addView(text("Días de la semana",14,Color.WHITE,true));
        String[] days={"Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo"};
        CheckBox[] checks=new CheckBox[7];
        for(int i=0;i<7;i++){ final int n=i; checks[i]=new CheckBox(this);checks[i].setText(days[i]);checks[i].setTextColor(Color.WHITE);checks[i].setButtonTintList(android.content.res.ColorStateList.valueOf(BLUE));body.addView(checks[i]); }
        body.addView(button("Guardar programación",()->{
            int mask=0;for(int i=0;i<7;i++)if(checks[i].isChecked())mask|=(1<<i);
            if(mask==0){Toast.makeText(this,"Selecciona al menos un día.",Toast.LENGTH_SHORT).show();return;}
            String key=action.getSelectedItemPosition()==0?"silent":"normal";
            getSharedPreferences("autocore",0).edit().putInt(key+"_hour",selectedHour).putInt(key+"_minute",selectedMinute).putInt(key+"_days",mask).putBoolean(key+"_enabled",true).apply();
            ScheduleUtils.scheduleNext(this,key);
            Toast.makeText(this,"Horario guardado. Requiere permiso No molestar para cambiar el modo de sonido.",Toast.LENGTH_LONG).show();
        }));
        body.addView(button("Conceder permiso No molestar",()->{
            Intent i=new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);startActivity(i);
        }));
        card("Nota sobre Android","Para cambiar el modo silencio/sonido, concede acceso a No molestar. Los horarios pueden retrasarse por las políticas de batería del sistema.","ⓘ");
    }
    private void showSound(){
        init("Detector de sonido");
        card("Grabar muestra","Graba una muestra breve para usarla como referencia. El audio se guarda localmente en el almacenamiento privado de AutoCore.","♫");
        soundStatus=text("No hay ninguna muestra grabada.",13,MUTED,false);body.addView(soundStatus);
        body.addView(button("●  Grabar muestra (máx. 5 s)",()->startRecording()));
        body.addView(button("■  Detener y guardar",()->stopRecording()));
        EditText key=new EditText(this);key.setSingleLine(true);key.setText("[SPACE]");key.setHint("Ejemplo: [SPACE] o [ENTER]");key.setTextColor(Color.WHITE);key.setHintTextColor(MUTED);key.setBackgroundTintList(android.content.res.ColorStateList.valueOf(BLUE));
        body.addView(text("Tecla de destino",14,Color.WHITE,true));body.addView(key);
        body.addView(button("Abrir teclado del dispositivo",()->{key.requestFocus();((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(key,InputMethodManager.SHOW_IMPLICIT);}));
        body.addView(button("Guardar configuración",()->{
            getSharedPreferences("autocore",0).edit().putString("sound_key",key.getText().toString()).apply();
            Toast.makeText(this,"Tecla guardada. La detección continua y el envío de teclas se habilitarán en una versión posterior.",Toast.LENGTH_LONG).show();
        }));
        card("Uso de recursos","La escucha continua no se inicia en esta versión. Así evitamos mantener el micrófono y el procesamiento activos innecesariamente.","◉");
    }
    private void startRecording(){
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},40);return;}
        try{
            if(recorder!=null)recorder.release();
            sampleFile=new File(getFilesDir(),"sound_sample.m4a");
            recorder=new MediaRecorder(this);recorder.setAudioSource(MediaRecorder.AudioSource.MIC);recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);recorder.setMaxDuration(5000);recorder.setOutputFile(sampleFile.getAbsolutePath());recorder.setOnInfoListener((mr,what,extra)->stopRecording());recorder.prepare();recorder.start();
            soundStatus.setText("Grabando muestra… se detendrá automáticamente a los 5 segundos.");
        }catch(Exception e){soundStatus.setText("No se pudo iniciar la grabación: "+e.getMessage());}
    }
    private void stopRecording(){
        if(recorder==null)return;
        try{recorder.stop();soundStatus.setText("Muestra guardada localmente ("+sampleFile.length()/1024+" KB).");}
        catch(Exception e){soundStatus.setText("La muestra no pudo guardarse. Inténtalo de nuevo.");}
        finally{recorder.release();recorder=null;}
    }
    private void showSettings(){
        init("Configuración");
        card("Permisos","Micrófono: solo al grabar muestras. No molestar: necesario para cambiar silencio/sonido.","⚙");
        body.addView(button("Abrir permisos No molestar",()->startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))));
        body.addView(button("Permisos de la aplicación",()->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:"+getPackageName())))));
        card("Información","AutoCore 0.1.0 · Android nativo · Las funciones pendientes se indican en cada pantalla.","ⓘ");
    }
    @Override protected void onDestroy(){if(recorder!=null){try{recorder.stop();}catch(Exception ignored){}recorder.release();recorder=null;}super.onDestroy();}
}
