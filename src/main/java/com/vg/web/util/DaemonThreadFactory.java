package com.vg.web.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DaemonThreadFactory implements ThreadFactory {
    private String name;

    public DaemonThreadFactory(String name) {
        this.name = name;
    }

    private AtomicInteger serial = new AtomicInteger();

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setName(name + "-" + serial.getAndIncrement());
        t.setDaemon(true);
        return t;
    }

}
