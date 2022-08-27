package com.example.community.dao;

import com.example.community.entity.DiscussPost;
import com.example.community.entity.LoginTicket;
import com.example.community.entity.Message;
import com.example.community.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.List;

@SpringBootTest
public class MapperTests {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Test
    public void testUserMapper() {
        User user = userMapper.selectUserById(1);
        System.out.println(user);

        user = userMapper.selectUserByName("SYSTEM");
        System.out.println(user);

        user = userMapper.selectUserByEmail("nowcoder1@sina.com");
        System.out.println(user);

        userMapper.insertUser(user);

        userMapper.updatePassword(user.getId(), "111");
        userMapper.updateType(user.getId(), 1);
        userMapper.updateStatus(user.getId(), 2);
        userMapper.updateHeaderUrl(user.getId(), "1");

        userMapper.deleteUserById(user.getId());
    }

    @Test
    public void testDiscussPostMapper() {
//        List<DiscussPost> list = discussPostMapper.selectDiscussPosts(0, 1, 10, 0);
//        for (DiscussPost post : list) {
//            System.out.println(post);
//        }
//
//        System.out.println(discussPostMapper.selectDiscussPostRows(0));
        discussPostMapper.updateScore(289, 1);
    }

    @Test
    public void testLoginTicketMapper() {
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(1);
        loginTicket.setTicket("dfsfsf");
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + 60 * 10));
        System.out.println(loginTicket);

        System.out.println(loginTicketMapper.insertLoginTicket(loginTicket));

        loginTicketMapper.updateStatusByTicket(loginTicket.getTicket(), 1);

        System.out.println(loginTicketMapper.selectLoginTicketByTicket(loginTicket.getTicket()));
    }

    @Test
    public void testMessageMapper() {
        List<Message> conversations = messageMapper.selectConversations(111, 0, 20);
        for (Message conversation : conversations) {
            System.out.println(conversation);
        }

        System.out.println(messageMapper.selectConversationCount(111));

        List<Message> letters = messageMapper.selectLetters("111_113", 0, 20);
        for (Message letter : letters) {
            System.out.println(letter);
        }

        System.out.println(messageMapper.selectLetterCount("111_113"));

        System.out.println(messageMapper.selectLetterUnreadCount(111, null));
    }
}
