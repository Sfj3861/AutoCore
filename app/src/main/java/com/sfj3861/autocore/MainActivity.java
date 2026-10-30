package com.sfj3861.autocore;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.speech.SpeechRecognizer;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

public class MainActivity extends Activity {
    private static final int BLUE=0xff3478f6, BG=0xff10141c, CARD=0xff1c2430, MUTED=0xffaab6c8, TEAL=0xff43d9c5;
    private LinearLayout body;
    private TextView soundStatus;
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density);}
    private TextView txt(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,1);t.setPadding(0,dp(4),0,dp(4));return t;}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);showHome();if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},92);}
    private void init(String subtitle){
        LinearLayout root=col();root.setBackgroundColor(BG);
        LinearLayout header=row();header.setPadding(dp(18),dp(16),dp(18),dp(8));
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.ic_autocore);header.addView(logo,new LinearLayout.LayoutParams(dp(42),dp(42)));
        LinearLayout brand=col();TextView name=txt("AutoCore",21,Color.WHITE,true);name.setPadding(dp(10),0,0,0);brand.addView(name);TextView sub=txt(subtitle,12,MUTED,false);sub.setPadding(dp(10),0,0,0);brand.addView(sub);header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        TextView settings=txt("⚙",25,Color.WHITE,true);settings.setGravity(Gravity.CENTER);settings.setOnClickListener(v->showSettings());header.addView(settings,new LinearLayout.LayoutParams(dp(42),dp(42)));root.addView(header);
        ScrollView scroll=new ScrollView(this);body=col();body.setPadding(dp(16),dp(8),dp(16),dp(18));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=row();nav.setPadding(dp(5),dp(5),dp(5),dp(5));nav.setBackgroundColor(CARD);
        navButton(nav,"⌂\nInicio",this::showHome);navButton(nav,"◉\nSonido",this::showSound);navButton(nav,"◷\nHorarios",this::showSchedule);navButton(nav,"✦\nAsistente",this::showAssistant);
        root.addView(nav);setContentView(root);
    }
    private void navButton(LinearLayout nav,String label,Runnable action){TextView b=txt(label,11,Color.WHITE,true);b.setGravity(Gravity.CENTER);nav.addView(b,new LinearLayout.LayoutParams(0,dp(52),1));b.setOnClickListener(v->action.run());}
    private void card(String title,String detail){LinearLayout c=col();c.setBackgroundColor(CARD);c.setPadding(dp(15),dp(12),dp(15),dp(12));c.addView(txt(title,16,Color.WHITE,true));if(detail!=null)c.addView(txt(detail,13,MUTED,false));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(10);body.addView(c,p);}
    private Button btn(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(BLUE));b.setOnClickListener(v->action.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(8);body.addView(b,p);return b;}
    private boolean monitoring(){return getSharedPreferences("autocore",0).getBoolean("monitor_ui",false);}
    private void showHome(){
        init("Automatización accesible");
        card("Hola. Esto es AutoCore","Crea una regla sencilla: detecta algo → ejecuta una acción. Funciona localmente y tú decides qué permisos conceder.");
        card("Estado del detector",monitoring()?"● Activo · micrófono en uso":"○ Detenido · micrófono liberado");
        card("Detección disponible","Palabra o frase hablada · cualquier sonido · sonido muy breve (desde 10 ms, según el dispositivo).");
        btn("Configurar detección y acción",this::showSound);
        btn(monitoring()?"Detener detector":"Iniciar detector",()->{if(monitoring())stopMonitor();else startMonitor();showHome();});
        card("Accesibilidad","AutoCore puede ayudar con toques, teclas y rutinas. Usa solo las acciones que necesites y revisa los permisos desde Ajustes.");
        btn("Abrir asistente virtual",this::showAssistant);
        card("Funciones","Detector de sonido · acciones táctiles · teclado AutoCore · horarios semanales · asistente · privacidad y permisos.");
        body.addView(txt("AutoCore 0.4.0 · Accesibilidad y automatización",11,MUTED,false));
    }
    private void showSound(){
        init("Detección y acciones");
        var p=getSharedPreferences("autocore",0);
        card("1. ¿Qué debe detectar?","Elige el tipo de activación. Los modos de voz y sonido se procesan de forma distinta.");
        Spinner mode=new Spinner(this);String[] modes={"Cualquier sonido (rápido)","Palabra o frase hablada","Comparar una muestra acústica"};ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,modes);mode.setAdapter(a);mode.setSelection(Math.max(0,Math.min(2,p.getInt("trigger_mode",0))));body.addView(mode);
        EditText phrase=new EditText(this);phrase.setSingleLine(true);phrase.setTextColor(Color.WHITE);phrase.setHintTextColor(MUTED);phrase.setHint("Ejemplo: activar, salto, siguiente");phrase.setText(p.getString("trigger_phrase",""));body.addView(txt("Palabra o frase (modo voz)",13,Color.WHITE,true));body.addView(phrase);
        card("Duración mínima del sonido","El detector rápido trabaja con bloques de audio de unos 10 ms. Puedes elegir desde 10 ms hasta 300 ms; menos duración responde antes, pero puede captar ruidos involuntarios.");
        SeekBar duration=new SeekBar(this);duration.setMax(29);duration.setProgress(Math.max(0,Math.min(29,p.getInt("min_sound_ms",30)/10-1)));body.addView(duration);
        TextView durationLabel=txt("Mínimo: "+((duration.getProgress()+1)*10)+" ms",13,TEAL,true);body.addView(durationLabel);duration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int v,boolean u){durationLabel.setText("Mínimo: "+((v+1)*10)+" ms");}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
        SeekBar sensitivity=new SeekBar(this);sensitivity.setMax(100);sensitivity.setProgress(p.getInt("sensitivity",55));body.addView(txt("Sensibilidad al sonido",13,Color.WHITE,true));body.addView(sensitivity);
        TextView sensLabel=txt("Sensibilidad: "+sensitivity.getProgress()+"%",13,TEAL,true);body.addView(sensLabel);sensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int v,boolean u){sensLabel.setText("Sensibilidad: "+v+"%");}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
        card("2. ¿Qué debe hacer?","Elige toque táctil para juegos y accesibilidad, o teclado para campos compatibles.");
        CheckBox touch=new CheckBox(this);touch.setText("Ejecutar toque táctil en pantalla");touch.setTextColor(Color.WHITE);touch.setChecked(p.getBoolean("gesture_enabled",true));body.addView(touch);
        EditText x=new EditText(this);x.setSingleLine(true);x.setInputType(2);x.setTextColor(Color.WHITE);x.setText(p.getString("gesture_x","50"));body.addView(txt("Posición X (%)",13,Color.WHITE,true));body.addView(x);
        EditText y=new EditText(this);y.setSingleLine(true);y.setInputType(2);y.setTextColor(Color.WHITE);y.setText(p.getString("gesture_y","50"));body.addView(txt("Posición Y (%)",13,Color.WHITE,true));body.addView(y);
        EditText key=new EditText(this);key.setSingleLine(true);key.setTextColor(Color.WHITE);key.setText(p.getString("sound_key","[SPACE]"));body.addView(txt("Tecla alternativa (ej. [SPACE], [ENTER])",13,Color.WHITE,true));body.addView(key);
        btn("Guardar configuración",()->{try{int xx=Integer.parseInt(x.getText().toString()),yy=Integer.parseInt(y.getText().toString());if(xx<0||xx>100||yy<0||yy>100)throw new Exception();String ph=phrase.getText().toString().trim();int m=mode.getSelectedItemPosition();if(m==1&&ph.isEmpty()){Toast.makeText(this,"Escribe la palabra o frase que debe reconocer.",Toast.LENGTH_LONG).show();return;}p.edit().putInt("trigger_mode",m).putString("trigger_phrase",ph).putInt("min_sound_ms",(duration.getProgress()+1)*10).putInt("sensitivity",sensitivity.getProgress()).putBoolean("gesture_enabled",touch.isChecked()).putString("gesture_x",String.valueOf(xx)).putString("gesture_y",String.valueOf(yy)).putInt("gesture_x_value",xx).putInt("gesture_y_value",yy).putString("sound_key",key.getText().toString().trim()).apply();Toast.makeText(this,"Configuración guardada.",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"Revisa las coordenadas: deben estar entre 0 y 100.",Toast.LENGTH_LONG).show();}});
        btn("Autorizar toques (Accesibilidad)",()->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        btn("Autorizar micrófono",()->{if(Build.VERSION.SDK_INT>=23)requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},90);});
        btn("Probar toque",()->{if(!AutoCoreAccessibilityService.tapConfigured(this))Toast.makeText(this,"Activa AutoCore en Accesibilidad primero.",Toast.LENGTH_LONG).show();});
        btn(monitoring()?"Detener escucha":"Iniciar escucha",()->{if(monitoring())stopMonitor();else startMonitor();});
        card("Sobre la precisión","Un sonido de 10–90 ms puede detectarse como evento acústico, pero una palabra hablada necesita tiempo suficiente para que Android la reconozca. El reconocimiento de voz depende del servicio de voz y del idioma disponibles en el dispositivo.");
        card("Privacidad","El modo de sonido analiza el nivel del micrófono en el dispositivo. El modo de voz usa el reconocedor de Android, que puede depender de servicios locales o en línea según tu móvil. Puedes detenerlo cuando quieras.");
    }
    private void startMonitor(){
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},91);return;}
        int mode=getSharedPreferences("autocore",0).getInt("trigger_mode",0);
        if(mode==1&&!SpeechRecognizer.isRecognitionAvailable(this)){Toast.makeText(this,"No hay un reconocedor de voz disponible en este dispositivo.",Toast.LENGTH_LONG).show();return;}
        Intent i=new Intent(this,SoundMonitorService.class);try{if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);getSharedPreferences("autocore",0).edit().putBoolean("monitor_ui",true).apply();Toast.makeText(this,"Detector iniciado.",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"No se pudo iniciar: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private void stopMonitor(){stopService(new Intent(this,SoundMonitorService.class));getSharedPreferences("autocore",0).edit().putBoolean("monitor_ui",false).apply();Toast.makeText(this,"Detector detenido; micrófono liberado.",Toast.LENGTH_SHORT).show();}
    private void showSchedule(){
        init("Horarios semanales");
        var p=getSharedPreferences("autocore",0);
        card("Programar No molestar","Dos reglas independientes que se repiten cada semana.");
        addSchedule("Activar No molestar","silent",7,55);addSchedule("Desactivar No molestar","normal",14,5);
        btn("Permitir alarmas exactas",()->{if(Build.VERSION.SDK_INT>=31)startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));});
        btn("Permiso de No molestar",()->startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)));
    }
    private void addSchedule(String title,String key,int dh,int dm){
        var p=getSharedPreferences("autocore",0);LinearLayout c=col();c.setBackgroundColor(CARD);c.setPadding(dp(14),dp(10),dp(14),dp(10));c.addView(txt(title,16,Color.WHITE,true));
        CheckBox enabled=new CheckBox(this);enabled.setText("Activar");enabled.setTextColor(Color.WHITE);enabled.setChecked(p.getBoolean(key+"_enabled",false));c.addView(enabled);
        int h=p.getInt(key+"_hour",dh),m=p.getInt(key+"_minute",dm);TextView time=txt(String.format("%02d:%02d",h,m),20,TEAL,true);c.addView(time);Button pick=new Button(this);pick.setText("Elegir hora");pick.setOnClickListener(v->new TimePickerDialog(this,(tp,hh,mm)->{time.setText(String.format("%02d:%02d",hh,mm));time.setTag(new int[]{hh,mm});},h,m,true).show());c.addView(pick);
        String[] days={"L","M","X","J","V","S","D"};CheckBox[] checks=new CheckBox[7];int mask=p.getInt(key+"_days",31);LinearLayout daysRow=row();for(int i=0;i<7;i++){checks[i]=new CheckBox(this);checks[i].setText(days[i]);checks[i].setTextColor(Color.WHITE);checks[i].setTextSize(11);checks[i].setChecked((mask&(1<<i))!=0);daysRow.addView(checks[i],new LinearLayout.LayoutParams(0,-2,1));}c.addView(daysRow);
        Button save=new Button(this);save.setText("Guardar horario");save.setOnClickListener(v->{int bits=0;for(int i=0;i<7;i++)if(checks[i].isChecked())bits|=1<<i;if(enabled.isChecked()&&bits==0){Toast.makeText(this,"Selecciona al menos un día.",Toast.LENGTH_SHORT).show();return;}int[] hm=(int[])time.getTag();if(hm==null)hm=new int[]{h,m};p.edit().putBoolean(key+"_enabled",enabled.isChecked()).putInt(key+"_hour",hm[0]).putInt(key+"_minute",hm[1]).putInt(key+"_days",bits).apply();ScheduleUtils.scheduleNext(this,key);Toast.makeText(this,"Horario guardado.",Toast.LENGTH_SHORT).show();});c.addView(save);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(12);body.addView(c,lp);
    }
    private void showAssistant(){
        init("Asistente Auto");
        card("¡Hola! Soy Auto, el asistente de AutoCore.","Puedo orientarte para configurar detecciones, acciones, permisos y privacidad. No necesito acceder a tus datos para mostrar esta ayuda.");
        String[][] qa={{"Quiero detectar una palabra","Entra en Sonido, elige «Palabra o frase hablada», escribe la expresión exacta y guarda. Android debe tener un reconocedor de voz disponible."},{"Quiero detectar un sonido muy corto","Elige «Cualquier sonido» y ajusta la duración mínima. 10 ms es un ajuste experimental sensible al ruido; empieza con 30–50 ms."},{"Quiero usarlo en Geometry Dash","Activa el toque táctil, configura X e Y en porcentaje, autoriza AutoCore en Accesibilidad y prueba el toque antes de iniciar el detector."},{"No funciona el toque","Comprueba que el servicio de accesibilidad está activado, que el juego está en primer plano y que las coordenadas caen dentro de la zona deseada."},{"¿Qué ocurre con mi micrófono?","El detector necesita el micrófono mientras está activo. Detén la escucha cuando termines. El modo de voz puede utilizar servicios de reconocimiento del sistema."},{"Necesito ayuda de accesibilidad","Configura una sola acción cada vez, usa una duración mayor si hay activaciones accidentales y prueba primero con una aplicación no crítica."}};
        for(String[] item:qa){Button b=btn(item[0],()->{card("Auto responde",item[1]);});}
        card("Consejo","No todas las funciones son compatibles con todos los móviles. Android puede limitar el micrófono en segundo plano, el reconocimiento de voz o los gestos según la versión y el fabricante.");
    }
    private void showSettings(){
        init("Ajustes y privacidad");
        btn("Permisos de la aplicación",()->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))));
        btn("Ajustes de accesibilidad",()->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        btn("Configurar teclado AutoCore",()->startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        btn("Elegir teclado",()->((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker());
        card("Privacidad y control","AutoCore solo escucha cuando activas el detector. El indicador/servicio de Android permanece visible durante la escucha. Puedes detenerlo desde la aplicación o desde la notificación.");
        card("Versión","AutoCore 0.4.0 · detección rápida de sonidos, activación por palabra/frase, gestos táctiles, teclado y horarios.");
    }
}
