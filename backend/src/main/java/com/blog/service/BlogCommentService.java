package com.blog.service;

import com.blog.dto.CommentVo;

import java.util.List;

public interface BlogCommentService {

    List<CommentVo> getCommentsByArticleId(Long articleId);

    void addComment(Long articleId, Long userId, String content, Long parentId, Long replyToUserId);
}
