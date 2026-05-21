package com.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.R;
import com.blog.dto.ArticleListVo;
import com.blog.dto.ArchiveVo;
import com.blog.service.BlogArticleService;
import com.blog.service.impl.BlogArticleServiceImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
public class ArticleController {

    @Resource
    private BlogArticleService blogArticleService;

    @GetMapping("/api/articles")
    public R<Page<ArticleListVo>> list(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size,
                                        @RequestParam(required = false) Long tag) {
        Page<ArticleListVo> result = blogArticleService.getArticleList(page, size, 1, tag);
        return R.ok(result);
    }

    @GetMapping("/api/articles/search")
    public R<Page<ArticleListVo>> search(@RequestParam String keyword,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        Page<ArticleListVo> result = blogArticleService.searchArticles(page, size, keyword);
        return R.ok(result);
    }

    @GetMapping("/api/articles/archive")
    public R<List<ArchiveVo>> archive() {
        List<ArchiveVo> result = blogArticleService.getArchive();
        return R.ok(result);
    }

    @GetMapping("/api/articles/{id}")
    public R<ArticleListVo> detail(@PathVariable Long id) {
        Long userId = getUserId();
        ArticleListVo vo;
        if (userId != null) {
            vo = ((BlogArticleServiceImpl) blogArticleService).getArticleDetail(id, userId);
        } else {
            vo = blogArticleService.getArticleDetail(id);
        }
        if (vo == null) {
            return R.fail(404, "文章未找到");
        }
        return R.ok(vo);
    }

    private Long getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return Long.valueOf(auth.getPrincipal().toString());
    }
}
