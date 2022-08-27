package com.example.community.service;

import com.example.community.dao.CommentMapper;
import com.example.community.dao.DiscussPostMapper;
import com.example.community.entity.Comment;
import com.example.community.util.CommunityContent;
import com.example.community.util.SensitiveWordFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class CommentService implements CommunityContent {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveWordFilter sensitiveWordFilter;

    public List<Comment> findCommentByEntity(int entityType, int entityId, int offset, int limit) {
        return commentMapper.selectCommentByEntity(entityType, entityId, offset, limit);
    }

    public int findCommentRowsByEntity(int entityType, int entityId) {
        return commentMapper.selectCommentRowsByEntity(entityType, entityId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int addComment(Comment comment) {
        if (comment == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        // 过滤敏感信息
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        comment.setContent(sensitiveWordFilter.filterSensitiveWord(comment.getContent()));

        // 添加评论
        commentMapper.insertComment(comment);

        // 更新帖子评论数
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            int commentCount = commentMapper.selectCommentRowsByEntity(comment.getEntityType(), comment.getEntityId());
            discussPostMapper.updateCommentCount(comment.getEntityId(), commentCount);
        }
        return 0;
    }

    public Comment findCommentById(int id) {
        return commentMapper.selectCommentById(id);
    }

}
