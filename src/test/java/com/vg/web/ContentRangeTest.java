package com.vg.web;

import static org.junit.Assert.*;

import org.junit.Test;

public class ContentRangeTest {
    @Test
    public void testParseRange() throws Exception {
        ContentRange parseRange = ContentRange.parseRange("bytes=2-42", 40);
        System.out.println(parseRange);
        assertEquals(38, parseRange.getCount());
    }

}
