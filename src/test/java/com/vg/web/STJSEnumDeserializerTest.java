package com.vg.web;

import static org.junit.Assert.*;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class STJSEnumDeserializerTest {
    public enum Meaning {
        HELLO, WORLD, LIFE, UNIVERSE, EVERYTHING
    }

    @Test
    public void testSTJSEnumDeserializer() throws Exception {
        GsonBuilder gb = new GsonBuilder();
        gb.registerTypeHierarchyAdapter(Enum.class, new STJSEnumDeserializer());

        Gson gson = gb.create();
        String json = gson.toJson(Meaning.LIFE);
        System.out.println(json);
        assertEquals(Meaning.LIFE, gson.fromJson("{\"_name\":\"LIFE\",\"_ordinal\":2}", Meaning.class));
        assertEquals(Meaning.UNIVERSE, gson.fromJson("\"UNIVERSE\"", Meaning.class));
        assertNull(gson.fromJson("{}", Meaning.class));
        assertNull(gson.fromJson("null", Meaning.class));
        assertNull(gson.fromJson("{\"_name\":\"UNKNOWN_ENUM_VALUE\",\"_ordinal\":2}", Meaning.class));
    }

}
