package com.blog.controller;

import com.blog.common.R;
import com.blog.dto.CommentVo;
import com.blog.service.BlogCommentService;
import com.blog.service.impl.BlogCommentServiceImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
public class CommentController {

    @Resource
    private BlogCommentService blogCommentService;

    @GetMapping("/api/articles/{id}/comments")
    public R<List<CommentVo>> list(@PathVariable Long id) {
        Long userId = getUserId();
        List<CommentVo> comments;
        if (userId != null) {
            comments = ((BlogCommentServiceImpl) blogCommentService).getCommentsByArticleId(id, userId);
        } else {
            comments = blogCommentService.getCommentsByArticleId(id);
        }
        return R.ok(comments);
    }

    @PostMapping("/api/articles/{id}/comments")
    public R<Void> create(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = getUserId();
        if (userId == null) return R.fail(401, "请先登录");
        String content = body.get("content");
        if (content == null || content.trim().isEmpty()) {
            return R.fail(400, "评论内容不能为空");
        }
        blogCommentService.addComment(id, userId, content.trim());
        return R.ok();
    }

    private Long getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return Long.valueOf(auth.getPrincipal().toString());
    }
}
