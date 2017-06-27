package com.vg.web.db;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.vg.web.socket.PubSubRedisChannel;
import com.vg.web.socket.PubSubUpdateListener;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import rx.Observable;
import rx.Subscription;

public abstract class BaseMvccRedisDao<T> extends RedisDao {

    public static final long UNKNOWN_REVISION = -1;
    protected static final String fRevision = "_rev";
    protected static final String fJson = "json";

    protected String kHash(String id) {
        return _kHash + "_" + id;
    }

    protected final String kMtime;
    protected final String kChannel;
    protected final Class<T> _class;
    protected final String _kHash;
    private final PubSubRedisChannel pubSub;
    private Observable<String> messages;

    public BaseMvccRedisDao(JedisPool pool, String kPrefix, Class<T> _class) {
        this(pool, kPrefix + "_obj", kPrefix + "_mtime", kPrefix + "_channel", _class);
    }

    protected BaseMvccRedisDao(JedisPool pool, String kHash, String kMtime, String kChannel, Class<T> _class) {
        super(pool);
        this._kHash = kHash;
        this.kMtime = kMtime;
        this.kChannel = kChannel;
        this._class = _class;

        this.pubSub = new PubSubRedisChannel(pool, kChannel);
        this.messages = pubSub.messagesOnNewThread().share();

    }

    //C
    public String create(T item) {
        String id = newId(item);
        withRedisTransactionOnOk(tx -> {
            create(tx, id, item);
        }, () -> publish(id, null, item));
        return id;
    }

    public void publishId(String itemId) {
        T o = get(itemId);
        if (o != null) {
            publish(serialize(o));
        } else {
            error("Item does not exist but should %s", itemId);
        }
    }

    protected abstract String serialize(T object);

    protected abstract T deserialize(String value);

    protected abstract T setRevision(T item, long rev);
    
    protected abstract long getRevision(T item);

    protected void create(Transaction tx, String id, T item) {
        tx.zadd(kMtime, System.currentTimeMillis(), id);
        tx.hset(kHash(id), fJson(0), serialize(item));
        tx.hset(kHash(id), fRevision, "1");
    }

    protected String newId(T item) {
        return randomUUID().toString();
    }

    //R
    public T get(String id) {
        return withRedis(r -> _get(r, id));
    }

    protected T _get(Jedis r, String id) {
        try {
            T item = null;
            long rev = 0;
            do {
                rev = _dbRev(r, id);
                String hget = r.hget(kHash(id), fJson(rev));
                if (hget != null) {
                    item = deserialize(hget);
                }
            } while (item == null && _contains(r, id));
            return setRevision(item, rev);
        } catch (Exception e) {
        }
        return null;
    }

    private long _nextRev(Jedis r, String id) {
        return toLong(r.hget(kHash(id), fRevision));
    }

