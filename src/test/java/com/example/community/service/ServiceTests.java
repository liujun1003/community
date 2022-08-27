package com.example.community.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ServiceTests {

    @Autowired
    private LikeService likeService;

    @Autowired
    private DiscussPostService discussPostService;

    @Test
    public void testLikeService() {
        likeService.like(111, 1, 1, 222);

        System.out.println(likeService.findEntityLikeStatusByUserId(111, 1, 1));

        System.out.println(likeService.findEntityLikeCount(1, 1));

        System.out.println(likeService.findUserLikeCount(222));
    }

    @Test
    public void testDiscussPostService() {
        discussPostService.modifyScore(289, 2.3);
    }
}
