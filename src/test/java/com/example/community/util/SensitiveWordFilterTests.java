package com.example.community.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SensitiveWordFilterTests {
    @Autowired
    private SensitiveWordFilter sensitiveWordFilter;

    @Test
    public void testFilterSensitiveWord() {
        String text = "hhhh赌，，，博， 吸fdsafd毒f附近嫖,.,娼打发时间哈酒开发商的开^&票，，，，，";
        System.out.println(sensitiveWordFilter.filterSensitiveWord(text));
    }
}
