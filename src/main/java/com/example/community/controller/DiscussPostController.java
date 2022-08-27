package com.example.community.controller;

import com.example.community.entity.*;
import com.example.community.event.EventProducer;
import com.example.community.service.CommentService;
import com.example.community.service.DiscussPostService;
import com.example.community.service.LikeService;
import com.example.community.service.UserService;
import com.example.community.util.CommunityContent;
import com.example.community.util.CommunityUtil;
import com.example.community.util.HostHodler;
import com.example.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@RequestMapping(path = "/post")
public class DiscussPostController implements CommunityContent {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private HostHodler hostHodler;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;


    @RequestMapping(path = "/add", method = RequestMethod.POST)
    @ResponseBody
    public String addDiscussPost(String title, String content) {
        User user = hostHodler.getUser();
        if (user == null) {
            return CommunityUtil.getJSONString(403, "您还没有登录！");
        }

        DiscussPost discussPost = new DiscussPost();
        discussPost.setUserId(user.getId());
        discussPost.setTitle(title);
        discussPost.setContent(content);
        discussPost.setCreateTime(new Date());
        discussPostService.addDiscussPost(discussPost);

        // 触发发布帖子事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setEntityId(discussPost.getId());
        eventProducer.fireEvent(event);

        // 增加需计算分数的帖子缓存
        String postChangeKey = RedisKeyUtil.getPostChangeKey();
        redisTemplate.opsForSet().add(postChangeKey, discussPost.getId());

        return CommunityUtil.getJSONString(0, "帖子发布成功！");
    }

    @RequestMapping(path = "/details/{postId}", method = RequestMethod.GET)
    public String getDetails(@PathVariable("postId") int postId, Page page, Model model) {
        User user = hostHodler.getUser();
        // 帖子
        DiscussPost discussPost = discussPostService.findDiscussPostById(postId);
        model.addAttribute("post", discussPost);

        // 作者
        User postUser = userService.findUserById(discussPost.getUserId());
        model.addAttribute("postUser", postUser);

        // 点赞数
        int liekCount = (int) likeService.findEntityLikeCount(ENTITY_TYPE_POST, postId);
        model.addAttribute("liekCount", liekCount);

        // 点赞状态
        int likeStatus = user == null ? 0 : likeService.findEntityLikeStatusByUserId(user.getId(), ENTITY_TYPE_POST, discussPost.getId());
        model.addAttribute("likeStatus", likeStatus);

        // 分页对象初始化
        page.setPath("/post/details/" + postId);
        page.setLimit(5);
        page.setRows(commentService.findCommentRowsByEntity(ENTITY_TYPE_POST, postId));
        // 评论列表
        List<Comment> commentList = commentService.findCommentByEntity(ENTITY_TYPE_POST, postId, page.getOffset(), page.getLimit());
        // 评论视图对象列表
        List<Map<String, Object>> cvoList = new ArrayList<>();
        if (commentList != null) {
            for (Comment comment : commentList) {
                Map<String, Object> cvo = new HashMap<>();
                // 评论
                cvo.put("comment", comment);
                // 评论作者
                User commentUser = userService.findUserById(comment.getUserId());
                cvo.put("commentUser", commentUser);
                // 评论点赞数
                liekCount = (int) likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
                cvo.put("likeCount", liekCount);
                // 评论点赞状态
                likeStatus = user == null ? 0 : likeService.findEntityLikeStatusByUserId(user.getId(), ENTITY_TYPE_COMMENT, comment.getId());
                cvo.put("likeStatus", likeStatus);
                // 回复列表
                List<Comment> replyList = commentService.findCommentByEntity(ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);
                // 回复视图对象列表
                List<Map<String, Object>> rvoList = new ArrayList<>();
                if (replyList != null) {
                    for (Comment reply : replyList) {
                        Map<String, Object> rvo = new HashMap<>();
                        // 回复
                        rvo.put("reply", reply);
                        // 回复作者
                        User replyUser = userService.findUserById(reply.getUserId());
                        rvo.put("replyUser", replyUser);
                        // 回复对象
                        User targetUser = userService.findUserById(reply.getTargetId());
                        rvo.put("targetUser", targetUser);
                        // 回复点赞数
                        liekCount = (int) likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                        rvo.put("likeCount", liekCount);
                        // 回复点赞状态
                        likeStatus = user == null ? 0 : likeService.findEntityLikeStatusByUserId(user.getId(), ENTITY_TYPE_COMMENT, reply.getId());
                        rvo.put("likeStatus", likeStatus);

                        // 将回复视图对象添加到rvoList
                        rvoList.add(rvo);
                    }
                }
                cvo.put("rvoList", rvoList);
                // 回复数量
                cvo.put("replyCount", commentService.findCommentRowsByEntity(ENTITY_TYPE_COMMENT, comment.getId()));

                // 将评论视图对象添加到cvoList
                cvoList.add(cvo);
            }
        }
        model.addAttribute("cvoList", cvoList);

        return "/site/discuss-detail";
    }

    @RequestMapping(path = "/top", method = RequestMethod.POST)
    @ResponseBody
    public String setTop(int postId) {
        // 查询帖子是否存在
        DiscussPost post = discussPostService.findDiscussPostById(postId);
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在！");
        }
        // 帖子置顶
        discussPostService.modifyType(postId, POST_TYPE_TOP);

        // 帖子type改变，触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setEntityId(postId);

        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0);
    }

    @RequestMapping(path = "/essence", method = RequestMethod.POST)
    @ResponseBody
    public String setEssence(int postId) {
        // 查询帖子是否存在
        DiscussPost post = discussPostService.findDiscussPostById(postId);
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在！");
        }
        // 帖子加精
        discussPostService.modifyStatus(postId, POST_STATUS_ESSENCE);

        // 帖子type改变，触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setEntityId(postId);
        eventProducer.fireEvent(event);

        // 增加需计算分数的帖子缓存
        String postChangeKey = RedisKeyUtil.getPostChangeKey();
        redisTemplate.opsForSet().add(postChangeKey, postId);

        return CommunityUtil.getJSONString(0);
    }

    @RequestMapping(path = "/delete", method = RequestMethod.POST)
    @ResponseBody
    public String setDelete(int postId) {
        // 查询帖子是否存在
        DiscussPost post = discussPostService.findDiscussPostById(postId);
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在！");
        }
        // 帖子加精
        discussPostService.modifyStatus(postId, POST_STATUS_DELETE);

        // 帖子type改变，触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_DELETE)
                .setEntityId(postId);

        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0);
    }

}
