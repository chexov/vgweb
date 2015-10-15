package com.vg.web.db;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.vg.web.GsonFactory;
import com.vg.web.socket.PubSubRedisChannel;
import com.vg.web.socket.PubSubUpdateListener;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.vg.web.GsonFactory.fromJson;
import static com.vg.web.GsonFactory.gsonToString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
            create(tx, id, item);
        } , () -> publishId(id));
        return id;
    }

    private void publishId(String itemId) {
        T o = get(itemId);
        if (o != null) {
            publish(GsonFactory.gsonToString(o));
        } else {
            System.err.println("Item does not exist but should " + itemId);
        }
    }

    protected void create(Transaction tx, String id, T item) {
        tx.zadd(kMtime, System.currentTimeMillis(), id);
        tx.hset(kHash(id), fJson, gsonToString(item));
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

    public List<T> getLatest(String startId, long count) {
        return withRedis(r -> {
            Set<String> ids;
            if (startId == null) {
                ids = r.zrevrange(kMtime, 0, count - 1);
            } else {
                Long start = r.zrevrank(kMtime, startId);
                if (start != null) {
                    ids = r.zrevrange(kMtime, start, start + count - 1);
                } else {
                    ids = Collections.emptySet();
                }
            }

            return ids.stream()
                      .map(id -> _get(r, id))
                      .collect(toList());
        });
    }

    //U
    public void update(String id, T item) {
        updateRedis(r -> {
            if (_contains(r, id)) {
                r.hset(kHash(id), fJson, gsonToString(item));
                publish(id);
            }
        });
    }

    public boolean contains(String id) {
        return withRedis(r -> _contains(r, id));
    }

    private Boolean _contains(Jedis r, String id) {
        return r.exists(kHash(id));
    }

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
                listeners.get(videoId).forEach(x -> x.onMessage(videoId));
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

    public void subscribe(PubSubUpdateListener listener) {
        allMessagesListeners.add(listener);
    }

    public void unsubscribe(PubSubUpdateListener listener) {
        allMessagesListeners.remove(listener);
    }

}
