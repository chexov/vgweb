package com.vg.web.db;

import com.google.gson.Gson;
import com.vg.web.GsonFactory;

import redis.clients.jedis.JedisPool;

public class BaseJsonRedisDao<T> extends BaseMvccRedisDao<T> {

    private final Gson gson;

    public BaseJsonRedisDao(JedisPool pool, String kPrefix, Class<T> _class) {
        this(pool, kPrefix + "_obj", kPrefix + "_mtime", kPrefix + "_channel", _class);
    }

    public BaseJsonRedisDao(JedisPool pool, String kHash, String kMtime, String kChannel, Class<T> _class) {
        this(pool, kHash, kMtime, kChannel, _class, GsonFactory.defaultGson());
    }
    
    public BaseJsonRedisDao(JedisPool pool, String kHash, String kMtime, String kChannel, Class<T> _class, Gson gson) {
        super(pool, kHash, kMtime, kChannel, _class);
        this.gson = gson;
    }

    @Override
    protected String serialize(T object) {
        return gson.toJson(object);
    }

    @Override
    protected T deserialize(String value) {
        return gson.fromJson(value, _class);
    }

}
