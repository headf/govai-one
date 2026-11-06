package com.govai.enterprise.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.govai.enterprise.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件存储服务(开发阶段本地磁盘;联调/上线可换 MinIO/OSS)
 */
@Slf4j
@Service
public class FileStorageService {

    @Value("${govai.file.upload-dir}")
    private String uploadDir;

    /**
     * 保存文件,返回可访问的相对 URL(如 /files/2026/09/uuid.jpg)
     */
    public String store(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件为空");
        }
        String original = file.getOriginalFilename();
        String ext = "";
        if (StrUtil.isNotBlank(original) && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }
        // 按日期分子目录
        String dateDir = cn.hutool.core.date.DateUtil.format(cn.hutool.core.date.DateUtil.date(), "yyyy/MM");
        String storeSub = StrUtil.isBlank(subDir) ? dateDir : subDir + "/" + dateDir;
        String filename = IdUtil.fastSimpleUUID() + ext;
        String relative = "/" + storeSub + "/" + filename;
        try {
            Path dir = Paths.get(uploadDir, storeSub);
            Files.createDirectories(dir);
            file.transferTo(new File(uploadDir, storeSub).toPath().resolve(filename));
            log.info("文件已保存: {}", relative);
            return relative;
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BusinessException("文件保存失败");
        }
    }

    /**
     * 从文件 URL 解析物理路径
     */
    public Path resolve(String fileUrl) {
        return Paths.get(uploadDir, StrUtil.removePrefix(fileUrl, "/"));
    }

    /**
     * 保存文本内容(SVG 证照等)到子目录,返回相对 URL
     */
    public String storeText(String subDir, String filename, String content) {
        String dateDir = cn.hutool.core.date.DateUtil.format(cn.hutool.core.date.DateUtil.date(), "yyyy/MM");
        String storeSub = StrUtil.isBlank(subDir) ? dateDir : subDir + "/" + dateDir;
        String relative = "/" + storeSub + "/" + filename;
        try {
            Path dir = Paths.get(uploadDir, storeSub);
            Files.createDirectories(dir);
            Files.write(dir.resolve(filename), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return relative;
        } catch (IOException e) {
            log.error("文本文件写入失败", e);
            throw new BusinessException("文件写入失败");
        }
    }
}
