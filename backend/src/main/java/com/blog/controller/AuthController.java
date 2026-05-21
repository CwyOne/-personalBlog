package com.blog.controller;

import com.blog.common.R;
import com.blog.dto.LoginDto;
import com.blog.dto.RegisterDto;
import com.blog.entity.BlogAbout;
import com.blog.entity.SysUser;
import com.blog.mapper.BlogAboutMapper;
import com.blog.mapper.SysUserMapper;
import com.blog.service.MailService;
import com.blog.service.OssService;
import com.blog.service.SysUserService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@RestController
public class AuthController {

    @Resource
    private SysUserService sysUserService;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private MailService mailService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private BlogAboutMapper blogAboutMapper;

    @PostMapping("/api/login")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginDto loginDto) {
        Map<String, Object> result = sysUserService.login(loginDto);
        return R.ok(result);
    }

    @PostMapping("/api/register")
    public R<Void> register(@Valid @RequestBody RegisterDto registerDto) {
        // 验证邮箱验证码
        String key = "email:code:" + registerDto.getEmail();
        String cachedCode = stringRedisTemplate.opsForValue().get(key);
        if (cachedCode == null || !cachedCode.equals(registerDto.getCode())) {
            return R.fail(400, "验证码错误或已过期");
        }
        sysUserService.register(registerDto);
        stringRedisTemplate.delete(key); // 验证成功后删除验证码
        return R.ok();
    }

    @PostMapping("/api/send-code")
    public R<Void> sendCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.trim().isEmpty()) {
            return R.fail(400, "邮箱不能为空");
        }
        // 检查邮箱是否已注册
        SysUser exist = sysUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getEmail, email)
        );
        if (exist != null) {
            return R.fail(400, "该邮箱已被注册");
        }
        // 60秒内不能重复发送
        String limitKey = "email:limit:" + email;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(limitKey))) {
            return R.fail(400, "请60秒后再重新发送");
        }
        // 生成6位验证码
        String code = String.format("%06d", new Random().nextInt(999999));
        String key = "email:code:" + email;
        stringRedisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);
        stringRedisTemplate.opsForValue().set(limitKey, "1", 60, TimeUnit.SECONDS);
        mailService.sendVerifyCode(email, code);
        return R.ok();
    }

    // ========== 关于博主 ==========

    @GetMapping("/api/about")
    public R<Map<String, Object>> about() {
        SysUser admin = sysUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
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
        data.put("createTime", admin.getCreateTime());

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

    // ========== 用户中心 ==========

    @GetMapping("/api/user/profile")
    public R<Map<String, Object>> profile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return R.fail(401, "请先登录");
        }
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return R.fail(404, "用户不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("phone", user.getPhone());
        data.put("email", user.getEmail());
        data.put("emailVerified", user.getEmailVerified());
        data.put("role", user.getRole());
        data.put("createTime", user.getCreateTime());
        return R.ok(data);
    }

    @PutMapping("/api/user/profile")
    public R<Void> updateProfile(@RequestBody Map<String, String> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return R.fail(401, "请先登录");
        }
        Long userId = Long.valueOf(auth.getPrincipal().toString());
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return R.fail(404, "用户不存在");
        }
        if (body.containsKey("nickname")) {
            user.setNickname(body.get("nickname"));
        }
        if (body.containsKey("avatar")) {
            user.setAvatar(body.get("avatar"));
        }
        if (body.containsKey("phone")) {
            user.setPhone(body.get("phone"));
        }
        if (body.containsKey("password") && body.get("password") != null && !body.get("password").isEmpty()) {
            user.setPassword(passwordEncoder.encode(body.get("password")));
        }
        sysUserMapper.updateById(user);
        return R.ok();
    }

    @Resource
    private OssService ossService;

    @PostMapping("/api/upload/avatar")
    public R<Map<String, String>> uploadAvatar(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return R.fail(401, "请先登录");
        }
        if (file.isEmpty()) {
            return R.fail(400, "文件为空");
        }
        try {
            Long userId = Long.valueOf(auth.getPrincipal().toString());
            String url = ossService.uploadAvatar(file, userId);
            Map<String, String> data = new HashMap<>();
            data.put("url", url);
            SysUser user = sysUserMapper.selectById(userId);
            if (user != null && !url.equals(user.getAvatar())) {
                user.setAvatar(url);
                sysUserMapper.updateById(user);
            }
            return R.ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return R.fail(500, "上传失败: " + e.getMessage());
        }
    }
}
