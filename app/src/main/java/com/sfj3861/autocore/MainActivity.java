package com.sfj3861.autocore;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

public class MainActivity extends Activity {
    private static final int BLUE=0xff3478f6, BG=0xff101217, CARD=0xff1d222c, MUTED=0xffa5aebe;
    private LinearLayout body;
    private int selectedHour=7,selectedMinute=55;
    private TextView soundStatus;

    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);showHome();
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},92);}
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density);}
    private TextView txt(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,1);t.setPadding(0,dp(4),0,dp(4));return t;}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void init(String subtitle){
        LinearLayout root=col();root.setBackgroundColor(BG);
        TextView brand=txt("◈  AutoCore",22,Color.WHITE,true);brand.setPadding(dp(20),dp(20),dp(20),0);root.addView(brand);
        TextView sub=txt(subtitle,13,MUTED,false);sub.setPadding(dp(20),0,dp(20),dp(12));root.addView(sub);
        ScrollView scroll=new ScrollView(this);body=col();body.setPadding(dp(16),dp(8),dp(16),dp(20));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=row();nav.setBackgroundColor(CARD);nav.setPadding(dp(4),dp(5),dp(4),dp(5));
        navButton(nav,"Inicio",this::showHome);navButton(nav,"Crear",this::showCreate);navButton(nav,"Horarios",this::showSchedule);navButton(nav,"Sonido",this::showSound);navButton(nav,"Ajustes",this::showSettings);
        root.addView(nav);setContentView(root);
    }
    private void navButton(LinearLayout nav,String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextSize(10);b.setAllCaps(false);b.setTextColor(Color.WHITE);nav.addView(b,new LinearLayout.LayoutParams(0,dp(46),1));b.setOnClickListener(v->action.run());}
    private void card(String title,String detail){LinearLayout c=col();c.setBackgroundColor(CARD);c.setPadding(dp(15),dp(12),dp(15),dp(12));c.addView(txt(title,16,Color.WHITE,true));if(detail!=null)c.addView(txt(detail,13,MUTED,false));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(10);body.addView(c,p);}
    private Button btn(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(BLUE));b.setOnClickListener(v->action.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(8);body.addView(b,p);return b;}
    private void showHome(){
        init("Panel de control");
        boolean monitoring=SoundMonitorServiceRunning();
        card("Motor de automatización","Horarios semanales y detector de sonido con ejecución local.");
        card("Detector de sonido",monitoring?"Escucha activa · micrófono en uso":"Detenido · no consume recursos de audio");
        card("Programaciones",describeSchedule("silent")+"\n"+describeSchedule("normal"));
        btn("Crear automatización",this::showCreate);btn("Configurar horarios",this::showSchedule);btn("Detector de sonido",this::showSound);
        body.addView(txt("AutoCore 0.2.0 · Automatización local",11,MUTED,false));
    }
    private boolean SoundMonitorServiceRunning(){return getSharedPreferences("autocore",0).getBoolean("monitor_ui",false);}
    private String describeSchedule(String key){var p=getSharedPreferences("autocore",0);if(!p.getBoolean(key+"_enabled",false))return key.equals("silent")?"Silencio: sin programar":"Sonido: sin programar";return (key.equals("silent")?"No molestar: ":"Sonido normal: ")+String.format("%02d:%02d",p.getInt(key+"_hour",7),p.getInt(key+"_minute",55))+" · "+daysText(p.getInt(key+"_days",0));}
    private String daysText(int mask){String[] d={"L","M","X","J","V","S","D"};StringBuilder b=new StringBuilder();for(int i=0;i<7;i++)if((mask&(1<<i))!=0){if(b.length()>0)b.append(" ");b.append(d[i]);}return b.toString();}
    private void showCreate(){init("Crear · Plantillas");card("Sonido → Tecla","Graba una muestra de 3,5 segundos, selecciona una tecla y activa el detector cuando quieras.");btn("Configurar sonido → tecla",this::showSound);card("Hora → modo de sonido","Programa No molestar o sonido normal por días de la semana.");btn("Configurar horario semanal",this::showSchedule);}
    private void showSchedule(){
        init("Programación semanal");
        card("Horarios independientes","Puedes guardar un horario para activar No molestar y otro para desactivarlo. Se repiten cada semana.");
        var prefs=getSharedPreferences("autocore",0);
        addScheduleEditor("Activar No molestar","silent",0);
        addScheduleEditor("Desactivar No molestar","normal",1);
        btn("Permitir alarmas exactas",()->{
            if(Build.VERSION.SDK_INT>=31){Intent i=new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName()));startActivity(i);}
            else Toast.makeText(this,"Las alarmas exactas no requieren este permiso en esta versión.",Toast.LENGTH_SHORT).show();
        });
        btn("Permiso de No molestar",()->startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)));
        card("Importante","El modo No molestar necesita acceso especial. Si no concedes alarmas exactas, Android puede retrasar la ejecución para ahorrar batería.");
    }
    private void addScheduleEditor(String title,String key,int defaultAction){
        var p=getSharedPreferences("autocore",0);
        LinearLayout c=col();c.setBackgroundColor(CARD);c.setPadding(dp(14),dp(12),dp(14),dp(12));
        c.addView(txt(title,16,Color.WHITE,true));
        CheckBox enabled=new CheckBox(this);enabled.setText("Activar esta programación");enabled.setTextColor(Color.WHITE);enabled.setChecked(p.getBoolean(key+"_enabled",false));c.addView(enabled);
        int hour=p.getInt(key+"_hour",defaultAction==0?7:14),minute=p.getInt(key+"_minute",defaultAction==0?55:5);
        TextView time=txt(String.format("Hora: %02d:%02d",hour,minute),18,Color.WHITE,true);c.addView(time);
        Button pick=new Button(this);pick.setText("Elegir hora");pick.setAllCaps(false);pick.setOnClickListener(v->new TimePickerDialog(this,(tp,h,m)->{time.setText(String.format("Hora: %02d:%02d",h,m));time.setTag(new int[]{h,m});},hour,minute,true).show());c.addView(pick);
        c.addView(txt("Días de ejecución",13,MUTED,true));
        String[] days={"Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo"};CheckBox[] checks=new CheckBox[7];int mask=p.getInt(key+"_days",31);
        for(int i=0;i<7;i++){checks[i]=new CheckBox(this);checks[i].setText(days[i]);checks[i].setTextColor(Color.WHITE);checks[i].setChecked((mask&(1<<i))!=0);c.addView(checks[i]);}
        Button save=new Button(this);save.setText("Guardar "+(key.equals("silent")?"silencio":"sonido"));save.setAllCaps(false);save.setOnClickListener(v->{
            int bits=0;for(int i=0;i<7;i++)if(checks[i].isChecked())bits|=1<<i;
            if(enabled.isChecked()&&bits==0){Toast.makeText(this,"Selecciona al menos un día.",Toast.LENGTH_SHORT).show();return;}
            int[] hm=(int[])time.getTag();if(hm==null)hm=new int[]{hour,minute};
            p.edit().putBoolean(key+"_enabled",enabled.isChecked()).putInt(key+"_hour",hm[0]).putInt(key+"_minute",hm[1]).putInt(key+"_days",bits).apply();
            ScheduleUtils.scheduleNext(this,key);Toast.makeText(this,"Programación guardada.",Toast.LENGTH_SHORT).show();
        });c.addView(save);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(12);body.addView(c,lp);
    }
    private void showSound(){
        init("Detector de sonido");
        var p=getSharedPreferences("autocore",0);
        boolean has=p.contains("fingerprint");
        card("Reconocimiento por muestra",has?"Hay una muestra guardada. El detector compara el perfil acústico con el sonido del micrófono.":"Graba durante 3,5 segundos el sonido que quieres reconocer. Se guarda un perfil acústico local, no el audio continuo.");
        soundStatus=txt(has?"Muestra preparada":"Todavía no hay muestra",13,MUTED,false);body.addView(soundStatus);
        btn("Grabar / reemplazar muestra (3,5 s)",this::recordFingerprint);
        EditText key=new EditText(this);key.setSingleLine(true);key.setText(p.getString("sound_key","[SPACE]"));key.setTextColor(Color.WHITE);key.setHintTextColor(MUTED);key.setHint("Ejemplo: [SPACE], [ENTER], [TAB]");body.addView(txt("Acción de teclado",14,Color.WHITE,true));body.addView(key);
        btn("Abrir teclado para elegir / escribir",()->{key.requestFocus();((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(key,InputMethodManager.SHOW_IMPLICIT);});
        btn("Elegir AutoCore Keyboard",()->{((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker();});
        CheckBox monitor=new CheckBox(this);monitor.setText("Activar reconocimiento continuo");monitor.setTextColor(Color.WHITE);monitor.setChecked(SoundMonitorServiceRunning());body.addView(monitor);
        btn("Guardar configuración",()->{
            String value=key.getText().toString().trim();if(value.isEmpty()){Toast.makeText(this,"Escribe una tecla o comando.",Toast.LENGTH_SHORT).show();return;}
            p.edit().putString("sound_key",value).apply();
            if(monitor.isChecked())startMonitor();else stopMonitor();
            Toast.makeText(this,"Configuración guardada.",Toast.LENGTH_SHORT).show();
        });
        btn("Detener detector ahora",this::stopMonitor);
        card("Compatibilidad de teclas","AutoCore Keyboard puede enviar SPACE, ENTER, TAB, borrar y texto a un campo de texto enfocado. Android no permite inyectar arbitrariamente teclas físicas en cualquier juego o aplicación; para esos casos la acción puede no estar disponible.");
        card("Privacidad y RAM","El micrófono solo se mantiene activo mientras el detector esté habilitado. Al detenerlo se libera AudioRecord. La muestra se almacena como un pequeño perfil numérico.");
    }
    private void recordFingerprint(){
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},90);return;}
        soundStatus.setText("Grabando muestra… mantén el sonido cerca del micrófono.");
        new Thread(()->{try{float[] fp=SoundFingerprint.recordSample();getSharedPreferences("autocore",0).edit().putString("fingerprint",SoundFingerprint.encode(fp)).apply();runOnUiThread(()->soundStatus.setText("Muestra guardada. Ya puedes activar el detector."));}
            catch(Exception e){runOnUiThread(()->soundStatus.setText("Error al grabar: "+e.getMessage()));}},"AutoCore-Sample").start();
    }
    private void startMonitor(){
        if(!getSharedPreferences("autocore",0).contains("fingerprint")){Toast.makeText(this,"Primero graba una muestra.",Toast.LENGTH_LONG).show();return;}
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},91);return;}
        Intent i=new Intent(this,SoundMonitorService.class);
        try{if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);getSharedPreferences("autocore",0).edit().putBoolean("monitor_ui",true).apply();}
        catch(Exception e){Toast.makeText(this,"Android no permitió iniciar el micrófono: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private void stopMonitor(){
        stopService(new Intent(this,SoundMonitorService.class));getSharedPreferences("autocore",0).edit().putBoolean("monitor_ui",false).apply();Toast.makeText(this,"Detector detenido; micrófono liberado.",Toast.LENGTH_SHORT).show();
    }
    private void showSettings(){
        init("Configuración");
        btn("Permiso No molestar",()->startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)));
        btn("Permisos de AutoCore",()->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))));
        btn("Configurar teclado AutoCore",()->startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        btn("Mostrar selector de teclado",()->((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker());
        card("Versión","AutoCore 0.2.0 · horarios semanales, reconocimiento acústico por muestra y teclado IME.");
    }
    @Override protected void onResume(){super.onResume();if(body!=null&&getIntent().getBooleanExtra("refresh",false))showHome();}
}