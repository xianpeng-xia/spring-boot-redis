package com.example.demo;

import jdk.nashorn.internal.ir.CallNode.EvalArgs;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
class Tests {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void t1() {
        redisTemplate.opsForValue().set("test1", "t1");
        String val = redisTemplate.opsForValue().get("test1");
        System.out.println(val);

    }

}
