package com.vg.web.db;

import static com.vg.web.db.BaseMvccRedisDao.UNKNOWN_REVISION;
import static org.junit.Assert.assertEquals;
import static redis.clients.jedis.Protocol.DEFAULT_PORT;
import static redis.clients.jedis.Protocol.DEFAULT_TIMEOUT;
import static rx.schedulers.Schedulers.newThread;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import rx.Observable;

public class BaseMvccRedisDaoTest {
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
    
    @Test
    public void testCustomRevisions() throws Exception {
        Class<Pair<Long, String>> cls = (Class) Pair.class;
        BaseMvccRedisDao<Pair<Long, String>> dao = new BaseMvccRedisDao<Pair<Long,String>>(pool, "pair", cls) {
            
            @Override
            protected Pair<Long, String> setRevision(Pair<Long, String> item, long rev) {
                return MutablePair.of(rev, item.getValue());
            }
            
            @Override
            protected String serialize(Pair<Long, String> object) {
                return object.getValue();
            }
            
            @Override
            protected long getRevision(Pair<Long, String> item) {
                return item.getKey();
            }
            
            @Override
            protected Pair<Long, String> deserialize(String value) {
                return Pair.of(UNKNOWN_REVISION, value);
            }
        };
        String id = dao.create(Pair.of(UNKNOWN_REVISION, "42"));
        
        Observable.range(0, 3).flatMap(threadNumber -> {
            return Observable.range(0, 500).subscribeOn(newThread()).doOnNext(x -> {
                try {
                    Thread.sleep((long) (Math.random() * 10));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                dao.update(id, p -> p.setValue("" + (Integer.parseInt(p.getValue()) + 1)));
            });
        }).toBlocking().subscribe(x -> {
        });
        
        Pair<Long, String> pair = dao.get(id);
        assertEquals("1542", pair.getValue());
        System.out.println(pair.getKey());
    }

}
