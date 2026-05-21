package com.blog.service;

import com.blog.dto.TagVo;

import java.util.List;

public interface BlogTagService {

    List<TagVo> getTagsWithCount();
}
