package com.vg.web.db;

import static com.vg.web.GsonFactory.fromJson;
import static com.vg.web.GsonFactory.gsonToString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.vg.web.GsonFactory;
import com.vg.web.socket.PubSubRedisChannel;
import com.vg.web.socket.PubSubUpdateListener;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class BaseJsonRedisDao<T> extends RedisDao {

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

    public BaseJsonRedisDao(JedisPool pool, String kPrefix, Class<T> _class) {
        this(pool, kPrefix + "_obj", kPrefix + "_mtime", kPrefix + "_channel", _class);
    }

    protected BaseJsonRedisDao(JedisPool pool, String kHash, String kMtime, String kChannel, Class<T> _class) {
        super(pool);
        this._kHash = kHash;
        this.kMtime = kMtime;
        this.kChannel = kChannel;
        this._class = _class;
        try {
            this.revisionField = _class.getDeclaredField("_rev");
        } catch (Exception e) {
        }

        this.pubSub = new PubSubRedisChannel(pool, kChannel);

    }

    //C
    public String create(T item) {
        String id = newId(item);
        withRedisTransactionOnOk(tx -> {
            create(tx, id, item);
        }, () -> publish(id));
        return id;
    }

    public void publishId(String itemId) {
        T o = get(itemId);
        if (o != null) {
            publish(GsonFactory.gsonToString(o));
        } else {
            System.err.println("Item does not exist but should " + itemId);
        }
    }

    protected void create(Transaction tx, String id, T item) {
        tx.zadd(kMtime, System.currentTimeMillis(), id);
        tx.hset(kHash(id), fJson(0), gsonToString(item));
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
            if (isNotBlank(id)) {
                long rev = _dbRev(r, id);
                String hget = r.hget(kHash(id), fJson(rev));
                T item = fromJson(hget, _class);
                if (item == null) {
                    System.err.println("null object at " + kHash(id) + " " + fJson(rev));
                } else {
                    setRevision(item, rev);
                }
                return item;
            }
        } catch (Exception e) {
        }
        return null;
    }

    private long _nextRev(Jedis r, String id) {
        return toLong(r.hget(kHash(id), fRevision));
    }

    private void setRevision(T item, long rev) {
        if (revisionField != null) {
            try {
                revisionField.set(item, rev);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private long getRevision(T item) {
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
            if (rev == -1) {
                withRedisTransaction(r, tx -> {
                    _update(tx, id, item);
                });
                publish(id);
                return true;
            }
            r.watch(kHash(id));
            long dbrev = _dbRev(r, id);
            if (dbrev != rev) {
                return false;
            }
            List<Object> result = withRedisTransaction(r, tx -> {
                _update(tx, id, item);
                tx.hincrBy(kHash(id), fRevision, 1);
            });
            if (result != null) {
                publish(id);
                return true;
            }
            return false;
        });
        if (!ok) {
            System.err.println("error updating " + id);
        }
        return ok;
    }

    private long _dbRev(Jedis r, String id) {
        return Math.max(0, _nextRev(r, id) - 1);
    }

    private static long toLong(Object object) {
        return toLong(object, 0);
    }

    private static long toLong(Object object, long defaultValue) {
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

    protected void _update(Transaction tx, String id, T item) {
        String gsonToString = gsonToString(item);
        tx.hset(kHash(id), fJson(getRevision(item) + 1), gsonToString);
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

    private final Multimap<String, PubSubUpdateListener> listeners = ArrayListMultimap.create();
    private final List<PubSubUpdateListener> allMessagesListeners = new CopyOnWriteArrayList<>();

    PubSubUpdateListener mainListener = id -> {
        synchronized (listeners) {
            if (listeners.containsKey(id)) {
                listeners.get(id).forEach(x -> x.accept(id));
            }
            allMessagesListeners.forEach(x -> x.accept(id));
        }
    };
    private Field revisionField;

    public void startPubSub() {
        pubSub.subscribe(mainListener);
    }

    public void stop() {
        this.pubSub.unsubscribe(mainListener);
    }

    public Subscription subscribe(String videoId, PubSubUpdateListener listener) {
        synchronized (listeners) {
            listeners.put(videoId, listener);
            return Subscriptions.create(() -> {
                synchronized (listeners) {
                    listeners.remove(videoId, listener);
                }
            });
        }
    }

    public void unsubscribe(String videoId, PubSubUpdateListener listener) {
        synchronized (listeners) {
            listeners.remove(videoId, listener);
        }
    }

    public void publish(String message) {
        pubSub.publish(message);
    }

    public Subscription subscribe(PubSubUpdateListener listener) {
        allMessagesListeners.add(listener);
        return Subscriptions.create(() -> allMessagesListeners.remove(listener));
    }

    public void unsubscribe(PubSubUpdateListener listener) {
        allMessagesListeners.remove(listener);
    }

    protected void updateAndPublish(String id, Consumer<Jedis> update) {
        updateRedis(r -> {
            if (_contains(r, id)) {
                update.accept(r);
                publish(id);
            }
        });
    }
}
