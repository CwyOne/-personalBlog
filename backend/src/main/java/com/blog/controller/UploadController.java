package com.blog.controller;

import com.blog.common.R;
import com.blog.service.OssService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@RestController
public class UploadController {

    @Resource
    private OssService ossService;

    @PostMapping("/api/upload/image")
    public R<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.fail(400, "文件为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return R.fail(400, "仅支持图片格式");
        }
        try {
            String url = ossService.upload(file, "blog");
            Map<String, String> data = new HashMap<>();
            data.put("url", url);
            return R.ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return R.fail(500, "上传失败: " + e.getMessage());
        }
    }
}
