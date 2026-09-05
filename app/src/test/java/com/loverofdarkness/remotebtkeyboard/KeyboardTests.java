package com.loverofdarkness.remotebtkeyboard;

import org.junit.Test;
import static org.junit.Assert.*;

public final class KeyboardTests {
    @Test public void asciiRoundTripMappingsExist(){
        for(char c=0x20;c<=0x7E;c++) assertTrue(KeyCodec.charStroke(c,false).usage>0);
    }
    @Test public void shiftedSymbols(){
        assertEquals(KeyCodec.SHIFT,KeyCodec.charStroke('A',false).modifiers);
        assertEquals(0x1E,KeyCodec.charStroke('!',false).usage);
        assertEquals(KeyCodec.SHIFT,KeyCodec.charStroke('!',false).modifiers);
        assertEquals(0x31,KeyCodec.charStroke('|',false).usage);
    }
    @Test public void newlineBecomesSpace(){assertEquals("a b c",KeyCodec.normalize("a\nb\r\nc"));}
    @Test(expected=IllegalArgumentException.class) public void unicodeRejected(){KeyCodec.normalize("🙂");}
    @Test public void releaseIsEightZeroBytes(){assertArrayEquals(new byte[8],KeyCodec.release());}
    @Test public void editPlanShorten(){
        EditPlan p=new EditPlan("Hello world","Hello wor");
        assertEquals(2,p.deletes);
        assertEquals(9,p.index);
        assertEquals("Hello wor",p.target);
    }
    @Test public void editPlanAppend(){
        EditPlan p=new EditPlan("Hi","Hello");
        assertEquals(0,p.deletes);
        assertEquals(2,p.index);
        assertFalse(p.done());
    }
}
