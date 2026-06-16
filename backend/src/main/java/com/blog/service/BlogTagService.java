package com.blog.service;

import com.blog.dto.TagVo;
import com.blog.entity.BlogTag;

import java.util.List;

public interface BlogTagService {

    List<TagVo> getTagsWithCount();

    BlogTag createTag(String name);
}
