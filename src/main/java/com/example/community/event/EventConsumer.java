package com.example.community.event;

import com.alibaba.fastjson.JSONObject;
import com.example.community.entity.DiscussPost;
import com.example.community.entity.Event;
import com.example.community.entity.Message;
import com.example.community.service.DiscussPostService;
import com.example.community.service.ElasticsearchService;
import com.example.community.service.MessageService;
import com.example.community.util.CommunityContent;
import com.example.community.util.CommunityUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.Json;
import com.qiniu.util.StringMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

@Component
public class EventConsumer implements CommunityContent {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Value("${wk.command}")
    private String wkCommand;

    @Value("${wk.imagePath}")
    private String wkImagePath;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.share.name}")
    private String bucketShareName;

    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW})
    public void halderEvent(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息内容为空！");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误！");
            return;
        }

        // 发送站内通知
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        Map<String, Object> content = new HashMap<>();
        content.put("userId", event.getUserId());
        content.put("entityType", event.getEntityType());
        content.put("entityId", event.getEntityId());

        if (!event.getData().isEmpty()) {
            for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
                content.put(entry.getKey(), entry.getValue());
            }
        }
        message.setContent(JSONObject.toJSONString(content));

        messageService.addMessage(message);
    }

    @KafkaListener(topics = TOPIC_PUBLISH)
    public void halderPublishEvent(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息内容为空！");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误！");
            return;
        }

        // 将新的帖子存入elasticsearch
        DiscussPost discussPost = discussPostService.findDiscussPostById(event.getEntityId());
        elasticsearchService.saveDiscussPost(discussPost);
    }

    @KafkaListener(topics = TOPIC_DELETE)
    public void halderDeleteEvent(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息内容为空！");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误！");
            return;
        }

        // 将帖子从elasticsearch中删除
        elasticsearchService.deleteDiscussPostById(event.getEntityId());
    }

    @KafkaListener(topics = TOPIC_SHARE)
    public void halderShareEvent(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息内容为空！");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误！");
            return;
        }

        String htmlUrl = (String) event.getData().get("htmlUrl");
        String fileName = (String) event.getData().get("fileName");
        String suffix = (String) event.getData().get("suffix");

        String cmd = wkCommand + " --quality 75 " + htmlUrl + " " + wkImagePath + "/" + fileName + "." + suffix;

        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("生成长图成功！");
        } catch (IOException e) {
            logger.error("生成长图失败！" + e.getMessage());
        }

        UploadTask task = new UploadTask(fileName, suffix);
        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(task, 500);
        task.setFuture(future);
    }

    class UploadTask implements Runnable {

        // 生成的文件名
        private final String fileName;
        // 生成的文件后缀
        private final String suffix;
        // 启动任务的时间
        private long startTime;
        // 上传失败的次数
        private int uploadFailTimes;
        // 线程池启动任务的返回值
        private ScheduledFuture<?> future;

        public UploadTask(String fileName, String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime = System.currentTimeMillis();
        }

        public void setFuture(ScheduledFuture<?> future) {
            this.future = future;
        }

        @Override
        public void run() {
            String filePath = wkImagePath + "/" + fileName + "." + suffix;
            // 任务启动时间超过限定值，取消任务
            if (System.currentTimeMillis() - startTime > 30000) {
                logger.error("上传任务超时，任务取消:" + filePath);
                future.cancel(true);
                return;
            }
            // 上传次数超过限定值，取消任务
            if (uploadFailTimes > 3) {
                logger.error("上传任务失败次数过多，任务取消:" + filePath);
                future.cancel(true);
                return;
            }

            // 开始执行上传任务
            File file = new File(filePath);
            // 判断wk是否已经生成图片
            if (file.exists()) {
                logger.info(String.format("开始第%d次上传[%s]", ++uploadFailTimes, filePath));
                // 生成响应信息
                StringMap policy = new StringMap();
                policy.put("returnBody", CommunityUtil.getJSONString(0));
                // 生成上传凭证
                Auth auth = Auth.create(accessKey, secretKey);
                String uploadToken = auth.uploadToken(bucketShareName, fileName, 3600, policy);
                // 指定上传机房
                UploadManager uploadManager = new UploadManager(new Configuration(Zone.huanan()));
                try {
                    // 开始上传
                    Response response = uploadManager.put(filePath, fileName, uploadToken, null, "image/"+suffix, false);
                    // 获取并处理响应结果
                    JSONObject json = JSONObject.parseObject(response.bodyString());
                    if (json == null || json.get("code") == null || !json.get("code").toString().equals("0")) {
                        logger.error(String.format("第%d次上传[%s]失败！", uploadFailTimes, filePath));
                    } else {
                        logger.info(String.format("第%d次上传[%s]成功！", uploadFailTimes, filePath));
                        future.cancel(true);
                    }
                } catch (QiniuException e) {
                    logger.error(String.format("第%d次上传[%s]失败！", uploadFailTimes, filePath));
                }
            } else {
                logger.info("等待图片生成:" + filePath);
            }
        }
    }
}
