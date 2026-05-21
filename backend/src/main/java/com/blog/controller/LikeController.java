package com.blog.controller;

import com.blog.common.R;
import com.blog.service.LikeService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@RestController
public class LikeController {

    @Resource
    private LikeService likeService;

    @PostMapping("/api/articles/{id}/like")
    public R<Map<String, Object>> likeArticle(@PathVariable Long id) {
        Long userId = getUserId();
        if (userId == null) return R.fail(401, "请先登录");
        return R.ok(likeService.toggleLike(userId, "article", id));
    }

    @PostMapping("/api/comments/{id}/like")
    public R<Map<String, Object>> likeComment(@PathVariable Long id) {
        Long userId = getUserId();
        if (userId == null) return R.fail(401, "请先登录");
        return R.ok(likeService.toggleLike(userId, "comment", id));
    }

    private Long getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return Long.valueOf(auth.getPrincipal().toString());
    }
}
