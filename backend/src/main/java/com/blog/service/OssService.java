package com.blog.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class OssService {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    private OSS ossClient;

    @PostConstruct
    public void init() {
        ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }

    /**
     * 上传文章配图等通用图片（按日期+UUID命名，每次唯一）
     */
    public String upload(MultipartFile file, String dir) throws Exception {
        String ext = getExt(file);
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String objectName = dir + "/" + datePath + "/" + UUID.randomUUID().toString().replace("-", "") + ext;
        return putObject(objectName, file);
    }

    /**
     * 上传用户头像（固定文件名：avatar/{userId}.{ext}，覆盖旧头像）
     */
    public String uploadAvatar(MultipartFile file, Long userId) throws Exception {
        String ext = getExt(file);
        String objectName = "avatar/user_" + userId + ext;
        return putObject(objectName, file);
    }

    /**
     * 通过 InputStream 上传文件（用于 DOCX 内嵌图片等场景）
     */
    public String upload(InputStream inputStream, String filename, String dir) throws Exception {
        String ext = "";
        if (filename != null && filename.contains(".")) {
            ext = filename.substring(filename.lastIndexOf("."));
        }
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String objectName = dir + "/" + datePath + "/" + UUID.randomUUID().toString().replace("-", "") + ext;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setObjectAcl(CannedAccessControlList.PublicRead);
        ossClient.putObject(bucketName, objectName, inputStream, metadata);
        return "https://" + bucketName + "." + endpoint + "/" + objectName;
    }

    private String getExt(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            return originalName.substring(originalName.lastIndexOf("."));
        }
        return "";
    }

    private String putObject(String objectName, MultipartFile file) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setObjectAcl(CannedAccessControlList.PublicRead);
            ossClient.putObject(bucketName, objectName, inputStream, metadata);
        }
        return "https://" + bucketName + "." + endpoint + "/" + objectName;
    }
}
