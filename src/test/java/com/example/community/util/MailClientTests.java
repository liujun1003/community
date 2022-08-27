package com.example.community.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@SpringBootTest
public class MailClientTests {
    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Test
    public void testSendMail() {
        mailClient.sendMessage("2112005260@mail2.gdut.edu.cn", "Test", "from qq mail of liujun!");
    }

    @Test
    public void testSendMailTemplate() {
        Context context = new Context();
        context.setVariable("username", "liujungdut");
        String content = templateEngine.process("/mail/demo", context);

        mailClient.sendMessage("2112005260@mail2.gdut.edu.cn", "Test", content);
    }
}
