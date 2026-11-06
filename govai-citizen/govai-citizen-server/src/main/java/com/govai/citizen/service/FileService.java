package com.govai.citizen.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.govai.citizen.common.BizException;
import com.govai.citizen.config.GovaiProperties;
import com.govai.citizen.dto.UploadVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地文件存储(开发阶段替代 MinIO): 文件落 uploads/{yyyyMM}/ 目录,
 * 通过 /uploads/** 静态映射访问, 第6周可替换为 MinIO 客户端。
 */
@Service
@RequiredArgsConstructor
public class FileService {

    private final GovaiProperties props;

    public UploadVO upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "文件不能为空");
        }
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }
        String sub = DateUtil.format(new java.util.Date(), "yyyyMM");
        String name = IdUtil.fastSimpleUUID() + ext;
        String relDir = "files/" + sub;
        Path dir = Paths.get(props.getUpload().getDir()).resolve(relDir).toAbsolutePath();
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(name);
            file.transferTo(target);
        } catch (IOException e) {
            throw new BizException("文件保存失败");
        }
        String url = props.getUpload().getUrlPrefix() + relDir.replace("\\", "/") + "/" + name;
        UploadVO vo = new UploadVO();
        vo.setFileUrl(url);
        vo.setFileName(original == null ? name : original);
        vo.setFileType(StrUtil.isBlank(ext) ? null : ext.replace(".", ""));
        vo.setSize(file.getSize());
        vo.setUploadTime(java.time.LocalDateTime.now());
        return vo;
    }
}
