package com.example.community.quartz;

import com.example.community.entity.DiscussPost;
import com.example.community.service.DiscussPostService;
import com.example.community.service.ElasticsearchService;
import com.example.community.service.LikeService;
import com.example.community.util.CommunityContent;
import com.example.community.util.RedisKeyUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PostScoreRefreshJob implements Job, CommunityContent {

    private static final Logger logger = LoggerFactory.getLogger(PostScoreRefreshJob.class);

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    private static final Date epoch;

    static {
        try {
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2014-08-01 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("初始化牛客纪元失败！" + e);
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String postChangeKey = RedisKeyUtil.getPostChangeKey();
        BoundSetOperations operations = redisTemplate.boundSetOps(postChangeKey);

        if (operations.size() == 0) {
            logger.info("[任务取消：帖子分数刷新] 没有需要刷新的帖子");
            return;
        }

        logger.info("[任务开始：帖子分数刷新] 需要刷新帖子分数：" + operations.size());
        while (operations.size() > 0) {
            refresh((Integer) operations.pop());
        }
        logger.info("[任务开始：帖子分数刷新] 刷新帖子分数完毕！");
    }

    // 刷新帖子分数
    // 帖子分数计算公式 score = log(精华分 + 评论数*10 + 点赞数*2 + 收藏数*2) + (发布时间 – 牛客纪元)
    private void refresh(int postId) {
        DiscussPost discussPost = discussPostService.findDiscussPostById(postId);

        // 加精
        boolean isEssence = discussPost.getStatus() == POST_STATUS_ESSENCE;

        // 评论数
        int commentCount = discussPost.getCommentCount();

        // 点赞数
        int likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPost.getId());

        // 权重
        double weight = (isEssence ? 75 : 0) + commentCount * 10 + likeCount * 2;
        // 分数
        double score = Math.log10(Math.max(weight, 1)) + ((discussPost.getCreateTime().getTime() - epoch.getTime()) / (1000 * 3600 * 24));

        // 更新帖子分数
        discussPostService.modifyScore(postId, score);
        // 更新elasticsearch数据
        discussPost.setScore(score);
        elasticsearchService.saveDiscussPost(discussPost);
    }
}
