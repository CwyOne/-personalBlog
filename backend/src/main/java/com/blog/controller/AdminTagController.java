package com.blog.controller;

import com.blog.common.R;
import com.blog.entity.BlogTag;
import com.blog.service.BlogTagService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/admin")
public class AdminTagController {

    @Resource
    private BlogTagService blogTagService;

    @PostMapping("/tags")
    @PreAuthorize("hasRole('ADMIN')")
    public R<BlogTag> createTag(@RequestBody BlogTag tag) {
        BlogTag created = blogTagService.createTag(tag.getName());
        return R.ok(created);
    }
}
