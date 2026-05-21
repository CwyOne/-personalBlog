package com.blog.controller;

import com.blog.common.R;
import com.blog.dto.TagVo;
import com.blog.service.BlogTagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
public class TagController {

    @Resource
    private BlogTagService blogTagService;

    @GetMapping("/api/tags")
    public R<List<TagVo>> tags() {
        List<TagVo> tags = blogTagService.getTagsWithCount();
        return R.ok(tags);
    }
}
