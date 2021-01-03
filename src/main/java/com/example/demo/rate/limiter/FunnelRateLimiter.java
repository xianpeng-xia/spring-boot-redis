package com.example.demo.rate.limiter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xianpeng.xia
 * on 2021/1/3 下午12:11
 */
public class FunnelRateLimiter {

    static class Funnel {

        int capacity;
        float leakingRate;
        int leftQuota;
        long leakingTs;

        public Funnel(int capacity, float leakingRate) {
            this.capacity = capacity;
            this.leakingRate = leakingRate;
        }

        void makeSpace() {
            long nowTs = System.currentTimeMillis();
            long deltaTs = nowTs - leakingTs;

            int deltaQuota = (int) (deltaTs * leakingRate);
            if (deltaQuota < 0) {
                this.leftQuota = capacity;
                this.leakingTs = nowTs;
                return;
            }

            if (deltaQuota < 1) {
                return;
            }

            this.leftQuota += deltaQuota;
            this.leakingTs = nowTs;

            if (this.leftQuota > this.capacity) {
                this.leftQuota = this.capacity;
            }
        }

        boolean watering(int quota) {
            makeSpace();

            if (this.leftQuota >= quota) {
                this.leftQuota -= quota;
                return true;
            }

            return false;
        }
    }

    private Map<String, Funnel> funnels = new HashMap<>();

    public boolean isActionAllowed(String userId, String actionKey, int capacity, float leakingRate) {
        String key = String.format("%s:%s", userId, actionKey);
        Funnel funnel = funnels.get(key);

        if (funnel == null) {
            funnel = new Funnel(capacity, leakingRate);
            funnels.put(key, funnel);
        }

        return funnel.watering(1);
    }
}
