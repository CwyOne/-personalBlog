package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.BlogArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BlogArticleMapper extends BaseMapper<BlogArticle> {

    @Select("SELECT DISTINCT ba.* FROM blog_article ba " +
            "INNER JOIN article_tag at ON ba.id = at.article_id " +
            "WHERE at.tag_id = #{tagId} AND ba.status = #{status} " +
            "ORDER BY ba.create_time DESC")
    List<BlogArticle> selectByTag(@Param("tagId") Long tagId, @Param("status") Integer status);

    @Select("SELECT DISTINCT ba.* FROM blog_article ba " +
            "LEFT JOIN article_tag at ON ba.id = at.article_id " +
            "LEFT JOIN blog_tag bt ON at.tag_id = bt.id " +
            "WHERE ba.status = 1 AND (" +
            "ba.title LIKE CONCAT('%',#{keyword},'%') OR " +
            "ba.content_md LIKE CONCAT('%',#{keyword},'%') OR " +
            "ba.summary LIKE CONCAT('%',#{keyword},'%') OR " +
            "bt.name LIKE CONCAT('%',#{keyword},'%')) " +
            "ORDER BY ba.create_time DESC")
    List<BlogArticle> searchByKeyword(@Param("keyword") String keyword);

    @Select("SELECT ba.* FROM blog_article ba WHERE ba.status = 1 ORDER BY ba.create_time DESC")
    List<BlogArticle> selectAllPublished();
}
