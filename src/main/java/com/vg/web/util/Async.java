package com.vg.web.util;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Async {

    public final static ExecutorService pool = Executors
            .newCachedThreadPool(new DaemonThreadFactory(Async.class.getName()));

    public static class ExceptionReportingRunnable implements Runnable {
        private final Runnable r;

        public ExceptionReportingRunnable(Runnable r) {
            this.r = r;
        }

        @Override
        public void run() {
            try {
                r.run();
            } catch (Throwable t) {
                UncaughtExceptionHandler eh = Thread.getDefaultUncaughtExceptionHandler();
                if (eh != null) {
                    eh.uncaughtException(Thread.currentThread(), t);
                } else {
                    t.printStackTrace();
                }
                throw t;
            }
        }
    }

    public static class ExceptionReportingCallable<V> implements Callable<V> {
        private Callable<V> c;

        public ExceptionReportingCallable(Callable<V> c) {
            this.c = c;
        }

        @Override
        public V call() throws Exception {
            try {
                return c.call();
            } catch (Throwable t) {
                UncaughtExceptionHandler eh = Thread.getDefaultUncaughtExceptionHandler();
                if (eh != null) {
                    eh.uncaughtException(Thread.currentThread(), t);
                } else {
                    t.printStackTrace();
                }
                throw t;
            }
        }
    }

    public static <T> Future<T> async(Callable<T> callable) {
        return pool.submit(ex(callable));
    }

    public static Future<?> async(Runnable runnable) {
        return pool.submit(ex(runnable));
    }

    public static Runnable ex(Runnable r) {
        return new ExceptionReportingRunnable(r);
    }

    public static <V> Callable<V> ex(Callable<V> c) {
        return new ExceptionReportingCallable<>(c);
    }

}
