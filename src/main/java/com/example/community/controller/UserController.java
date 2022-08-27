package com.example.community.controller;

import com.example.community.annotation.LoginRequired;
import com.example.community.entity.User;
import com.example.community.service.FollowService;
import com.example.community.service.LikeService;
import com.example.community.service.UserService;
import com.example.community.util.CommunityContent;
import com.example.community.util.CommunityUtil;
import com.example.community.util.HostHodler;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

@Controller
@RequestMapping(path = "/user")
public class UserController implements CommunityContent{

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${community.path.upload}")
    private String upload;

    @Autowired
    private UserService userService;

    @Autowired
    private FollowService followService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private HostHodler hostHodler;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.name}")
    private String bucketHeaderName;

    @Value("${qiniu.bucket.header.url}")
    private String bucketHeaderUrl;

    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage(Model model) {
        // 生成上传的文件名
        String fileName = CommunityUtil.generateUUID();

        // 设置响应信息
        StringMap policy = new StringMap();
        policy.put("returnBody", CommunityUtil.getJSONString(0));

        // 生成upload_token传回setting页面
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(bucketHeaderName, fileName, 3600, policy);

        // 将参数返回页面
        model.addAttribute("fileName", fileName);
        model.addAttribute("uploadToken", uploadToken);

        return "/site/setting";
    }

    // 更新头像访问路径
    @RequestMapping(path = "/update/header", method = RequestMethod.POST)
    @ResponseBody
    public String updateHeaderUrl(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return CommunityUtil.getJSONString(1, "参数不能为空！");
        }

        String url = bucketHeaderUrl + "/" + fileName;
        userService.updateHeaderUrl(hostHodler.getUser().getId(), url);

        return CommunityUtil.getJSONString(0);
    }

    @Deprecated
    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String upload(MultipartFile headerImage, Model model) {
        // 验证文件是否上传
        if (headerImage == null) {
            model.addAttribute("error", "未选择图片！");
            return "/site/setting";
        }

        // 验证文件格式
        String fileName = headerImage.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        if (StringUtils.isBlank(suffix)) {
            model.addAttribute("error", "文件格式不正确！");
            return "/site/setting";
        }

        // 上传文件至服务器上传目录
        fileName = CommunityUtil.generateUUID() + suffix;
        File dest = new File(upload + "/" + fileName);
        try {
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("文件上传失败！" + e.getMessage());
            throw new RuntimeException("文件上传失败，服务器发生异常！" + e.getMessage());
        }

        // 更新用户的头像图片链接（headerUrl）
        String headerUrl = domain + contextPath + "/user/header/" + fileName;
        User user = hostHodler.getUser();
        if (user != null) {
            userService.updateHeaderUrl(user.getId(), headerUrl);
        }

        return "redirect:/index";
    }

    @Deprecated
    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        // 构建头像文件在服务器的路径
        String filePath = upload + "/" + fileName;

        // 设置response响应的类型
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        response.setContentType("image/" + suffix);

        //响应图片
        try (
                FileInputStream is = new FileInputStream(filePath);
                ServletOutputStream os = response.getOutputStream()
        ) {
            IOUtils.copy(is, os);
        } catch (IOException e) {
            logger.error("读取头像失败！" + e.getMessage());
        }
    }

    @RequestMapping(path = "/modifyPassword", method = RequestMethod.POST)
    public String modifyPassword(Model model, String passwordOrigin, String passwordNew) {
        Map<String, Object> map = userService.modifyPassword(passwordOrigin, passwordNew);

        if (map.isEmpty()) {
            model.addAttribute("msg", "密码修改成功,稍后请重新登录！");
            model.addAttribute("target", "/logout");
            return "/site/operate-result";
        } else {
            model.addAttribute("passwordOriginMsg", map.get("passwordOriginMsg"));
            model.addAttribute("passwordNewMsg", map.get("passwordNewMsg"));
            model.addAttribute("passwordOrigin", passwordOrigin);
            model.addAttribute("passwordNew", passwordNew);
            return "/site/setting";
        }
    }

    @LoginRequired
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfile(@PathVariable("userId") int userId, Model model) {
        // 查询用户是否存在
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在！");
        }

        // 查询该用户的关注数量
        int followeeCount = followService.findFolloweeCount(user.getId(), ENTITY_TYPE_USER);

        // 查询该用户的粉丝数量
        int followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, user.getId());

        // 查询当前用户是否关注该用户
        boolean isFollow = hostHodler.getUser() != null && followService.findFollowStatus(hostHodler.getUser().getId(), ENTITY_TYPE_USER, userId);

        // 查询用户的获赞数量
        int likeCount = likeService.findUserLikeCount(userId);

        // 将参数加入model对象返回
        model.addAttribute("user", user);
        model.addAttribute("followeeCount", followeeCount);
        model.addAttribute("followerCount", followerCount);
        model.addAttribute("isFollow", isFollow);
        model.addAttribute("likeCount", likeCount);

        return "/site/profile";
    }
}
