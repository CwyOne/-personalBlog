package com.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.R;
import com.blog.dto.ArticleDto;
import com.blog.dto.ArticleListVo;
import com.blog.service.BlogArticleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/admin")
public class AdminArticleController {

    @Resource
    private BlogArticleService blogArticleService;

    @GetMapping("/articles")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Page<ArticleListVo>> list(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        Page<ArticleListVo> result = blogArticleService.getAdminArticleList(page, size);
        return R.ok(result);
    }

    @PostMapping("/articles")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> create(@Valid @RequestBody ArticleDto dto) {
        blogArticleService.createArticle(dto);
        return R.ok();
    }

    @PutMapping("/articles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ArticleDto dto) {
        blogArticleService.updateArticle(id, dto);
        return R.ok();
    }

    @DeleteMapping("/articles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        blogArticleService.deleteArticle(id);
        return R.ok();
    }
}
