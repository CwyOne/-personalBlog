package com.blog.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentVo {

    private Long id;

    private Long articleId;

    private Long userId;

    private String username;

    private String nickname;

    private String avatar;

    private String articleTitle;

    private String content;

    private Long likeCount;

    private Boolean liked;

    private LocalDateTime createTime;
}
