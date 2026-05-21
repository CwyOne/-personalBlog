package com.blog.service.impl;

import com.blog.dto.TagVo;
import com.blog.mapper.BlogTagMapper;
import com.blog.service.BlogTagService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class BlogTagServiceImpl implements BlogTagService {

    @Resource
    private BlogTagMapper blogTagMapper;

    @Override
    @Cacheable(value = "tags")
    public List<TagVo> getTagsWithCount() {
        return blogTagMapper.selectTagsWithCount();
    }
}
