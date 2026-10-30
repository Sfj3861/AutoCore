package com.sfj3861.autocore;

import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.view.KeyEvent;

public class AutoCoreIme extends InputMethodService {
    private static volatile AutoCoreIme active;
    @Override public void onStartInputView(android.view.inputmethod.EditorInfo info, boolean restarting){super.onStartInputView(info,restarting);active=this;}
    @Override public void onFinishInputView(boolean finishingInput){if(active==this)active=null;super.onFinishInputView(finishingInput);}
    @Override public View onCreateInputView(){android.widget.LinearLayout l=new android.widget.LinearLayout(this);l.setOrientation(android.widget.LinearLayout.VERTICAL);l.setPadding(8,8,8,8);android.widget.TextView title=new android.widget.TextView(this);title.setText("AutoCore Keyboard · usa el teclado del sistema para introducir texto");title.setTextColor(android.graphics.Color.WHITE);l.setBackgroundColor(0xff202633);l.addView(title);android.widget.Button space=new android.widget.Button(this);space.setText("SPACE");space.setOnClickListener(v->send(" "));l.addView(space);android.widget.Button enter=new android.widget.Button(this);enter.setText("ENTER");enter.setOnClickListener(v->send("[ENTER]"));l.addView(enter);return l;}
    private void send(String value){InputConnection ic=getCurrentInputConnection();if(ic==null)return;if("[ENTER]".equals(value))ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER));else ic.commitText(value,1);}
    public static boolean sendConfiguredKey(android.content.Context c){
        AutoCoreIme ime=active;if(ime==null)return false;
        String key=c.getSharedPreferences("autocore",0).getString("sound_key","[SPACE]");
        InputConnection ic=ime.getCurrentInputConnection();if(ic==null)return false;
        switch(key.trim().toUpperCase()){
            case "[SPACE]": case "SPACE": ic.commitText(" ",1);break;
            case "[ENTER]": case "ENTER": ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER));ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_ENTER));break;
            case "[TAB]": case "TAB": ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_TAB));ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_TAB));break;
            case "[DEL]": case "[BACKSPACE]": ic.deleteSurroundingText(1,0);break;
            default: if(key.startsWith("[")&&key.endsWith("]"))return false;ic.commitText(key,1);
        }
        return true;
    }
}