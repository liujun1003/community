package com.example.community.service;

import com.example.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class DataService {

    @Autowired
    private RedisTemplate redisTemplate;

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

    // 将ip记录到UV
    public void recordUV(String ip) {
        // 定义当前日期的RedisUVKey
        String redisUVKey = RedisKeyUtil.getUVKey(sdf.format(new Date()));
        // 将ip加入对应的Key中
        redisTemplate.opsForHyperLogLog().add(redisUVKey, ip);
    }

    // 统计指定日期范围内的UV值
    public long getUVPeriod(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        // 整理指定日期范围内的key
        List<String> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);

        while(!calendar.getTime().after(end)) {
            String redisUVKey = RedisKeyUtil.getUVKey(sdf.format(calendar.getTime()));
            keyList.add(redisUVKey);
            calendar.add(Calendar.DATE, 1);
        }

        String redisUVkeyPeriod = RedisKeyUtil.getUVKey(sdf.format(start), sdf.format(end));
        redisTemplate.opsForHyperLogLog().union(redisUVkeyPeriod, keyList.toArray());
        return redisTemplate.opsForHyperLogLog().size(redisUVkeyPeriod);
    }

    // 将userId记录到DAU
    public void recordDAU(int userId) {
        // 定义当前日期的RedisDAUKey
        String redisDAUKey = RedisKeyUtil.getDAUKey(sdf.format(new Date()));
        // 将ip加入对应的Key中
        redisTemplate.opsForValue().setBit(redisDAUKey, userId, true);
    }

    // 统计指定日期范围内的UV值
    public long getDAUPeriod(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        // 整理指定日期范围内的key
        List<byte[]> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);

        while(!calendar.getTime().after(end)) {
            String redisDAUKey = RedisKeyUtil.getDAUKey(sdf.format(calendar.getTime()));
            keyList.add(redisDAUKey.getBytes());
            calendar.add(Calendar.DATE, 1);
        }

        String redisDAUkeyPeriod = RedisKeyUtil.getDAUKey(sdf.format(start), sdf.format(end));
        return (long) redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                connection.bitOp(RedisStringCommands.BitOperation.OR,
                        redisDAUkeyPeriod.getBytes(), keyList.toArray(new byte[0][]));
                return connection.bitCount(redisDAUkeyPeriod.getBytes());
            }
        });
    }

}
