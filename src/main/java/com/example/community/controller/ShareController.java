package com.example.community.controller;

import com.example.community.entity.Event;
import com.example.community.event.EventProducer;
import com.example.community.util.CommunityContent;
import com.example.community.util.CommunityUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ShareController implements CommunityContent {

    private static final Logger logger = LoggerFactory.getLogger(ShareController.class);

    @Autowired
    private EventProducer eventProducer;

    @Value("${wk.imagePath}")
    private String wkImagePath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${qiniu.bucket.share.url}")
    private String bucketShareUrl;

    @RequestMapping(path = "/share", method = RequestMethod.GET)
    @ResponseBody
    public String getShare(String htmlUrl) {
        if (StringUtils.isBlank(htmlUrl)) {
            throw new IllegalArgumentException("参数不能为空！");
        }

        String fileName = CommunityUtil.generateUUID();
        String suffix = "png";

        Event event = new Event()
                .setTopic(TOPIC_SHARE)
                .setData("htmlUrl", htmlUrl)
                .setData("fileName", fileName)
                .setData("suffix", suffix);
        eventProducer.fireEvent(event);

        Map<String, Object> map = new HashMap<>();
//        map.put("shareUrl", domain + contextPath + "/share/image/" + fileName);
        map.put("shareUrl", bucketShareUrl + "/" + fileName);

        return CommunityUtil.getJSONString(0, null, map);
    }

    @Deprecated
    @RequestMapping(path = "/share/image/{fileName}", method = RequestMethod.GET)
    public void getShareImage(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        if (StringUtils.isBlank(fileName)) {
            throw new IllegalArgumentException("参数不能为空！");
        }

        String suffix = "png";
        response.setContentType("image/" + suffix);
        String filePath = wkImagePath + "/" + fileName + "." + suffix;

        try (
            FileInputStream fis = new FileInputStream(filePath);
            ServletOutputStream os = response.getOutputStream();
        ){
            IOUtils.copy(fis, os);
        } catch (IOException e) {
            logger.error("访问图片失败！" + e.getMessage());
        }
    }

}
