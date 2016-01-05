package com.vg.web.db;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

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

    protected void withRedisTransaction(Consumer<Transaction> r) {
        withRedisTransactionOnOk(r, (Runnable) null);
    }

    protected void withRedisTransaction(Jedis redis, Consumer<Transaction> tx) {
        Transaction transaction = null;
        try {
            transaction = redis.multi();
            tx.accept(transaction);
            transaction.exec();
            transaction = null;
        } finally {
            rollback(transaction);
        }
    }

    protected void withRedisTransactionOnOk(Consumer<Transaction> r, Runnable onOk) {
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

    protected void withRedisTransactionOnOk(Consumer<Transaction> r, Consumer<Jedis> onOk) {
        Jedis redis = getRedis();
        Transaction transaction = null;
        try {
            transaction = redis.multi();
            r.accept(transaction);
            transaction.exec();
            transaction = null;
            if (onOk != null) {
                onOk.accept(redis);
            }
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
