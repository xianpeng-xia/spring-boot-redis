package com.example.demo.lock;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

/**
 * @author xianpeng.xia
 * on 2021/1/2 下午9:43
 */
@Service
public class RedisWithReentrantLock {

    private ThreadLocal<Map<String, Integer>> lockers = new ThreadLocal<>();

    private Jedis jedis;

    public RedisWithReentrantLock() {
        jedis = new Jedis();
    }

    private boolean _lock(String key, int secondsToExpire) {
        SetParams setParams = new SetParams();
        setParams.nx();
        setParams.ex(secondsToExpire);

        return jedis.set(key, "", setParams) != null;
    }

    private void _unlock(String key) {
        jedis.del(key);
    }

    private Map<String, Integer> currentLockers() {
        Map<String, Integer> refs = lockers.get();
        if (refs != null) {
            return refs;
        }
        lockers.set(new HashMap<>());
        return lockers.get();
    }

    public boolean lock(String key, int secondsToExpire) {
        Map<String, Integer> refs = currentLockers();
        Integer refCnt = refs.get(key);
        if (refCnt != null) {
            refs.put(key, refCnt + 1);
            return true;
        }

        boolean locked = this._lock(key, secondsToExpire);
        if (!locked) {
            return false;
        }
        refs.put(key, 1);
        return true;
    }

    public boolean unlock(String key) {
        Map<String, Integer> refs = currentLockers();
        Integer refCnt = refs.get(key);
        if (refCnt == null) {
            return false;
        }

        refCnt -= 1;

        if (refCnt > 0) {
            refs.put(key, refCnt);
        } else {
            refs.remove(key);
            this._unlock(key);
        }
        return true;
    }

    public static void main(String[] args) {
        RedisWithReentrantLock redis = new RedisWithReentrantLock();

        System.out.println(redis.lock("lock",10));
        System.out.println(redis.lock("lock",10));

        System.out.println(redis.unlock("lock"));
        System.out.println(redis.unlock("lock"));
    }
}
