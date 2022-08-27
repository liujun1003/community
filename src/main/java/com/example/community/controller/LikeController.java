package com.example.community.controller;

import com.example.community.annotation.LoginRequired;
import com.example.community.entity.Event;
import com.example.community.entity.User;
import com.example.community.event.EventProducer;
import com.example.community.service.LikeService;
import com.example.community.util.CommunityContent;
import com.example.community.util.CommunityUtil;
import com.example.community.util.HostHodler;
import com.example.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LikeController implements CommunityContent {

    @Autowired
    private LikeService likeService;

    @Autowired
    private HostHodler hostHodler;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @LoginRequired
    @RequestMapping(path = "/like", method = RequestMethod.POST)
    @ResponseBody
    public String like(int entityType, int entityId, int entityUserId, int postId) {
        // 获取当前用户对象
        User user = hostHodler.getUser();

        // 执行点赞动作
        likeService.like(user.getId(), entityType, entityId, entityUserId);

        // 查询当前实体的点赞数、当前用户对于当前实体的点赞状态
        int likeCount = (int) likeService.findEntityLikeCount(entityType, entityId);
        int likeStatus = likeService.findEntityLikeStatusByUserId(user.getId(), entityType, entityId);

        if (likeStatus == LIKE_STATUS_YES && user.getId() != entityUserId) {
            // 触发点赞事件
            Event event = new Event()
                    .setTopic(TOPIC_LIKE)
                    .setUserId(user.getId())
                    .setEntityType(entityType)
                    .setEntityId(entityId)
                    .setEntityUserId(entityUserId)
                    .setData("postId", postId);
            eventProducer.fireEvent(event);
        }

        if (entityType == ENTITY_TYPE_POST) {
            // 增加需计算分数的帖子缓存
            String postChangeKey = RedisKeyUtil.getPostChangeKey();
            redisTemplate.opsForSet().add(postChangeKey, postId);
        }

        // 封装返回结果
        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);

        return CommunityUtil.getJSONString(0, null, map);
    }
}
