package com.example.community.dao;

import com.example.community.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Mapper
public interface MessageMapper {

    List<Message> selectConversations(@Param("userId") int userId, @Param("offset") int offset, @Param("limit") int limit);

    int selectConversationCount(@Param("userId") int userId);

    List<Message> selectLetters(@Param("conversationId") String conversationId, @Param("offset") int offset, @Param("limit") int limit);

    int selectLetterCount(@Param("conversationId") String conversationId);

    int selectLetterUnreadCount(@Param("userId") int userId, @Param("conversationId") String conversationId);

    int insertMessage(Message message);

    int updateMessageStatus(@Param("ids") List<Integer> ids, @Param("status") int status);

    // 查询某个主题的系统通知数量
    int selectNoticeCount(@Param("userId") int userId, @Param("topic") String topic);

    // 查询某个主题的未读系统通知数量
    int selectNoticeUnreadCount(@Param("userId") int userId, @Param("topic") String topic);

    // 分页查询某个主体的系统通知
    List<Message> selectNotices(@Param("userId") int userId, @Param("topic") String topic, @Param("offset") int offset, @Param("limit") int limit);

    // 查询某个主题的最新通知
    Message selectLatestNotice(@Param("userId") int userId, @Param("topic") String topic);

}
