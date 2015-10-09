package com.vg.web.db;

import static com.vg.web.GsonFactory.fromJson;
import static com.vg.web.GsonFactory.gsonToString;
import static com.vg.web.GsonFactory.toGson;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import com.vg.web.socket.PubSubRedisChannel;
import com.vg.web.socket.PubSubUpdateListener;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class BaseJsonRedisDao<T> extends RedisDao {

    private static final String fJson = "json";

    protected String kHash(String id) {
        return _kHash + "_" + id;

    }

    protected final String kMtime;
    protected final String kChannel;
    protected final Class<T> _class;
    protected final String _kHash;
    private PubSubRedisChannel pubSub;

    public BaseJsonRedisDao(JedisPool pool, String kPrefix, Class<T> _class) {
        this(pool, kPrefix + "_obj", kPrefix + "_mtime", kPrefix + "_channel", _class);
    }

    protected BaseJsonRedisDao(JedisPool pool, String kHash, String kMtime, String kChannel, Class<T> _class) {
        super(pool);
        this._kHash = kHash;
        this.kMtime = kMtime;
        this.kChannel = kChannel;
        this._class = _class;
    }

    //C
    public String create(T item) {
        String id = newId(item);
        withRedisTransactionOnOk(tx -> {
            tx.zadd(kMtime, System.currentTimeMillis(), id);
            tx.hset(kHash(id), fJson, gsonToString(item));
        } , () -> publishIds(id));
        return id;
    }

    protected String newId(T item) {
        return randomUUID().toString();
    }

    //R
    public T get(String id) {
        return withRedis(r -> _get(r, id));
    }

    protected T _get(Jedis r, String id) {
        return isNotBlank(id) ? fromJson(r.hget(kHash(id), fJson), _class) : null;
    }

    //U

    //D
    public void deleteTask(String id) {
        withRedisTransaction((transaction) -> {
            transaction.del(kHash(id));
            transaction.zrem(kMtime, id);
        });
    }

    private final Multimap<String, PubSubUpdateListener> listeners = ArrayListMultimap.create();
    private final List<PubSubUpdateListener> allMessagesListeners = new CopyOnWriteArrayList<>();

    PubSubUpdateListener mainListener = videoId -> {
        synchronized (listeners) {
            if (listeners.containsKey(videoId)) {
                listeners.get(videoId)
                         .forEach(x -> x.onMessage(videoId));
            }
            allMessagesListeners.forEach(x -> x.onMessage(videoId));
        }
    };

    public void startPubSub() {
        this.pubSub = new PubSubRedisChannel(pool, kChannel);
        pubSub.subscribe(mainListener);
    }

    public void stop() {
        if (pubSub != null) {
            this.pubSub.unsubscribe(mainListener);
        }
    }

    public void subscribe(String videoId, PubSubUpdateListener listener) {
        synchronized (listeners) {
            listeners.put(videoId, listener);
        }
    }

    public void unsubscribe(String videoId, PubSubUpdateListener listener) {
        synchronized (listeners) {
            listeners.remove(videoId, listener);
        }
    }

    public void publish(String message) {
        if (pubSub != null) {
            pubSub.publish(message);
        }
    }

    private void publishJson(Object src) {
        if (pubSub != null) {
            pubSub.publish(toGson(src));
        }
    }

    public void publishIds(String... ids) {
        publishJson(asList(ids));
    }

    public void subscribe(PubSubUpdateListener listener) {
        allMessagesListeners.add(listener);
    }

    public void unsubscribe(PubSubUpdateListener listener) {
        allMessagesListeners.remove(listener);
    }

}
