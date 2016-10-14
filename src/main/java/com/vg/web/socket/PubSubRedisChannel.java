package com.vg.web.socket;

import static com.github.davidmoten.rx.RetryWhen.delay;
import static java.util.concurrent.TimeUnit.SECONDS;
import static rx.schedulers.Schedulers.newThread;

import com.vg.web.db.RedisDao;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import rx.Observable;
import rx.subscriptions.Subscriptions;

public class PubSubRedisChannel extends RedisDao {
    private final long redisDb;
    private final String channelId;

    public PubSubRedisChannel(JedisPool pool, String channelId) {
        super(pool);
        this.channelId = channelId;
        this.redisDb = getDb();
    }

    private String kChannel() {
        return redisDb + "/" + channelId;
    }

    private static void debug(String string) {
        //System.out.println(Thread.currentThread().getName() + " " + string);
    }

    public Observable<String> messages() {
        String channel = kChannel();
        return Observable.create(o -> {
            debug("JedisPool.getResource");
            Jedis r = pool.getResource();
            debug("JedisPool.getResource ok");
            try {
                JedisPubSub jedisPubSub = new JedisPubSub() {
                    @Override
                    public void onMessage(String ch, String message) {
                        o.onNext(message);
                    }
                };
                o.add(Subscriptions.create(() -> {
                    debug("unsubscribe " + channel);
                    try {
                        jedisPubSub.unsubscribe();
                        debug("unsubscribed " + channel);
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        System.err.println(e);
                    }
                }));

                debug("subscribe " + channel);
                r.subscribe(jedisPubSub, channel);
                debug("subscribe " + channel + " end");
            } finally {
                debug("Jedis.close");
                r.close();
                debug("Jedis.closed");
            }
        });
    }
    
    public Observable<String> messagesOnNewThread() {
        return messages()
                .subscribeOn(newThread())
                .timeout(30, SECONDS)
                .retryWhen(delay(1, SECONDS).build())
                .onBackpressureBuffer();
    }
    

    public void publish(String message) {
        withRedis(r -> r.publish(kChannel(), message));
    }

}
