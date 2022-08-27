package com.example.community.service;

import com.example.community.util.CommunityContent;
import com.example.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class LikeService implements CommunityContent {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 点赞
    public void like(int userId, int entityType, int entityId, int entityUserId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                // 定义实体获赞和用户获赞的key
                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);

                // 查询userid是否已经对entity点赞
                boolean isLike = redisOperations.opsForSet().isMember(entityLikeKey, userId);

                // 开启事物
                redisOperations.multi();
                if (isLike) {
                    redisOperations.opsForSet().remove(entityLikeKey, userId);
                    redisOperations.opsForValue().decrement(userLikeKey);
                } else {
                    redisOperations.opsForSet().add(entityLikeKey, userId);
                    redisOperations.opsForValue().increment(userLikeKey);
                }
                return redisOperations.exec();
            }
        });
    }

    // 查询某实体的获赞数量
    public int findEntityLikeCount(int entityType, int entityId) {
        // 定义实体获赞的key
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().size(entityLikeKey).intValue();

    }

    // 查询某用户对某是实体的点赞状态
    public int findEntityLikeStatusByUserId(int userId, int entityType, int entityId) {
        // 定义实体获赞的key
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().isMember(entityLikeKey, userId) == true ? LIKE_STATUS_YES : LIKE_STATUS_NO;
    }

    // 查询某用户的获赞数
    public int findUserLikeCount(int userId) {
        // 定义某用户获赞的key
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        Integer count =  (Integer) redisTemplate.opsForValue().get(userLikeKey);
        return count == null ? 0 : count.intValue();

    }

}
