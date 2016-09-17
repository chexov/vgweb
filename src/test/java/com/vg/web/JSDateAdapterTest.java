package com.vg.web;

import static java.util.Calendar.MILLISECOND;
import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Before;
import org.junit.Test;
import org.stjs.javascript.Date;

public class JSDateAdapterTest {

    private Gson gson;

    @Before
    public void setup() {
        GsonBuilder gb = new GsonBuilder();
        gb.registerTypeAdapter(Date.class, new JSDateAdapter());
        gson = gb.create();
    }
    
    @Test
    public void testDate() throws Exception {
        Date date = new Date();
        System.out.println(date);
        String json = gson.toJson(date);
        System.out.println(json);
        Date fromJson = gson.fromJson(json, Date.class);
        System.out.println(fromJson);
        assertEquals((long) date.getTime(), (long) fromJson.getTime());
        
    }
    
    @Test
    public void testFromDateToString() throws Exception {
        String json = "\"Sat Sep 17 2016 14:25:33 GMT+0300 (EEST)\"";
        Date fromJson = gson.fromJson(json, Date.class);
        System.out.println(fromJson);

        Calendar cal = Calendar.getInstance(Locale.US);
        cal.set(2016, 8, 17, 14, 25, 33);
        cal.set(MILLISECOND, 0);
        cal.setTimeZone(TimeZone.getTimeZone("Europe/Kiev"));
        java.util.Date time = cal.getTime();
        System.out.println(time);
        assertEquals(time.getTime(), (long) fromJson.getTime());
    }
    
    @Test
    public void testFromDateToGMTString() throws Exception {
        String json = "\"Sat, 17 Sep 2016 11:31:00 GMT\"";
        Date fromJson = gson.fromJson(json, Date.class);
        System.out.println(fromJson);

        Calendar cal = Calendar.getInstance(Locale.US);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.set(2016, 8, 17, 11, 31, 0);
        cal.set(MILLISECOND, 0);
        assertEquals(cal.getTime().getTime(), (long) fromJson.getTime());
    }

}
