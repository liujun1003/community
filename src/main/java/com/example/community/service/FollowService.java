package com.example.community.service;

import com.example.community.entity.Comment;
import com.example.community.entity.DiscussPost;
import com.example.community.entity.User;
import com.example.community.util.CommunityContent;
import com.example.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService implements CommunityContent {

    @Autowired
    private UserService userService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private RedisTemplate redisTemplate;

    // 查询某个用户的关注实体数量
    public int findFolloweeCount(int userId, int entityType) {
        // 定义用户关注key
        String FolloweeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);

        return redisTemplate.opsForZSet().zCard(FolloweeKey).intValue();
    }

    // 查询某个实体的粉丝数量
    public int findFollowerCount(int entityType, int entityId) {
        // 定义实体粉丝key
        String FollowerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

        return redisTemplate.opsForZSet().zCard(FollowerKey).intValue();
    }

    // 查询某个用户对一实体的关注状态
    public boolean findFollowStatus(int userId, int entityType, int entityId) {
        // 定义用户关注的key
        String FolloweeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);

        return redisTemplate.opsForZSet().score(FolloweeKey, entityId) != null;
    }

    // 关注某个实体
    public void follow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                // 定义用户关注key和目标粉丝key
                String FolloweeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String FollowerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

                redisOperations.multi();
                // 执行关注
                redisOperations.opsForZSet().add(FolloweeKey, entityId, System.currentTimeMillis());
                redisOperations.opsForZSet().add(FollowerKey, userId, System.currentTimeMillis());

                return redisOperations.exec();
            }
        });
    }

    // 取关某个实体
    public void unfollow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                // 定义用户关注key和目标用户粉丝key
                String FolloweeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String FollowerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

                redisOperations.multi();
                // 执行取关
                redisOperations.opsForZSet().remove(FolloweeKey, entityId);
                redisOperations.opsForZSet().remove(FollowerKey, userId);

                return redisOperations.exec();
            }
        });
    }

    // 分页查询某个用户关注的实体
    public List<Map<String, Object>> findFollowees(int userId, int entityType, int offset, int limit) {
        // 定义用户关注的key
        String FolloweeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        Set<Integer> followeeIds = redisTemplate.opsForZSet().reverseRange(FolloweeKey, offset, offset + limit - 1);

        if (followeeIds == null) {
            return null;
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Integer followeeId : followeeIds) {
            Map<String, Object> map = new HashMap<>();
            // 实体
            if (entityType == ENTITY_TYPE_USER) {
                User entity = userService.findUserById(followeeId);
                map.put("entity", entity);
            } else if(entityType == ENTITY_TYPE_POST) {
                DiscussPost entity = discussPostService.findDiscussPostById(followeeId);
                map.put("entity", entity);
            } else if(entityType == ENTITY_TYPE_COMMENT) {
                Comment entity = commentService.findCommentById(followeeId);
                map.put("entity", entity);
            }
            // 关注时间
            Double score = redisTemplate.opsForZSet().score(FolloweeKey, followeeId);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);
        }
        return list;
    }

    // 分页查询某个实体的粉丝
    public List<Map<String, Object>> findFollowers(int entityType, int entityId, int offset, int limit) {
        // 定义实体粉丝的key
        String FollowerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        Set<Integer> followerIds = redisTemplate.opsForZSet().reverseRange(FollowerKey, offset, offset + limit - 1);

        if (followerIds == null) {
            return null;
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Integer followerId : followerIds) {
            Map<String, Object> map = new HashMap<>();
            // 用户
            User user = userService.findUserById(followerId);
            map.put("user", user);
            // 关注时间
            Double score = redisTemplate.opsForZSet().score(FollowerKey, followerId);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);
        }
        return list;
    }
}
