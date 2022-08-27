package com.example.community;

import com.example.community.service.TestService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class ThreadPoolTests {

    @Autowired
    private TestService testService;

    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolTests.class);

    // JDK普通线程池
    private ExecutorService executorService = Executors.newFixedThreadPool(5);
    // JDK可定时的线程池
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

    // Spring普通线程池
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;
    // Spring可定时的线程池
    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    private void sleep(long milliSecond) {
        try {
            Thread.sleep(milliSecond);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // 测试JDK的普通线程池
    @Test
    public void testExecutorService() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                logger.info("hello ExecutorService");
            }
        };

        for (int i = 0; i < 10; i++) {
            executorService.execute(runnable);
        }
        sleep(10000);
    }

    // 测试JDK的可定时线程池
    @Test
    public void testScheduledExecutorService() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                logger.info("hello ScheduledExecutorService");
            }
        };

        scheduledExecutorService.scheduleAtFixedRate(runnable, 0, 1, TimeUnit.SECONDS);
        sleep(10000);
    }

    // 测试Spring的普通线程池
    @Test
    public void testThreadPoolTaskExecutor() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                logger.info("hello ThreadPoolTaskExecutor");
            }
        };

        for (int i = 0; i < 10; i++) {
            taskExecutor.execute(runnable);
        }
        sleep(10000);
    }

    // 测试Spring的可定时线程池
    @Test
    public void testThreadPoolTaskScheduler() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                logger.info("hello ThreadPoolTaskScheduler");
            }
        };

        taskScheduler.scheduleAtFixedRate(runnable, 1000);
        sleep(10000);
    }

    // 测试Spring普通线程池的简单模式
    @Test
    public void testSimpleThreadPoolTaskExecutor() {
        for (int i = 0; i < 10; i++) {
            testService.simpleThreadPoolTaskExecutor();
        }

        sleep(10000);
    }

    // 测试Spring可定时线程池的简单模式
    @Test
    public void testSimpleThreadPoolTaskScheduler() {
        sleep(10000);
    }

}
