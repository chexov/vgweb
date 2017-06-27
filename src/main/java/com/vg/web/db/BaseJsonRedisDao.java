package com.vg.web.db;

import java.lang.reflect.Field;

import com.google.gson.Gson;
import com.vg.web.GsonFactory;

import redis.clients.jedis.JedisPool;

public class BaseJsonRedisDao<T> extends BaseMvccRedisDao<T> {

    private final Gson gson;
    private final Field revisionField;

    public BaseJsonRedisDao(JedisPool pool, String kPrefix, Class<T> _class) {
        this(pool, kPrefix + "_obj", kPrefix + "_mtime", kPrefix + "_channel", _class);
    }

    public BaseJsonRedisDao(JedisPool pool, String kHash, String kMtime, String kChannel, Class<T> _class) {
        this(pool, kHash, kMtime, kChannel, _class, GsonFactory.defaultGson());
    }
    
    public BaseJsonRedisDao(JedisPool pool, String kHash, String kMtime, String kChannel, Class<T> _class, Gson gson) {
        super(pool, kHash, kMtime, kChannel, _class);
        this.revisionField = revisionField(_class);
        this.gson = gson;
    }

    private Field revisionField(Class<T> cls) {
        try {
            return cls.getDeclaredField("_rev");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected String serialize(T object) {
        return gson.toJson(object);
    }

    @Override
    protected T deserialize(String value) {
        return gson.fromJson(value, _class);
    }
    
    @Override
    protected void setRevision(T item, long rev) {
        if (item != null && revisionField != null) {
            try {
                revisionField.set(item, rev);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    @Override
    protected long getRevision(T item) {
        if (revisionField != null) {
            try {
                return toLong(revisionField.get(item));
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return -1;
    }
    


}
