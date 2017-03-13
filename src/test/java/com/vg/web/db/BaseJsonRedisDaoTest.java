package com.vg.web.db;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.*;
import static redis.clients.jedis.Protocol.DEFAULT_PORT;
import static redis.clients.jedis.Protocol.DEFAULT_TIMEOUT;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vg.web.socket.PubSubUpdateListener;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import rx.Observable;
import rx.subjects.ReplaySubject;

public class BaseJsonRedisDaoTest {
    public static GenericObjectPoolConfig poolConfig() {
        GenericObjectPoolConfig pc = new GenericObjectPoolConfig();
        pc.setMaxTotal(160);
        pc.setTestOnBorrow(true);
        pc.setMinIdle(1);
        pc.setMaxIdle(5);
        pc.setTestWhileIdle(true);
        return pc;
    }

    private JedisPool pool;

    @Before
    public void setup() {
        this.pool = new JedisPool(poolConfig(), "localhost", DEFAULT_PORT, DEFAULT_TIMEOUT, null, 4);
        try (Jedis r = pool.getResource()) {
            r.flushDB();
        }
    }

    @After
    public void teardown() {
        pool.close();
    }

    @Test
    public void beforeUpdateItemIsThere() throws Exception {

        BaseJsonRedisDao<Task> dao = new BaseJsonRedisDao<Task>(pool, "task", Task.class) {
            @Override
            protected void _update(Transaction tx, String id, Task beforeItem, Task item) {
                super._update(tx, id, beforeItem, item);
            }
        };
    }

    @Test
    public void testReturnNullOnGet() throws Exception {
        BaseJsonRedisDao<Task> dao = new BaseJsonRedisDao<>(pool, "task", Task.class);
        Assert.assertNull(dao.get("bla"));
    }
    
    @Test
    public void testSubscribe() throws Exception {
        BaseJsonRedisDao<Task> dao = new BaseJsonRedisDao<>(pool, "task", Task.class);
        Observable<String> allEvents = dao.updates().replay().autoConnect();
        allEvents.subscribe();

        ReplaySubject<Boolean> handledEvents = ReplaySubject.create();
        List<String> fired = new CopyOnWriteArrayList<>();
        PubSubUpdateListener listener = x -> {
            fired.add(x);
            handledEvents.onNext(true);
        };
        dao.subscribe(listener);
        
        String t1 = dao.create(new Task());
        String t2 = dao.create(new Task());
        wait5sec(allEvents.take(2));
        wait5sec(handledEvents.take(2));
        assertArrayEquals(new String[] { t1, t2 }, fired.toArray(new String[0]));
        dao.unsubscribe(listener);

        AtomicInteger t1fired = new AtomicInteger();
        AtomicInteger t2fired = new AtomicInteger();
        PubSubUpdateListener listener1 = x -> {
            t1fired.incrementAndGet();
            handledEvents.onNext(true);

        };
        PubSubUpdateListener listener2 = x -> {
            t2fired.incrementAndGet();
            handledEvents.onNext(true);
        };
        dao.subscribe(t1, listener1);
        dao.subscribe(t2, listener2);

        assertEquals(0, t1fired.intValue());
        assertEquals(0, t2fired.intValue());
        
        dao.update(t1, t -> t.counter++);
        dao.update(t2, t -> t.counter++);
        
        wait5sec(allEvents.take(4));
        wait5sec(handledEvents.take(4));

        assertEquals(1, t1fired.intValue());
        assertEquals(1, t2fired.intValue());
        
        dao.unsubscribe(t1, listener1);

        dao.update(t1, t -> t.counter++);
        dao.update(t2, t -> t.counter++);
        
        wait5sec(allEvents.take(6));
        wait5sec(handledEvents.take(5));

        assertEquals(1, t1fired.intValue());
        assertEquals(2, t2fired.intValue());

    }

    private static void sleep100() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static <T> void wait5sec(Observable<T> take) {
        take.toList().timeout(5, SECONDS).toBlocking().first();
    }

    @Test
    public void testCreateGet() throws Exception {
        BaseJsonRedisDao<Dimension> dao = new BaseJsonRedisDao<>(pool, "test", Dimension.class);
        String id1 = dao.create(new Dimension(42, 43));
        String id2 = dao.create(new Dimension(142, 143));
        assertEquals(42, dao.get(id1).width);
        assertEquals(142, dao.get(id2).width);
        Dimension dimension = dao.get(id1);
        dimension.width = 43;
        dao.update(id1, dimension);
        assertEquals(43, dao.get(id1).width);
        dao.delete(id1);
        dao.delete(id2);
        assertNull(dao.get(id1));
        assertNull(dao.get(id2));
    }

    public static class Task {
        public long _rev;
        public String id;
        public Dimension videoDim;
        public String message;
        public int counter;
    }

    @Test
    public void testRevisions() throws Exception {
        BaseJsonRedisDao<Task> dao = new BaseJsonRedisDao<>(pool, "task", Task.class);
        String id = dao.create(new Task());
        Task task = dao.get(id);
        task.message = "hello";
        assertTrue(dao.update(id, task));
        assertFalse(dao.update(id, task));
        Task task2 = dao.get(id);
        task2.message = "world";
        assertTrue(dao.update(id, task2));
        assertFalse(dao.update(id, task2));
        ExecutorService _pool = Executors.newFixedThreadPool(2);
        List<Future<?>> futures = new ArrayList<>();
        Runnable incrementCounter = () -> {
            Task _t1;
            int retry = 0;
            do {
                if (retry > 0) {
                    System.out.println(Thread.currentThread().getName() + " retry " + retry);
                }
                _t1 = dao.get(id);
                _t1.counter++;
                retry++;
            } while (!dao.update(id, _t1));
        };
        for (int i = 0; i < 500; i++) {
            futures.add(_pool.submit(incrementCounter));
            futures.add(_pool.submit(incrementCounter));
        }
        for (Future<?> future : futures) {
            future.get();
        }
        _pool.shutdown();
        Task task3 = dao.get(id);
        assertEquals(1000, task3.counter);
    }

    @Test
    public void testWatch() throws Exception {
        Jedis j1 = pool.getResource();
        Jedis j2 = pool.getResource();

        j1.set("a", "1");

        j1.watch("a");
        Transaction t1 = j1.multi();
        t1.incr("a");

        j2.watch("a");
        Transaction t2 = j2.multi();
        t2.incr("a");

        System.out.println(t1.exec());
        System.out.println(t2.exec());
    }

    @Test
    public void testCreateRev() throws Exception {
        BaseJsonRedisDao<Task> dao = new BaseJsonRedisDao<>(pool, "task", Task.class);
        Task t = new Task();
        t.message = "msg";
        t.counter = 42;
        String id = dao.create(t);
        Task task = dao.get(id);
        assertEquals(0, task._rev);
        task.message = "msg2";
        dao.update(id, task);
        Task task1 = dao.get(id);
        assertEquals(1, task1._rev);
    }
}
