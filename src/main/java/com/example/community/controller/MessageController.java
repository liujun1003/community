package com.example.community.controller;

import com.alibaba.fastjson.JSONObject;
import com.example.community.annotation.LoginRequired;
import com.example.community.entity.Message;
import com.example.community.entity.Page;
import com.example.community.entity.User;
import com.example.community.service.MessageService;
import com.example.community.service.UserService;
import com.example.community.util.CommunityContent;
import com.example.community.util.CommunityUtil;
import com.example.community.util.HostHodler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Controller
public class MessageController implements CommunityContent {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHodler hostHodler;

    @LoginRequired
    @RequestMapping(path = "/letter/list", method = RequestMethod.GET)
    public String getLetterList(Model model, Page page) {
//        Integer.parseInt("fdsaf");
        User user = hostHodler.getUser();

        // 设置分页工具条
        page.setLimit(5);
        page.setRows(messageService.findConversationCount(user.getId()));
        page.setPath("/letter/list");

        // 会话列表
        List<Message> conversationList = messageService.findConversations(user.getId(), page.getOffset(), page.getLimit());
        List<Map<String, Object>> conversations = new ArrayList<>();
        if (conversationList != null) {
            for (Message conversation : conversationList) {
                Map<String, Object> map = new HashMap<>();

                // 会话最近的一条消息
                map.put("conversation", conversation);
                // 会话的对话方
                int showUserId = user.getId() == conversation.getFromId() ? conversation.getToId() : conversation.getFromId();
                User showUser = userService.findUserById(showUserId);
                map.put("showUser", showUser);
                // 会话未读消息数
                int unreadLetterCount = messageService.findLetterUnreadCount(user.getId(), conversation.getConversationId());
                map.put("unreadLetterCount", unreadLetterCount);
                // 会话消息数
                int letterCount = messageService.findLetterCount(conversation.getConversationId());
                map.put("letterCount", letterCount);

                // 将会话map加入会话列表
                conversations.add(map);
            }
        }
        model.addAttribute("conversations", conversations);

        // 用户未读消息数
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);
        // 用户未读通知数
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);

        return "/site/letter";
    }

    @LoginRequired
    @RequestMapping(path = "/letter/detail/{conversationId}", method = RequestMethod.GET)
    public String getLetterDetail(@PathVariable("conversationId") String conversationId, Model model, Page page) {
        User user = hostHodler.getUser();

        // 设置分页工具条
        page.setLimit(5);
        page.setRows(messageService.findLetterCount(conversationId));
        page.setPath("/message/detail/" + conversationId);

        // 消息列表
        List<Message> letterList = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> letters = new ArrayList<>();
        for (Message letter : letterList) {
            Map<String, Object> map = new HashMap<>();
            // 消息
            map.put("letter", letter);
            // 消息发送用户
            User fromUser = userService.findUserById(letter.getFromId());
            map.put("fromUser", fromUser);
            letters.add(map);
        }

        // 会话的对象
        User targetUser = getTargetUser(conversationId);
        model.addAttribute("targetUser", targetUser);
        model.addAttribute("letters", letters);

        // 更新消息未读状态
        List<Integer> unreadIds = getUnreadIds(letterList);
        if (!unreadIds.isEmpty()) {
            messageService.readMessage(unreadIds, 1);
        }

        return "/site/letter-detail";
    }

    private User getTargetUser(String conversationId) {
        User user = hostHodler.getUser();

        String[] fromTo = conversationId.split("_");
        int targetId = user.getId() == Integer.parseInt(fromTo[0]) ? Integer.parseInt(fromTo[1]) : Integer.parseInt(fromTo[0]);

        return userService.findUserById(targetId);
    }

    private List<Integer> getUnreadIds(List<Message> letterList) {
        List<Integer> unreadIds = new ArrayList<>();

        if (letterList != null) {
            for (Message letter : letterList) {
                if (letter.getToId() == hostHodler.getUser().getId() && letter.getStatus() == 0) {
                    unreadIds.add(letter.getId());
                }
            }
        }

        return unreadIds;
    }

    @LoginRequired
    @RequestMapping(path = "/letter/send", method = RequestMethod.POST)
    @ResponseBody
    public String sendLetter(String toName, String content) {
        // 获取收信方id
        User target = userService.findUserByName(toName);
        if (target == null) {
            return CommunityUtil.getJSONString(1, "收信人不存在！");
        }

        // 设置message信息
        Message message = new Message();
        message.setFromId(hostHodler.getUser().getId());
        message.setToId(target.getId());

        if (message.getFromId() < message.getToId()) {
            message.setConversationId(message.getFromId() + "_" + message.getToId());
        } else {
            message.setConversationId(message.getToId() + "_" + message.getFromId());
        }
        message.setContent(content);
        message.setCreateTime(new Date());

        // 发送信息
        messageService.addMessage(message);

        return CommunityUtil.getJSONString(0);
    }


    @RequestMapping(path = "/notice/list", method = RequestMethod.GET)
    public String getNotiaceList(Model model) {
        // 获取当前用户
        User user = hostHodler.getUser();

        // 评论类通知
        Message message = messageService.findLatestNotice(user.getId(), TOPIC_COMMENT);
        Map<String, Object> messageVO = new HashMap<>();
        if (message != null) {
            messageVO.put("message", message);

            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
            messageVO.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVO.put("entityType", data.get("entityType"));

            int count = messageService.findNoticeCount(user.getId(), TOPIC_COMMENT);
            messageVO.put("count", count);

            int unreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_COMMENT);
            messageVO.put("unreadCount", unreadCount);

            model.addAttribute("commentNotice", messageVO);
        }

        // 点赞类通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_LIKE);
        messageVO = new HashMap<>();
        if (message != null) {
            messageVO.put("message", message);

            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
            messageVO.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVO.put("entityType", data.get("entityType"));

            int count = messageService.findNoticeCount(user.getId(), TOPIC_LIKE);
            messageVO.put("count", count);

            int unreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_LIKE);
            messageVO.put("unreadCount", unreadCount);

            model.addAttribute("likeNotice", messageVO);
        }

        // 点赞类通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_FOLLOW);
        messageVO = new HashMap<>();
        if (message != null) {
            messageVO.put("message", message);

            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
            messageVO.put("user", userService.findUserById((Integer) data.get("userId")));

            int count = messageService.findNoticeCount(user.getId(), TOPIC_FOLLOW);
            messageVO.put("count", count);

            int unreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_FOLLOW);
            messageVO.put("unreadCount", unreadCount);

            model.addAttribute("followNotice", messageVO);
        }

        // 用户未读消息数
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);
        // 用户未读通知数
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);

        return "site/notice";
    }

    @RequestMapping(path = "/notice/detail/{topic}", method = RequestMethod.GET)
    public String getNoticeDetail(@PathVariable("topic") String topic, Page page, Model model) {
        // 获取当前用户
        User user = hostHodler.getUser();

        // 设置分页信息
        page.setPath("/notice/detail/" + topic);
        page.setLimit(5);
        page.setRows(messageService.findNoticeCount(user.getId(), topic));

        // 查询通知列表
        List<Message> notices = messageService.findNotices(user.getId(), topic, page.getOffset(), page.getLimit());
        List<Map<String, Object>> noticesVO = new ArrayList<>();
        if (notices != null) {
            for (Message notice : notices) {
                Map<String, Object> map = new HashMap<>();
                map.put("notice", notice);
                map.put("fromUser", userService.findUserById(notice.getFromId()));

                String content = HtmlUtils.htmlUnescape(notice.getContent());
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
                map.put("user", userService.findUserById((Integer) data.get("userId")));
                map.put("entityType", data.get("entityType"));
                map.put("postId", data.get("postId"));

                noticesVO.add(map);
            }
        }
        model.addAttribute("noticesVO", noticesVO);

        // 更新消息未读状态
        List<Integer> unreadIds = getUnreadIds(notices);
        if (!unreadIds.isEmpty()) {
            messageService.readMessage(unreadIds, 1);
        }

        return "site/notice-detail";
    }

}
