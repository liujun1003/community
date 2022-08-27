package com.example.community.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;

@Configuration
public class WkConfig {

    private static final Logger logger = LoggerFactory.getLogger(WkConfig.class);

    @Value("${wk.imagePath}")
    private String wkImagePath;

    @PostConstruct
    public void init() {
        File file = new File(wkImagePath);

        if (!file.exists()) {
            file.mkdirs();
            logger.info("创建wk图片目录！" + wkImagePath);
        }
    }
}
