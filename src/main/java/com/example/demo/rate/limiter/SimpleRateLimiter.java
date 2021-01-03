package com.example.demo.rate.limiter;

import javax.xml.transform.Source;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

/**
 * @author xianpeng.xia
 * on 2021/1/3 上午11:42
 */
public class SimpleRateLimiter {

    private Jedis jedis;

    public SimpleRateLimiter(Jedis jedis) {
        this.jedis = jedis;
    }

    public boolean isActionAllowed(String userId, String actionKey, int period, int maxCount) {
        String key = String.format("hist:%s:%s", userId, actionKey);
        long now = System.currentTimeMillis();

        Pipeline pipeline = jedis.pipelined();

        pipeline.multi();
        pipeline.zadd(key, now, now + "");
        pipeline.zremrangeByScore(key, 0, now - period * 1000);
        Response<Long> count = pipeline.zcard(key);
        pipeline.expire(key, period + 1);
        pipeline.exec();
        pipeline.close();
        //System.out.println("Count: " + count.get() + " Max count: " + maxCount);
        return count.get() <= maxCount;
    }

    public static void main(String[] args) {
        Jedis jedis = new Jedis();
        SimpleRateLimiter simpleRateLimiter = new SimpleRateLimiter(jedis);

        for (int i = 0; i < 20; i++) {
            System.out.println("time: " + System.currentTimeMillis() + " " + i + " " + simpleRateLimiter.isActionAllowed("A", "reply", 60, 5));
        }
    }
}
