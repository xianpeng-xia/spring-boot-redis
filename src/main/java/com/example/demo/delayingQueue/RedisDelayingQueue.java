package com.example.demo.delayingQueue;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import redis.clients.jedis.Jedis;

/**
 * @author xianpeng.xia
 * on 2021/1/2 下午10:34
 */
public class RedisDelayingQueue<T> {

    static class TaskItem<T> {

        public String id;
        public T msg;
        public Long scheduledTime;
    }

    private Type taskType = new TypeReference<TaskItem<T>>() {
    }.getType();

    private Jedis jedis;
    private String queueKey;

    public RedisDelayingQueue(Jedis jedis, String queueKey) {
        this.jedis = jedis;
        this.queueKey = queueKey;
    }

    public void delay(T msg, int secondToDelay) {
        Long scheduledTime = System.currentTimeMillis() + secondToDelay * 1000;
        TaskItem<T> task = new TaskItem<>();
        task.id = UUID.randomUUID().toString();
        task.msg = msg;
        task.scheduledTime = scheduledTime;

        String s = JSON.toJSONString(task);

        System.out.println("msg: " + s + " delay time: " + scheduledTime);
        jedis.zadd(queueKey, scheduledTime, s);
    }

    public void loop() {
        while (!Thread.interrupted()) {
            long now = System.currentTimeMillis();
            Set<String> values = jedis.zrangeByScore(queueKey, 0, now, 0, 1);
            // 如果空,sleep500ms继续
            if (values.isEmpty()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                }
                continue;
            }

            String s = values.iterator().next();
            TaskItem<T> taskItem = JSON.parseObject(s, taskType);
            Long scheduledTime = taskItem.scheduledTime;

            if (now >= scheduledTime && jedis.zrem(queueKey, s) > 0) {
                this.handleMsg(taskItem.msg);
            }
        }
    }

    public void handleMsg(T msg) {
        System.out.println("msg: " + msg + " run time: " + System.currentTimeMillis());
    }

    public static void main(String[] args) {
        Jedis jedis = new Jedis();
        RedisDelayingQueue<String> delayingQueue = new RedisDelayingQueue<>(jedis, "delayingQueue");

        Thread producer = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                delayingQueue.delay("msg " + i, i);
            }
        });

        Thread consumer = new Thread(() -> {
            delayingQueue.loop();
        });

        producer.start();
        consumer.start();

        try {
            producer.join();
            Thread.sleep(15 * 1000);
            consumer.interrupt();
            consumer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}