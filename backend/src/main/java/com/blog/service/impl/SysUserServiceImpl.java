package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.dto.LoginDto;
import com.blog.dto.RegisterDto;
import com.blog.entity.SysUser;
import com.blog.mapper.SysUserMapper;
import com.blog.service.SysUserService;
import com.blog.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
public class SysUserServiceImpl implements SysUserService {

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private JwtUtil jwtUtil;

    @Override
    public Map<String, Object> login(LoginDto loginDto) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, loginDto.getUsername())
        );
        if (user == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname() != null ? user.getNickname() : user.getUsername());
        userInfo.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");
        userInfo.put("email", user.getEmail() != null ? user.getEmail() : "");
        userInfo.put("role", user.getRole());
        result.put("user", userInfo);
        return result;
    }

    @Override
    public void register(RegisterDto registerDto) {
        SysUser exist = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, registerDto.getUsername())
        );
        if (exist != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        // 检查邮箱是否已注册
        SysUser emailExist = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getEmail, registerDto.getEmail())
        );
        if (emailExist != null) {
            throw new IllegalArgumentException("该邮箱已被注册");
        }

        SysUser user = new SysUser();
        user.setUsername(registerDto.getUsername());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        user.setNickname(registerDto.getUsername());
        user.setPhone(registerDto.getPhone() != null ? registerDto.getPhone() : "");
        user.setEmail(registerDto.getEmail());
        user.setEmailVerified(1);
        user.setRole("user");
        sysUserMapper.insert(user);
    }
}
