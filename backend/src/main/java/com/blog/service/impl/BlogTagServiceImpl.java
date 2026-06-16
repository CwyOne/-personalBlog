package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.dto.TagVo;
import com.blog.entity.BlogTag;
import com.blog.mapper.BlogTagMapper;
import com.blog.service.BlogTagService;
import org.springframework.cache.annotation.CacheEvict;
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

    @Override
    @CacheEvict(value = "tags", allEntries = true)
    public BlogTag createTag(String name) {
        // Check if tag already exists
        BlogTag existing = blogTagMapper.selectOne(
            new LambdaQueryWrapper<BlogTag>().eq(BlogTag::getName, name)
        );
        if (existing != null) {
            return existing;
        }
        BlogTag tag = new BlogTag();
        tag.setName(name);
        blogTagMapper.insert(tag);
        return tag;
    }
}
