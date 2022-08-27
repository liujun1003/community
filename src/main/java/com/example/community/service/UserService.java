package com.example.community.service;

import com.example.community.dao.LoginTicketMapper;
import com.example.community.dao.UserMapper;
import com.example.community.entity.LoginTicket;
import com.example.community.entity.User;
import com.example.community.util.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.net.Authenticator;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityContent {

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private HostHodler hostHodler;

    @Autowired
    private RedisTemplate redisTemplate;

    public User findUserById(int userId) {
//        return userMapper.selectUserById(userId);
        User user = this.findCache(userId);
        if (user == null) {
            user = this.initCache(userId);
        }
        return user;
    }

    public User findUserByName(String name) {
        return userMapper.selectUserByName(name);
    }

    public Map<String, Object> register(User user) {
        Map<String, Object> map = new HashMap<>(); // 存储提示信息

        // 空值校验
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "用户名不能为空！");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空！");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }

        // 验证用户名
        User u = userMapper.selectUserByName(user.getUsername());
        if (u != null) {
            map.put("usernameMsg", "该用户名已存在！");
            return map;
        }

        // 验证邮箱
        u = userMapper.selectUserByEmail(user.getEmail());
        if (u != null) {
            map.put("emailMsg", "该邮箱已注册！");
            return map;
        }

        // 注册
        user.setSalt(CommunityUtil.generateUUID().substring(0, 6));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        // 激活邮件
        Context context = new Context();
        context.setVariable("username", user.getUsername());
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMessage(user.getEmail(), "账户激活", content);

        return map;
    }

    public int activation(int userId, String activationCode) {
        User user = userMapper.selectUserById(userId);
        if (user.getStatus() == 1) {
            return ACTIVATION_REPEAT;
        }
        if (user.getActivationCode().equals(activationCode)) {
            userMapper.updateStatus(userId, 1);
            this.clearCache(userId);
            return ACTIVATION_SUCCES;
        }
        return ACTIVATION_FAILURE;
    }

    public Map<String ,Object> login(String username, String password, long expiredSeconds) {
        Map<String ,Object> map = new HashMap<>();

        // 空值校验
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "用户名不能为空！");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }

        // 验证账户
        User user = userMapper.selectUserByName(username);
        if (user == null) {
            map.put("usernameMsg", "用户未注册！");
            return map;
        }
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "用户未激活！");
            return map;
        }

        // 验证密码
        password = CommunityUtil.md5(password + user.getSalt());
        assert password != null;
        if (!password.equals(user.getPassword())) {
            map.put("passwordMsg", "密码不正确！");
            return map;
        }

        // 生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + 1000 * expiredSeconds));
//        loginTicketMapper.insertLoginTicket(loginTicket);

        // 存入redis
        String loginTicketKey = RedisKeyUtil.getLoginTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(loginTicketKey, loginTicket, expiredSeconds, TimeUnit.SECONDS);

        map.put("ticket", loginTicket.getTicket());
        return map;
    }

    public void logout(String ticket) {
//        LoginTicket loginTicket = loginTicketMapper.selectLoginTicketByTicket(ticket);
        String loginTicketKey = RedisKeyUtil.getLoginTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(loginTicketKey);
        if (loginTicket != null) {
//            loginTicketMapper.updateStatusByTicket(ticket, 1);
            loginTicket.setStatus(1);
            redisTemplate.opsForValue().set(loginTicketKey, loginTicket);
        }
    }

    public LoginTicket findLoginTicketByTicket(String ticket) {
//        return loginTicketMapper.selectLoginTicketByTicket(ticket);
        String loginTicketKey = RedisKeyUtil.getLoginTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(loginTicketKey);
    }

    public int updateHeaderUrl(int userId, String headerUrl) {
        int rows = userMapper.updateHeaderUrl(userId, headerUrl);
        this.clearCache(userId);
        return rows;
    }

    public Map<String, Object> modifyPassword(String passwordOrigin, String passwordNew) {
        Map<String, Object> map = new HashMap<>();

        // 空值校验
        if (StringUtils.isBlank(passwordOrigin)) {
            map.put("passwordOriginMsg", "请输入原密码！");
            return map;
        }
        if (StringUtils.isBlank(passwordNew)) {
            map.put("passwordNewMsg", "请输入新密码！");
            return map;
        }

        // 原密码校验
        User user = hostHodler.getUser();
        if (!user.getPassword().equals(CommunityUtil.md5(passwordOrigin + user.getSalt()))) {
            map.put("passwordOriginMsg", "原密码输入有误！");
            return map;
        }

        // 修改密码
        userMapper.updatePassword(user.getId(), CommunityUtil.md5(passwordNew + user.getSalt()));
        this.clearCache(user.getId());
        return map;
    }

    // 初始化用户缓存
    public User initCache(int userId) {
        User user = userMapper.selectUserById(userId);
        if (user != null) {
            // 定义用户缓存的key
            String userCacheKey = RedisKeyUtil.getUserCacheKey(userId);
            redisTemplate.opsForValue().set(userCacheKey, user, 1, TimeUnit.HOURS);
        }
        return user;
    }

    // 查询用户缓存
    public User findCache(int userId) {
        // 定义用户缓存的key
        String userCacheKey = RedisKeyUtil.getUserCacheKey(userId);
        return (User) redisTemplate.opsForValue().get(userCacheKey);
    }

    // 清除用户缓存
    public void clearCache(int userId) {
        // 定义用户缓存的key
        String userCacheKey = RedisKeyUtil.getUserCacheKey(userId);
        redisTemplate.delete(userCacheKey);
    }

    // 根据用户id获取用户的权限
    public Collection<? extends GrantedAuthority> getAuthorities(int id) {
        User user = this.findUserById(id);

        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (user.getType()) {
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });
        return list;
    }
}
