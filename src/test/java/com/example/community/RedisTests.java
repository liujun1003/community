package com.example.community;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
public class RedisTests {

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void testHyperLoglog() {
        String redisKeyhll1 = "test:hll:01";
        String redisKeyhll2 = "test:hll:02";
        String redisKeyhllUnion = "test:hll:union";

        for (int i = 1; i <= 1000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKeyhll1, i);
        }
        System.out.println("redisTemplate.opsForHyperLogLog().size(redisKeyhll1) = " + redisTemplate.opsForHyperLogLog().size(redisKeyhll1));

        for (int i = 500; i <= 1500; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKeyhll2, i);
        }
        System.out.println("redisTemplate.opsForHyperLogLog().size(redisKeyhll2) = " + redisTemplate.opsForHyperLogLog().size(redisKeyhll2));

        redisTemplate.opsForHyperLogLog().union(redisKeyhllUnion, redisKeyhll1, redisKeyhll2);
        System.out.println("redisTemplate.opsForHyperLogLog().size(redisKeyhllUnion) = " + redisTemplate.opsForHyperLogLog().size(redisKeyhllUnion));
    }

    @Test
    public void testBitMap() {
        String redisKeybm1 = "test:bm:01";
        String redisKeybm2 = "test:bm:02";
        String redisKeybmOR = "test:bm:or";

        redisTemplate.opsForValue().setBit(redisKeybm1, 0, true);
        redisTemplate.opsForValue().setBit(redisKeybm1, 1, true);
        redisTemplate.opsForValue().setBit(redisKeybm1, 2, true);
        System.out.println(redisTemplate.opsForValue().getBit(redisKeybm1, 0));
        System.out.println(redisTemplate.opsForValue().getBit(redisKeybm1, 1));
        System.out.println(redisTemplate.opsForValue().getBit(redisKeybm1, 2));

        redisTemplate.opsForValue().setBit(redisKeybm2, 2, true);
        redisTemplate.opsForValue().setBit(redisKeybm2, 3, true);
        redisTemplate.opsForValue().setBit(redisKeybm2, 4, true);
        System.out.println(redisTemplate.opsForValue().getBit(redisKeybm2, 0));
        System.out.println(redisTemplate.opsForValue().getBit(redisKeybm2, 1));
        System.out.println(redisTemplate.opsForValue().getBit(redisKeybm2, 2));

        redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                System.out.println("connection.bitCount(redisKeybm1.getBytes()) = " + connection.bitCount(redisKeybm1.getBytes()));
                System.out.println("connection.bitCount(redisKeybm2.getBytes()) = " + connection.bitCount(redisKeybm2.getBytes()));

                connection.bitOp(RedisStringCommands.BitOperation.OR, redisKeybmOR.getBytes(), redisKeybm1.getBytes(), redisKeybm2.getBytes());
                System.out.println("connection.bitCount(redisKeybmOR.getBytes()) = " + connection.bitCount(redisKeybmOR.getBytes()));
                return null;
            }
        });



    }
}
