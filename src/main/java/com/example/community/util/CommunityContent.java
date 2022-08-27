package com.example.community.util;

public interface CommunityContent {
    /**
     * 账户激活状态：成功、重复、失败
     */
    int ACTIVATION_SUCCES = 0;
    int ACTIVATION_REPEAT = 1;
    int ACTIVATION_FAILURE = 2;

    /**
     * 登录凭证超时时间（s）：默认、记住密码
     */
    int DEFAULT_EXPIRED_SECONDS = 3600 * 12;
    int REMEMBER_EXPIRED_SECONDS = 3600 * 24 * 100;

    /**
     * 验证码超时时间（s）
     */
    int CODE_EXPIRED_SECONDS = 60 * 5;

    /**
     * 评论主体类型：帖子、评论、用户
     */
    int ENTITY_TYPE_POST = 1;
    int ENTITY_TYPE_COMMENT = 2;
    int ENTITY_TYPE_USER = 3;

    /**
     * 点赞状态：已赞、未赞
     */
    int LIKE_STATUS_YES = 1;
    int LIKE_STATUS_NO = 0;

    /**
     * 主题：评论、点赞、关注、发帖、删帖、分享
     */
    String TOPIC_COMMENT = "comment";
    String TOPIC_LIKE = "like";
    String TOPIC_FOLLOW = "follow";
    String TOPIC_PUBLISH = "publish";
    String TOPIC_DELETE = "delete";
    String TOPIC_SHARE = "share";

    /**
     * 系统用户ID
     */
    int SYSTEM_USER_ID = 1;

    /**
     * 用户权限：普通用户、管理员、版主
     */
    String AUTHORITY_USER = "user";
    String AUTHORITY_ADMIN = "admin";
    String AUTHORITY_MODERATOR = "moderator";

    /**
     * 帖子类型
     */
    int POST_TYPE_TOP = 1;

    /**
     * 帖子状态
     */
    int POST_STATUS_ESSENCE = 1;
    int POST_STATUS_DELETE = 2;

    /**
     * 帖子展示模式：按时间、按热度
     */
    int ORDER_MODE_TIME = 0;
    int ORDER_MODE_SCORE = 1;
}
