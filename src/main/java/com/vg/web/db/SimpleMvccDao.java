package com.vg.web.db;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import redis.clients.jedis.JedisPool;

public class SimpleMvccDao extends BaseMvccRedisDao<Pair<Long, String>>{

    public SimpleMvccDao(JedisPool pool, String kPrefix) {
        super(pool, kPrefix, (Class) Pair.class);
    }
    
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
}
