package com.blog.service;

import com.blog.dto.LoginDto;
import com.blog.dto.RegisterDto;

import java.util.Map;

public interface SysUserService {

    Map<String, Object> login(LoginDto loginDto);

    void register(RegisterDto registerDto);
}
