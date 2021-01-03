package com.example.demo.transaction;

import java.util.List;
import redis.clients.jedis.Jedis;

/**
 * @author xianpeng.xia
 * on 2021/1/3 下午8:18
 */
public class RedisTransaction {

    public static int doubleAccount(Jedis jedis, String userId) {
        String key = keyFor(userId);

        while (true) {
            jedis.watch(key);
            Integer balance = Integer.parseInt(jedis.get(key));
            balance *= 2;

            redis.clients.jedis.Transaction transaction = jedis.multi();
            transaction.set(key, String.valueOf(balance));

            List<Object> res = transaction.exec();
            if (res != null) {
                // 成功
                break;
            }
        }
        return Integer.parseInt(jedis.get(key));
    }

    public static String keyFor(String userId) {
        return String.format("account:%s", userId);
    }

    public static void main(String[] args) {
        Jedis jedis = new Jedis();
        String userId = "a";
        String key = keyFor(userId);
        jedis.setnx(key, String.valueOf(100));
        System.out.println(doubleAccount(jedis, userId));
        jedis.close();
    }
}
