package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.R;
import com.blog.dto.CommentVo;
import com.blog.entity.BlogAbout;
import com.blog.entity.BlogComment;
import com.blog.entity.SysUser;
import com.blog.mapper.BlogAboutMapper;
import com.blog.mapper.BlogCommentMapper;
import com.blog.mapper.SysUserMapper;
import com.blog.service.BlogCommentService;
import com.blog.service.OssService;
import com.blog.service.impl.BlogCommentServiceImpl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Resource
    private BlogCommentMapper blogCommentMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private BlogCommentService blogCommentService;

    @Resource
    private BlogAboutMapper blogAboutMapper;

    @Resource
    private OssService ossService;

    // ========== 评论管理 ==========

    @GetMapping("/comments")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Map<String, Object>> listComments(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        Page<BlogComment> commentPage = blogCommentMapper.selectPage(
                new Page<>(page, size),
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BlogComment>()
                        .orderByDesc(BlogComment::getCreateTime)
        );

        List<CommentVo> voList = ((BlogCommentServiceImpl) blogCommentService).toFlatVoList(commentPage.getRecords());

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("records", voList);
        data.put("total", commentPage.getTotal());
        data.put("current", commentPage.getCurrent());
        data.put("size", commentPage.getSize());
        return R.ok(data);
    }

    @DeleteMapping("/comments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> deleteComment(@PathVariable Long id) {
        blogCommentMapper.deleteById(id);
        return R.ok();
    }

    // ========== 用户管理 ==========

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public R<List<SysUser>> listUsers() {
        List<SysUser> users = sysUserMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .orderByAsc(SysUser::getCreateTime)
        );
        users.forEach(u -> u.setPassword(null)); // 不返回密码
        return R.ok(users);
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> createUser(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String nickname = body.getOrDefault("nickname", username);
        String role = body.getOrDefault("role", "user");

        if (username == null || username.trim().isEmpty()) {
            return R.fail(400, "用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return R.fail(400, "密码不能为空");
        }
        SysUser exist = sysUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );
        if (exist != null) {
            return R.fail(400, "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname != null ? nickname.trim() : username.trim());
        user.setRole(role);
        sysUserMapper.insert(user);
        return R.ok();
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> deleteUser(@PathVariable Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user != null && "admin".equals(user.getRole()) && "admin".equals(user.getUsername())) {
            return R.fail(400, "不能删除超级管理员");
        }
        sysUserMapper.deleteById(id);
        return R.ok();
    }

    // ========== 关于页面管理 ==========

    @GetMapping("/about")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Map<String, Object>> getAbout() {
        SysUser admin = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRole, "admin")
                        .last("LIMIT 1")
        );
        if (admin == null) {
            return R.fail(404, "未找到站长信息");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("nickname", admin.getNickname());
        data.put("avatar", admin.getAvatar());
        data.put("email", admin.getEmail());

        BlogAbout about = blogAboutMapper.selectById(1);
        if (about != null) {
            data.put("bio", about.getBio());
            data.put("techStack", about.getTechStack());
            data.put("github", about.getGithub());
            data.put("twitter", about.getTwitter());
            data.put("publicEmail", about.getEmail());
            data.put("favicon", about.getFavicon());
            data.put("loginBg", about.getLoginBg());
        }
        return R.ok(data);
    }

    @PutMapping("/about")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> updateAbout(@RequestBody Map<String, String> body) {
        // Update admin user profile (nickname, avatar)
        SysUser admin = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRole, "admin")
                        .last("LIMIT 1")
        );
        if (admin != null) {
            if (body.containsKey("nickname")) {
                admin.setNickname(body.get("nickname"));
            }
            if (body.containsKey("avatar")) {
                admin.setAvatar(body.get("avatar"));
            }
            sysUserMapper.updateById(admin);
        }

        // Update about page content
        BlogAbout about = blogAboutMapper.selectById(1);
        if (about == null) {
            about = new BlogAbout();
            about.setId(1);
            if (body.containsKey("bio")) about.setBio(body.get("bio"));
            if (body.containsKey("techStack")) about.setTechStack(body.get("techStack"));
            if (body.containsKey("github")) about.setGithub(body.get("github"));
            if (body.containsKey("twitter")) about.setTwitter(body.get("twitter"));
            if (body.containsKey("publicEmail")) about.setEmail(body.get("publicEmail"));
            if (body.containsKey("favicon")) about.setFavicon(body.get("favicon"));
            if (body.containsKey("loginBg")) about.setLoginBg(body.get("loginBg"));
            blogAboutMapper.insert(about);
        } else {
            if (body.containsKey("bio")) about.setBio(body.get("bio"));
            if (body.containsKey("techStack")) about.setTechStack(body.get("techStack"));
            if (body.containsKey("github")) about.setGithub(body.get("github"));
            if (body.containsKey("twitter")) about.setTwitter(body.get("twitter"));
            if (body.containsKey("publicEmail")) about.setEmail(body.get("publicEmail"));
            if (body.containsKey("favicon")) about.setFavicon(body.get("favicon"));
            if (body.containsKey("loginBg")) about.setLoginBg(body.get("loginBg"));
            blogAboutMapper.updateById(about);
        }
        return R.ok();
    }

    @PostMapping("/upload/avatar")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Map<String, String>> adminUploadAvatar(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        if (file.isEmpty()) {
            return R.fail(400, "文件为空");
        }
        try {
            SysUser admin = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getRole, "admin")
                            .last("LIMIT 1")
            );
            Long userId = admin != null ? admin.getId() : 1L;
            String url = ossService.uploadAvatar(file, userId);
            Map<String, String> data = new HashMap<>();
            data.put("url", url);
            if (admin != null && !url.equals(admin.getAvatar())) {
                admin.setAvatar(url);
                sysUserMapper.updateById(admin);
            }
            return R.ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return R.fail(500, "上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/upload/favicon")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Map<String, String>> adminUploadFavicon(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        if (file.isEmpty()) {
            return R.fail(400, "文件为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("image/x-icon") && !contentType.equals("image/vnd.microsoft.icon"))) {
            return R.fail(400, "仅支持图片格式");
        }
        try {
            String url = ossService.upload(file, "site");
            BlogAbout about = blogAboutMapper.selectById(1);
            if (about == null) {
                about = new BlogAbout();
                about.setId(1);
                about.setFavicon(url);
                blogAboutMapper.insert(about);
            } else {
                about.setFavicon(url);
                blogAboutMapper.updateById(about);
            }
            Map<String, String> data = new HashMap<>();
            data.put("url", url);
            return R.ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return R.fail(500, "上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/upload/login-bg")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Map<String, String>> adminUploadLoginBg(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        if (file.isEmpty()) {
            return R.fail(400, "文件为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return R.fail(400, "仅支持图片格式");
        }
        try {
            String url = ossService.upload(file, "site");
            BlogAbout about = blogAboutMapper.selectById(1);
            if (about == null) {
                about = new BlogAbout();
                about.setId(1);
                about.setLoginBg(url);
                blogAboutMapper.insert(about);
            } else {
                about.setLoginBg(url);
                blogAboutMapper.updateById(about);
            }
            Map<String, String> data = new HashMap<>();
            data.put("url", url);
            return R.ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return R.fail(500, "上传失败: " + e.getMessage());
        }
    }
}
