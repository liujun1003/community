package com.example.community.controller;

import com.example.community.annotation.LoginRequired;
import com.example.community.entity.Event;
import com.example.community.entity.Page;
import com.example.community.entity.User;
import com.example.community.event.EventProducer;
import com.example.community.service.FollowService;
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

import java.util.List;
import java.util.Map;

@Controller
public class FollowController implements CommunityContent {

    @Autowired
    private UserService userService;

    @Autowired
    private FollowService followService;

    @Autowired
    private HostHodler hostHodler;

    @Autowired
    private EventProducer eventProducer;

    @LoginRequired
    @RequestMapping(path = "/follow", method = RequestMethod.POST)
    @ResponseBody
    public String follow(int entityType, int entityId) {
        // 执行关注
        followService.follow(hostHodler.getUser().getId(), entityType, entityId);

        // 触发关注事件
        Event event = new Event()
                .setTopic(TOPIC_FOLLOW)
                .setUserId(hostHodler.getUser().getId())
                .setEntityType(entityType)
                .setEntityId(entityId)
                .setEntityUserId(entityId);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0, "已关注！");
    }

    @LoginRequired
    @RequestMapping(path = "/unfollow", method = RequestMethod.POST)
    @ResponseBody
    public String unfollow(int entityType, int entityId) {
        // 执行关注
        followService.unfollow(hostHodler.getUser().getId(), entityType, entityId);

        return CommunityUtil.getJSONString(0, "已取关！");
    }

    @LoginRequired
    @RequestMapping(path = "/followees/{userId}", method = RequestMethod.GET)
    public String followees(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在！");
        }

        // 设置分页信息
        page.setPath("/followees/" + userId);
        page.setLimit(5);
        page.setRows(followService.findFolloweeCount(userId, ENTITY_TYPE_USER));

        List<Map<String, Object>> userFollowees = followService.findFollowees(userId, ENTITY_TYPE_USER, page.getOffset(), page.getLimit());
        // 添加当前用户是否关注目标用户
        if (userFollowees != null) {
            for (Map<String, Object> map : userFollowees) {
                User u = (User) map.get("entity");
                map.put("isFollow", isFollow(u.getId()));
            }
        }

        model.addAttribute("user", user);
        model.addAttribute("userFollowees", userFollowees);
        return "/site/followee";
    }

    @LoginRequired
    @RequestMapping(path = "/followers/{userId}", method = RequestMethod.GET)
    public String followers(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在！");
        }

        // 设置分页信息
        page.setPath("/followers/" + userId);
        page.setLimit(5);
        page.setRows(followService.findFollowerCount(ENTITY_TYPE_USER, userId));

        List<Map<String, Object>> userFollowers = followService.findFollowers(ENTITY_TYPE_USER, userId, page.getOffset(), page.getLimit());
        // 添加当前用户是否关注目标用户
        if (userFollowers != null) {
            for (Map<String, Object> map : userFollowers) {
                User u = (User) map.get("user");
                map.put("isFollow", isFollow(u.getId()));
            }
        }

        model.addAttribute("user", user);
        model.addAttribute("userFollowers", userFollowers);
        return "/site/follower";
    }

    private boolean isFollow(int userId) {
        if (hostHodler.getUser() == null) {
            return false;
        }
        return followService.findFollowStatus(hostHodler.getUser().getId(), ENTITY_TYPE_USER, userId);
    }

}
