package com.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.dto.ArticleDto;
import com.blog.dto.ArticleListVo;
import com.blog.dto.ArchiveVo;

import java.util.List;

public interface BlogArticleService {

    Page<ArticleListVo> getArticleList(int page, int size, Integer status, Long tagId);

    ArticleListVo getArticleDetail(Long id);

    Page<ArticleListVo> getAdminArticleList(int page, int size);

    Page<ArticleListVo> searchArticles(int page, int size, String keyword);

    List<ArchiveVo> getArchive();

    void createArticle(ArticleDto dto);

    void updateArticle(Long id, ArticleDto dto);

    void deleteArticle(Long id);
}
