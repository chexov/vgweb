package com.vg.web.db;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.Transaction;
import rx.Observable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class RedisDao {
    protected final JedisPool pool;

    public RedisDao(JedisPool pool) {
        this.pool = pool;
    }

    protected long getDb() {
        return withRedis(r -> r.getClient().getDB());
    }

    protected <T> T withRedis(Function<Jedis, T> r) {
        Jedis redis = getRedis();
        try {
            return r.apply(redis);
        } finally {
            redis.close();
        }
    }

    protected void updateRedis(Consumer<Jedis> r) {
        Jedis redis = getRedis();
        try {
            r.accept(redis);
        } finally {
            redis.close();
        }
    }

    protected Jedis getRedis() {
        return pool.getResource();
    }

    protected List<Object> withRedisTransaction(Consumer<Transaction> r) {
        return withRedisTransactionOnOk(r, (Runnable) null);
    }

    public void withRedisTransaction(Consumer<Transaction> r, Runnable onOk) {
        Jedis redis = getRedis();
        Transaction transaction = null;
        try {
            transaction = redis.multi();
            r.accept(transaction);
            transaction.exec();
            transaction = null;
            if (onOk != null) {
                onOk.run();
            }
        } finally {
            rollback(transaction);
            redis.close();
        }
    }

    public static Observable<List<String>> scanKeys(Jedis r, String pattern) {
        Observable<List<String>> scanKeys = Observable.create(o -> {
            String cursor = "0";
            do {
                ScanParams params = new ScanParams().match(pattern);
                ScanResult<String> scan = r.scan(cursor, params);
                cursor = scan.getStringCursor();
                List<String> keys = scan.getResult();
                o.onNext(keys);
            } while (!"0".equals(cursor) && !o.isUnsubscribed());
            o.onCompleted();
        });
        return scanKeys.onBackpressureBuffer();
    }

    protected List<Object> withRedisTransaction(Jedis redis, Consumer<Transaction> tx) {
        Transaction transaction = null;
        try {
            transaction = redis.multi();
            tx.accept(transaction);
            List<Object> exec = transaction.exec();
            transaction = null;
            return exec;
        } finally {
            rollback(transaction);
        }
    }

    protected List<Object> withRedisTransactionOnOk(Consumer<Transaction> r, Runnable onOk) {
        Jedis redis = getRedis();
        Transaction transaction = null;
        try {
            transaction = redis.multi();
            r.accept(transaction);
            List<Object> exec = transaction.exec();
            transaction = null;
            if (exec != null && onOk != null) {
                onOk.run();
            }
            return exec;
        } finally {
            rollback(transaction);
            redis.close();
        }
    }

    protected List<Object> withRedisTransactionOnOk(Consumer<Transaction> r, Consumer<Jedis> onOk) {
        Jedis redis = getRedis();
        Transaction transaction = null;
        try {
            transaction = redis.multi();
            r.accept(transaction);
            List<Object> exec = transaction.exec();
            transaction = null;
            if (exec != null && onOk != null) {
                onOk.accept(redis);
            }
            return exec;
        } finally {
            rollback(transaction);
            redis.close();
        }
    }

    private void rollback(Transaction transaction) {
        if (transaction != null) {
            transaction.discard();
        }
    }

}
