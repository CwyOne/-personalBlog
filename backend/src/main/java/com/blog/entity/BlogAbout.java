package com.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("blog_about")
public class BlogAbout {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String bio;
    private String techStack;
    private String github;
    private String twitter;
    private String email;
    private String favicon;
    private String loginBg;
}
