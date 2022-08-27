package com.example.community.controller;

import com.example.community.annotation.LoginRequired;
import com.example.community.entity.Comment;
import com.example.community.entity.DiscussPost;
import com.example.community.entity.Event;
import com.example.community.entity.User;
import com.example.community.event.EventProducer;
import com.example.community.service.CommentService;
import com.example.community.service.DiscussPostService;
import com.example.community.service.UserService;
import com.example.community.util.CommunityContent;
import com.example.community.util.HostHodler;
import com.example.community.util.RedisKeyUtil;
import com.example.community.util.SensitiveWordFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.util.HtmlUtils;

import java.util.Date;

@Controller
@RequestMapping(path = "/comment")
public class CommentController implements CommunityContent {

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private HostHodler hostHodler;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @LoginRequired
    @RequestMapping(path = "/add/{postId}", method = RequestMethod.POST)
    public String addComment(@PathVariable("postId") int postId, Comment comment) {
        // 补全comment信息
        User user = hostHodler.getUser();
        comment.setUserId(user.getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());

        // 添加评论
        commentService.addComment(comment);

        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            // 触发发布帖子事件
            Event event = new Event()
                    .setTopic(TOPIC_PUBLISH)
                    .setEntityId(comment.getEntityId());
            eventProducer.fireEvent(event);
            // 增加需计算分数的帖子缓存
            String postChangeKey = RedisKeyUtil.getPostChangeKey();
            redisTemplate.opsForSet().add(postChangeKey, postId);
        }

        // 查询评论对象的所属用户
        User entityUser = null;
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            entityUser = userService.findUserById(target.getUserId());
        } else if (comment.getEntityType() == ENTITY_TYPE_COMMENT) {
            Comment target = commentService.findCommentById(comment.getEntityId());
            entityUser = userService.findUserById(target.getUserId());
        }

        if (entityUser != null && entityUser.getId() != user.getId()) {
            // 触发评论事件
            Event event = new Event()
                    .setTopic(TOPIC_COMMENT)
                    .setUserId(user.getId())
                    .setEntityType(comment.getEntityType())
                    .setEntityId(comment.getEntityId())
                    .setData("postId", postId)
                    .setEntityUserId(entityUser.getId());
            eventProducer.fireEvent(event);
        }

        return "redirect:/post/details/" + postId;
    }

}
