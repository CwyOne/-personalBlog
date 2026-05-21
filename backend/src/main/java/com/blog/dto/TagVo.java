package com.blog.dto;

import lombok.Data;

@Data
public class TagVo {

    private Long id;

    private String name;

    private Integer articleCount;
}
