package com.vg.web.socket;

import com.vg.web.util.WebUtils;
import com.vg.web.db.RedisDao;
import com.vg.web.util.DaemonThreadFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class PubSubRedisChannel extends RedisDao {
    //    public static final Logger log = LogManager.getLogger(PubSubRedisChannel.class);
    private final long redisDb;
    private final String channelId;
    PubSubUpdatedChannel pubsub = null;
    ExecutorService pubsubThread;
    Future<?> pubsubJob;

    public PubSubRedisChannel(JedisPool pool, String channelId) {
        super(pool);
        this.channelId = channelId;
        this.redisDb = getDb();
    }

    class PubSubUpdatedChannel extends JedisPubSub implements Runnable {
        private Jedis r;

        public PubSubUpdatedChannel(Jedis r) {
            this.r = r;
        }

        @Override
        public void run() {
            try {
                r.subscribe(this, kChannel());
            } catch (Throwable e) {
                e.printStackTrace();
                WebUtils.rethrow(e);
            }
        }

        List<PubSubUpdateListener> listeners = new CopyOnWriteArrayList<>();

        public void addListener(PubSubUpdateListener listener) {
            listeners.add(listener);
        }

        public void removeListener(PubSubUpdateListener listener) {
            listeners.remove(listener);
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
        synchronized (this) {
            if (pubsub == null) {
                pubsubThread = newSingleThreadExecutor(new DaemonThreadFactory(
                        PubSubRedisChannel.class.getSimpleName()));
                pubsub = new PubSubUpdatedChannel(getRedis());
                pubsubJob = pubsubThread.submit(pubsub);

                try {
                    // this is to give Future time to warm up.
                    // quick hack to make tests pass on kote@
                    //                    log.warn("warming up pubsubThread...");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    WebUtils.rethrow(e);
                }

            }
            pubsub.addListener(listener);
            //            log.debug(kChannel() + ". subscribed " + listener);
        }
    }

    public void unsubscribe(PubSubUpdateListener listener) {
        synchronized (this) {
            if (pubsub != null) {
                pubsub.removeListener(listener);
                //                log.debug(kChannel() + ". UNsubscribed " + listener);
            }
        }
    }

    public void publish(String message) {
        //        log.debug(kChannel() + ". publishing message " + message);
        withRedis(r -> r.publish(kChannel(), message));
    }

}
