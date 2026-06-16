package com.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.R;
import com.blog.dto.ArticleDto;
import com.blog.dto.ArticleListVo;
import com.blog.service.BlogArticleService;
import com.blog.service.OssService;
import com.blog.util.DocxToMdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminArticleController {

    @Resource
    private BlogArticleService blogArticleService;

    @Resource
    private OssService ossService;

    /** Markdown 最大字符数 */
    private static final int MAX_MD_LENGTH = 15000;

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

    /**
     * 上传 DOCX 文档，转换为 Markdown 并返回
     */
    @PostMapping("/articles/upload-docx")
    @PreAuthorize("hasRole('ADMIN')")
    public R<String> uploadDocx(@RequestParam("file") MultipartFile file) {
        // 1. 文件非空校验
        if (file.isEmpty()) {
            return R.fail(400, "请选择要上传的文件");
        }

        // 2. 文件格式校验
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".docx")) {
            return R.fail(400, "仅支持 .docx 格式的 Word 文档");
        }

        // 3. 文件大小校验（10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            return R.fail(400, "文件大小不能超过 10MB");
        }

        try {
            // 4. 解析 DOCX → Markdown
            String markdown = DocxToMdUtil.convert(file.getInputStream(), ossService);

            // 5. 内容长度校验
            if (markdown.length() > MAX_MD_LENGTH) {
                return R.fail(400, "文档转换后内容过长（超过 " + MAX_MD_LENGTH + " 字），请精简文档内容");
            }

            if (markdown.isEmpty()) {
                return R.fail(400, "文档内容为空，请检查文件");
            }

            return R.ok("转换成功", markdown);
        } catch (IllegalArgumentException e) {
            log.warn("DOCX 解析参数错误: {}", e.getMessage());
            return R.fail(400, e.getMessage());
        } catch (Exception e) {
            log.error("DOCX 解析失败", e);
            return R.fail(500, "文档解析失败，请确认文件格式正确且未损坏");
        }
    }
}
