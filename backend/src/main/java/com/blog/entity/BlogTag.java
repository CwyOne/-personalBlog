package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("blog_tag")
public class BlogTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
}
