package com.loverofdarkness.remotebtkeyboard;

import java.util.ArrayList;
import java.util.List;

final class KeyCodec {
    static final int CTRL=1, SHIFT=2, ALT=4, GUI=8;
    static final int ENTER=0x28, ESC=0x29, BACKSPACE=0x2A, TAB=0x2B, DELETE=0x4C;
    static final int UP=0x52, DOWN=0x51, LEFT=0x50, RIGHT=0x4F, HOME=0x4A, END=0x4D, PGUP=0x4B, PGDN=0x4E;
    static final int INSERT=0x49, CAPS=0x39;
    static final int PRINT=0x46, SCROLL=0x47, PAUSE=0x48;
    private static final byte[] DESC = {
        0x05,0x01,0x09,0x06,(byte)0xA1,0x01,0x05,0x07,0x19,(byte)0xE0,0x29,(byte)0xE7,
        0x15,0x00,0x25,0x01,0x75,0x01,(byte)0x95,0x08,(byte)0x81,0x02,
        (byte)0x95,0x01,0x75,0x08,(byte)0x81,0x01,
        (byte)0x95,0x05,0x75,0x01,0x05,0x08,0x19,0x01,0x29,0x05,(byte)0x91,0x02,
        (byte)0x95,0x01,0x75,0x03,(byte)0x91,0x01,
        (byte)0x95,0x06,0x75,0x08,0x15,0x00,0x25,0x65,0x05,0x07,0x19,0x00,0x29,0x65,(byte)0x81,0x00,
        (byte)0xC0
    };
    static byte[] descriptor(){ return DESC.clone(); }
    static byte[] report(int mod,int usage){ return new byte[]{(byte)mod,0,(byte)usage,0,0,0,0,0}; }
    static byte[] release(){ return new byte[8]; }
    static Stroke charStroke(char c, boolean caps){
        if(c>='a'&&c<='z') return new Stroke(0x04+c-'a', caps?SHIFT:0);
        if(c>='A'&&c<='Z') return new Stroke(0x04+c-'A', caps?0:SHIFT);
        if(c>='1'&&c<='9') return new Stroke(0x1E+c-'1',0);
        if(c=='0') return new Stroke(0x27,0);
        String s="!@#$%^&*()"; int i=s.indexOf(c); if(i>=0) return new Stroke(i==9?0x27:0x1E+i,SHIFT);
        switch(c){
            case ' ':return new Stroke(0x2C,0); case '-':return new Stroke(0x2D,0); case '_':return new Stroke(0x2D,SHIFT);
            case '=':return new Stroke(0x2E,0); case '+':return new Stroke(0x2E,SHIFT); case '[':return new Stroke(0x2F,0); case '{':return new Stroke(0x2F,SHIFT);
            case ']':return new Stroke(0x30,0); case '}':return new Stroke(0x30,SHIFT); case '\\':return new Stroke(0x31,0); case '|':return new Stroke(0x31,SHIFT);
            case ';':return new Stroke(0x33,0); case ':':return new Stroke(0x33,SHIFT); case '\'':return new Stroke(0x34,0); case '"':return new Stroke(0x34,SHIFT);
            case '`':return new Stroke(0x35,0); case '~':return new Stroke(0x35,SHIFT); case ',':return new Stroke(0x36,0); case '<':return new Stroke(0x36,SHIFT);
            case '.':return new Stroke(0x37,0); case '>':return new Stroke(0x37,SHIFT); case '/':return new Stroke(0x38,0); case '?':return new Stroke(0x38,SHIFT);
            default: throw new IllegalArgumentException("Unsupported character: "+Integer.toHexString(c));
        }
    }
    static String normalize(String s){
        String n=s.replace("\r\n","\n").replace('\r','\n').replace('\n',' ');
        for(int i=0;i<n.length();i++) if(n.charAt(i)<0x20||n.charAt(i)>0x7E) throw new IllegalArgumentException("Only printable US ASCII is supported");
        return n;
    }
    static final class Stroke { final int usage, modifiers; Stroke(int u,int m){usage=u;modifiers=m;} }
    static List<Stroke> encode(String s, boolean caps){ List<Stroke> out=new ArrayList<>(); for(char c:s.toCharArray()) out.add(charStroke(c,caps)); return out; }
}