    private String fJson(long rev) {
        return rev <= 0 ? fJson : fJson + "" + rev;
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

            return ids.stream().map(id -> _get(r, id)).collect(toList());
        });
    }

    public T update(String id, Consumer<T> transformer) {
        return update(id, null, transformer);
    }

    public T update(String id, Predicate<T> predicate, Consumer<T> transformer) {
        T j;
        do {
            j = get(id);
            if (j == null || (predicate != null && !predicate.test(j))) {
                break;
            }
            transformer.accept(j);
        } while (!update(id, j));
        return j;
    }

    //U
    public boolean update(String id, T item) {
        boolean ok = withRedis(r -> {
            if (!_contains(r, id)) {
                return false;
            }
            long rev = getRevision(item);
            if (rev == UNKNOWN_REVISION) {
                withRedisTransaction(r, tx -> {
                    _update(tx, id, null, item);
                });
                publish(id, null, item);
                return true;
            }
            r.watch(kHash(id));
            long dbrev = _dbRev(r, id);
            if (dbrev != rev) {
                return false;
            }
            T beforeItem = get(id);
            List<Object> result = withRedisTransaction(r, tx -> {
                _update(tx, id, beforeItem, item);
                tx.hincrBy(kHash(id), fRevision, 1);
            });
            if (result != null) {
                publish(id, beforeItem, item);
                return true;
            }
            return false;
        });
        if (!ok) {
            error("error updating %s", id);
        }
        return ok;
    }

    private final static boolean debug = false;

    private void error(String format, Object... args) {
        if (debug) {
            if (!format.endsWith("\n")) {
                format += "\n";
            }
            System.err.printf(format, args);
        }
    }

    private long _dbRev(Jedis r, String id) {
        return Math.max(0, _nextRev(r, id) - 1);
    }

    protected static long toLong(Object object) {
        return toLong(object, 0);
    }

    protected static long toLong(Object object, long defaultValue) {
        if (object == null) {
            return defaultValue;
        }
        if (object instanceof Number) {
            return ((Number) object).longValue();
        } else if (object instanceof String) {
            try {
                return Long.parseLong((String) object);
            } catch (NumberFormatException nfe) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    protected void _update(Transaction tx, String id, T beforeItem, T item) {
        String value = serialize(item);
        long rev = getRevision(item);
        String _fJsonR0 = fJson(rev);
        String _fJsonR1 = fJson(rev + 1);
        tx.hset(kHash(id), _fJsonR1, value);
        if (!_fJsonR0.equals(_fJsonR1)) {
            tx.hdel(kHash(id), _fJsonR0);
        }
    }

    public boolean contains(String id) {
        return id != null ? withRedis(r -> _contains(r, id)) : false;
    }

    protected Boolean _contains(Jedis r, String id) {
        return id != null ? r.exists(kHash(id)) : false;
    }

    public void delete(String id) {
        withRedisTransaction((transaction) -> {
            delete(transaction, id);
        });
    }

    protected void delete(Transaction tx, String id) {
        tx.del(kHash(id));
        tx.zrem(kMtime, id);
    }

    private final Map<String, Subscription> listeners = new ConcurrentHashMap<>();

    public Subscription subscribe(String videoId, PubSubUpdateListener listener) {
        String key = listenerKey(videoId, listener);
        Subscription subscribe = updates().filter(id -> videoId.equals(id)).subscribe(x -> listener.accept(x));
        Subscription old = listeners.put(key, subscribe);
        unsubscribe(old);
        return subscribe;

    }

    private static String listenerKey(String videoId, PubSubUpdateListener listener) {
        int id = System.identityHashCode(listener);
        return videoId == null ? "" : videoId + "/" + id;
    }

    public void unsubscribe(String videoId, PubSubUpdateListener listener) {
        unsubscribe(listeners.remove(listenerKey(videoId, listener)));
    }

    protected void publish(String id, T before, T now) {
        pubSub.publish(id);
    }
    
    protected void publish(String message) {
        pubSub.publish(message);
    }

    public Subscription subscribe(PubSubUpdateListener listener) {
        Subscription subscribe = updates().subscribe(x -> listener.accept(x));
        Subscription old = listeners.put(listenerKey(null, listener), subscribe);
        unsubscribe(old);
        return subscribe;
    }

    private static void unsubscribe(Subscription sub) {
        if (sub != null && !sub.isUnsubscribed()) {
            sub.unsubscribe();
        }
    }

    public void unsubscribe(PubSubUpdateListener listener) {
        unsubscribe(listeners.remove(listenerKey(null, listener)));
    }

    protected void updateAndPublish(String id, Consumer<Jedis> update) {
        updateRedis(r -> {
            if (_contains(r, id)) {
                update.accept(r);
                publish(id);
            }
        });
    }
    
    public Observable<String> listIds() {
        return withRedisRx(r -> zscanElements(r, kMtime));
    }
    
    public Observable<String> updates() {
        return messages;
    }

}
