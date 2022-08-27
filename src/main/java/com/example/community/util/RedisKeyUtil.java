package com.example.community.util;

public class RedisKeyUtil {

    private static final String SPLIT = ":";
    private static final String PREFIX_USER_LIKE = "user:like";
    private static final String PREFIX_ENTITY_LIKE = "entity:like";
    private static final String PREFIX_FOLLOWEE = "followee";
    private static final String PREFIX_FOLLOWER = "follower";
    private static final String PREFIX_USER_CACHE = "user:cache";
    private static final String PREFIX_KAPTCHA = "kaptcha";
    private static final String PREFIX_LOGIN_TICKET = "login:ticket";
    private static final String PREFIX_UV = "uv";
    private static final String PREFIX_DAU = "dau";
    private static final String PREFIX_POST_CHANGE = "post:change";


    // 某个用户的赞
    // user:like:userId -> int
    public static String getUserLikeKey(int userId) {
        return PREFIX_USER_LIKE + SPLIT + userId;
    }

    // 某个实体的赞
    // entity:like:entityType:entityId -> set(userId)
    public static String getEntityLikeKey(int entityTYpe, int entityId) {
        return PREFIX_ENTITY_LIKE + SPLIT + entityTYpe + SPLIT + entityId;
    }

    // 某个用户关注的实体
    // user:followee:userId -> zset(userId, time)
    public static String getFolloweeKey(int userId, int entityType) {
        return PREFIX_FOLLOWEE + SPLIT + userId + SPLIT + entityType;
    }

    // 某个实体的粉丝
    // user:follower:userId -> zset(userId, time)
    public static String getFollowerKey(int entityType, int entityId) {
        return PREFIX_FOLLOWER + SPLIT + entityType + SPLIT + entityId;
    }

    // 用户缓存
    // user:cache:userId -> String（User对象的序列化字符串）
    public static String getUserCacheKey(int userId) {
        return PREFIX_USER_CACHE + SPLIT + userId;
    }

    // 验证码
    // kaptcha:owner -> String（验证码）
    public static String getKaptchaKey(String owner) {
        return PREFIX_KAPTCHA + SPLIT + owner;
    }

    // 登录凭证
    // login:ticket:ticket -> String（登录凭证对象）
    public static String getLoginTicketKey(String ticket) {
        return PREFIX_LOGIN_TICKET + SPLIT +ticket;
    }

    // UV
    // uv:date -> ip
    public static String getUVKey(String date) {
        return PREFIX_UV + SPLIT + date;
    }
    public static String getUVKey(String startDate, String endDate) {
        return PREFIX_UV + SPLIT + startDate + SPLIT + endDate;
    }

    // DAU
    // dau:date -> userId
    public static String getDAUKey(String date) {
        return PREFIX_DAU + SPLIT + date;
    }
    public static String getDAUKey(String startDate, String endDate) {
        return PREFIX_DAU + SPLIT + startDate + SPLIT + endDate;
    }

    // 数据发生变化的帖子id，需要重新计算热度score
    // post:change -> postId
    public static String getPostChangeKey() {
        return PREFIX_POST_CHANGE + SPLIT + "ids";
    }

}
