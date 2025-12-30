package com.GeekPaperAssistant.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.GeekPaperAssistant.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.core.exception.BusinessException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 文件存储服务实现 - 本地存储版本
 * 后续可替换为 MinIO 实现
 * 
 * @author 席崇援
 */
@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {
    
    @Value("${file.storage.base-path:./uploads}")
    private String basePath;
    
    @Override
    public String uploadFile(MultipartFile file, String bucket) {
        try {
            // 1. 验证文件
            if (file == null || file.isEmpty()) {
                throw new BusinessException("文件不能为空");
            }
            
            // 2. 验证bucket名称
            if (StrUtil.isBlank(bucket) || !bucket.matches("^[a-zA-Z0-9_-]+$")) {
                throw new BusinessException("无效的存储桶名称");
            }
            
            // 3. 创建目录
            Path bucketPath = Paths.get(basePath, bucket);
            if (!Files.exists(bucketPath)) {
                Files.createDirectories(bucketPath);
            }
            
            // 4. 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            if (StrUtil.isBlank(originalFilename)) {
                throw new BusinessException("文件名不能为空");
            }
            
            // 验证文件扩展名
            int lastDotIndex = originalFilename.lastIndexOf(".");
            if (lastDotIndex == -1 || lastDotIndex == originalFilename.length() - 1) {
                throw new BusinessException("文件缺少扩展名");
            }
            
            String extension = originalFilename.substring(lastDotIndex);
            // 验证扩展名安全性
            if (!extension.matches("^\\.[a-zA-Z0-9]+$")) {
                throw new BusinessException("无效的文件扩展名");
            }
            
            String filename = IdUtil.fastSimpleUUID() + extension;
            
            // 5. 保存文件
            Path filePath = bucketPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // 6. 返回相对路径
            String fileUrl = bucket + "/" + filename;
            
            log.info("文件上传成功: originalName={}, fileUrl={}, size={}", originalFilename, fileUrl, file.getSize());
            return fileUrl;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }
    
    @Override
    public byte[] downloadFile(String fileUrl) {
        try {
            // 验证文件路径安全性
            if (StrUtil.isBlank(fileUrl)) {
                throw new BusinessException("文件路径不能为空");
            }
            
            // 防止路径遍历攻击
            if (fileUrl.contains("..") || fileUrl.contains("\\") || fileUrl.startsWith("/")) {
                throw new BusinessException("无效的文件路径");
            }
            
            Path filePath = Paths.get(basePath, fileUrl).normalize();
            
            // 确保文件路径在基础目录内
            Path basePathNormalized = Paths.get(basePath).normalize();
            if (!filePath.startsWith(basePathNormalized)) {
                throw new BusinessException("文件路径超出允许范围");
            }
            
            if (!Files.exists(filePath)) {
                throw new BusinessException("文件不存在: " + fileUrl);
            }
            
            if (!Files.isRegularFile(filePath)) {
                throw new BusinessException("路径不是文件: " + fileUrl);
            }
            
            byte[] data = Files.readAllBytes(filePath);
            
            log.debug("文件下载成功: fileUrl={}, size={}", fileUrl, data.length);
            return data;
        } catch (IOException e) {
            log.error("文件下载失败: fileUrl={}", fileUrl, e);
            throw new BusinessException("文件下载失败: " + e.getMessage());
        }
    }
    
    @Override
    public void deleteFile(String fileUrl) {
        try {
            // 验证文件路径安全性
            if (StrUtil.isBlank(fileUrl)) {
                throw new BusinessException("文件路径不能为空");
            }
            
            // 防止路径遍历攻击
            if (fileUrl.contains("..") || fileUrl.contains("\\") || fileUrl.startsWith("/")) {
                throw new BusinessException("无效的文件路径");
            }
            
            Path filePath = Paths.get(basePath, fileUrl).normalize();
            
            // 确保文件路径在基础目录内
            Path basePathNormalized = Paths.get(basePath).normalize();
            if (!filePath.startsWith(basePathNormalized)) {
                throw new BusinessException("文件路径超出允许范围");
            }
            
            if (Files.exists(filePath)) {
                if (Files.isRegularFile(filePath)) {
                    Files.delete(filePath);
                    log.info("文件删除成功: fileUrl={}", fileUrl);
                } else {
                    log.warn("路径不是文件,无法删除: fileUrl={}", fileUrl);
                }
            } else {
                log.warn("文件不存在,无需删除: fileUrl={}", fileUrl);
            }
        } catch (IOException e) {
            log.error("文件删除失败: fileUrl={}", fileUrl, e);
            throw new BusinessException("文件删除失败: " + e.getMessage());
        }
    }
}

