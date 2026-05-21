package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.dto.TagVo;
import com.blog.entity.BlogTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BlogTagMapper extends BaseMapper<BlogTag> {

    @Select("SELECT bt.id, bt.name, COUNT(at.article_id) AS article_count " +
            "FROM blog_tag bt " +
            "LEFT JOIN article_tag at ON bt.id = at.tag_id " +
            "LEFT JOIN blog_article ba ON at.article_id = ba.id AND ba.status = 1 " +
            "GROUP BY bt.id, bt.name " +
            "ORDER BY article_count DESC")
    List<TagVo> selectTagsWithCount();
}
