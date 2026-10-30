package com.sfj3861.autocore;

import android.media.*;
import java.util.Arrays;

public final class SoundFingerprint {
    private static final int RATE=16000, FRAME=1024;
    private static final int[] FREQ={250,400,600,850,1150,1500,2100,3000,3900};
    private SoundFingerprint(){}
    public static float[] recordSample() throws Exception {
        int min=AudioRecord.getMinBufferSize(RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord r=new AudioRecord(MediaRecorder.AudioSource.MIC,RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(min,FRAME*4));
        if(r.getState()!=AudioRecord.STATE_INITIALIZED)throw new IllegalStateException("No se pudo inicializar el micrófono");
        short[] data=new short[FRAME]; double[] sum=new double[FREQ.length]; int frames=0;
        r.startRecording(); long end=System.currentTimeMillis()+3500;
        try{
            while(System.currentTimeMillis()<end){
                int n=r.read(data,0,data.length);
                if(n==FRAME){float[] f=feature(data);for(int i=0;i<f.length;i++)sum[i]+=f[i];frames++;}
            }
        }finally{try{r.stop();}catch(Exception ignored){}r.release();}
        if(frames<5)throw new IllegalStateException("La muestra es demasiado corta");
        float[] result=new float[sum.length];for(int i=0;i<sum.length;i++)result[i]=(float)(sum[i]/frames);
        normalize(result);return result;
    }
    public static float[] feature(short[] samples){
        float[] out=new float[FREQ.length]; int n=samples.length;
        for(int j=0;j<FREQ.length;j++){
            double w=2.0*Math.PI*FREQ[j]/RATE, coeff=2*Math.cos(w), q0,q1=0,q2=0;
            for(short sample:samples){q0=coeff*q1-q2+sample;q2=q1;q1=q0;}
            double power=q1*q1+q2*q2-coeff*q1*q2;
            out[j]=(float)Math.sqrt(Math.max(0,power));
        }
        normalize(out);return out;
    }
    private static void normalize(float[] v){double norm=0;for(float x:v)norm+=x*x;norm=Math.sqrt(norm);if(norm>0)for(int i=0;i<v.length;i++)v[i]/=(float)norm;}
    public static float similarity(float[] a,float[] b){if(a==null||b==null||a.length!=b.length)return 0;float s=0;for(int i=0;i<a.length;i++)s+=a[i]*b[i];return s;}
    public static String encode(float[] v){StringBuilder b=new StringBuilder();for(int i=0;i<v.length;i++){if(i>0)b.append(',');b.append(v[i]);}return b.toString();}
    public static float[] decode(String s){if(s==null||s.isEmpty())return null;try{String[] p=s.split(",");float[] a=new float[p.length];for(int i=0;i<p.length;i++)a[i]=Float.parseFloat(p[i]);return a;}catch(Exception e){return null;}}
}