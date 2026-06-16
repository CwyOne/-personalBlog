package com.blog.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVo {

    private Long id;

    private Long articleId;

    private Long userId;

    private String username;

    private String nickname;

    private String avatar;

    private String articleTitle;

    private Long parentId;

    private Long replyToUserId;

    private String replyToNickname;

    private String content;

    private Long likeCount;

    private Boolean liked;

    private LocalDateTime createTime;

    private List<CommentVo> children;
}
