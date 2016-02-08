package com.vg.web.socket;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.vg.web.db.RedisDao;
import com.vg.web.util.Async;
import com.vg.web.util.DaemonThreadFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

public class PubSubRedisChannel extends RedisDao {
    //    public static final Logger log = LogManager.getLogger(PubSubRedisChannel.class);
    private final long redisDb;
    private final String channelId;
    private Runnable redisListener = Async.ex(new PubSubUpdatedChannel());
    private ScheduledExecutorService pubsubThread;
    private Future<?> pubsubJob;

    public PubSubRedisChannel(JedisPool pool, String channelId) {
        super(pool);
        this.channelId = channelId;
        this.redisDb = getDb();
    }

    private final List<PubSubUpdateListener> listeners = new CopyOnWriteArrayList<>();

    private class PubSubUpdatedChannel extends JedisPubSub implements Runnable {
        @Override
        public void run() {
            try {
                try (Jedis r = pool.getResource()) {
                    r.subscribe(this, kChannel());
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        @Override
        public void onMessage(String channel, String message) {
            for (PubSubUpdateListener listener : listeners) {
                listener.onMessage(message);
            }
        }
    }

    private String kChannel() {
        return redisDb + "/" + channelId;
    }

    public void subscribe(PubSubUpdateListener listener) {
        listeners.add(listener);

        synchronized (this) {
            if (redisListener == null) {
                pubsubThread = newSingleThreadScheduledExecutor(new DaemonThreadFactory(
                        PubSubRedisChannel.class.getSimpleName()));
                pubsubJob = pubsubThread.scheduleWithFixedDelay(redisListener, 0, 1000, TimeUnit.MILLISECONDS);
            }
        }
    }

    public void unsubscribe(PubSubUpdateListener listener) {
        listeners.remove(listener);
    }

    public void publish(String message) {
        withRedis(r -> r.publish(kChannel(), message));
    }

}
