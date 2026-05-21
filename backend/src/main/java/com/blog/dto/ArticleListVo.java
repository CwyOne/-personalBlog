package com.blog.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleListVo {

    private Long id;

    private String title;

    private String summary;

    private String coverImage;

    private Integer status;

    private Integer viewCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String contentMd;

    private String contentHtml;

    private String authorNickname;

    private String authorAvatar;

    private Long likeCount;

    private Boolean liked;

    private List<TagVo> tags;
}
