package com.example.community.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TestService {

    private static final Logger logger = LoggerFactory.getLogger(TestService.class);

    /**
     * Spring线程池的普通任务和定时任务的简单定义形式
     */
    // 加上Async注解后，表示该方法可以被异步调用
    @Async
    public void simpleThreadPoolTaskExecutor() {
        logger.info("hello simpleThreadPoolTaskExecutor");
    }

    // 加上Scheduled注解后，表示该方法可以按设定的周期自动执行
//    @Scheduled(fixedRate = 1000)
    public void simpleThreadPoolTaskScheduling() {
        logger.info("hello simpleThreadPoolTaskScheduler");
    }
}
