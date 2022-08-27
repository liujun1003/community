package com.example.community.util;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CommunityUtilTests {
    @Test
    public void testCommunityUtil() {
        String key = CommunityUtil.generateUUID();
        System.out.println(key);

        System.out.println(CommunityUtil.md5(key));
    }

}
