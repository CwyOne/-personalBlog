package com.blog.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class ArticleDto {

    @NotBlank(message = "Title is required")
    private String title;

    private String summary;

    private String coverImage;

    @NotBlank(message = "Content is required")
    private String contentMd;

    private Integer status;

    private List<Long> tagIds;
}
