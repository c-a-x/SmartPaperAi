package com.GeekPaperAssistant.service.impl;

import com.GeekPaperAssistant.service.DocumentParserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.core.exception.BusinessException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 文档解析服务实现
 * 使用 Apache Tika 自动检测和解析多种文档格式
 * 
 * @author 席崇援
 */
@Slf4j
@Service
public class DocumentParserServiceImpl implements DocumentParserService {

    private final Tika tika = new Tika();
    
    /**
     * 解析文档（自动检测格式）
     * 使用 Tika 自动识别文件类型并解析
     */
    @Override
    public String parseDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new BusinessException("文件名不能为空");
        }
        
        try {
            log.info("开始解析文档: {}", filename);
            
            // 使用 Tika 自动检测并解析
            String text = tika.parseToString(file.getInputStream());
            
            // 清理文本
            text = cleanText(text);
            
            log.info("文档解析完成: {}, 字符数={}", filename, text.length());
            return text;
            
        } catch (Exception e) {
            log.error("文档解析失败: {}", filename, e);
            throw new BusinessException("文档解析失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析 PDF 文档
     * 使用 Tika 自动解析
     */
    @Override
    public String parsePDF(MultipartFile file) {
        return parseDocument(file);
    }
    
    /**
     * 解析 Word 文档
     * 使用 Tika 自动解析
     */
    @Override
    public String parseWord(MultipartFile file) {
        return parseDocument(file);
    }
    
    /**
     * 解析 Markdown 文档
     * 使用 Tika 自动解析
     */
    @Override
    public String parseMarkdown(MultipartFile file) {
        return parseDocument(file);
    }
    
    /**
     * 解析文本文档
     * 使用 Tika 自动解析
     */
    @Override
    public String parseText(MultipartFile file) {
        return parseDocument(file);
    }
    
    /**
     * 清理文本
     * - 移除多余空白
     * - 标准化换行
     * - 移除特殊字符
     */
    private String cleanText(String text) {
        if (text == null) {
            return "";
        }
        
        return text
            // 移除特殊控制字符（保留换行符和制表符）
            .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
            // 移除多余的空行（保留最多两个连续换行）
            .replaceAll("\\n{3,}", "\n\n")
            // 移除行首行尾空格和制表符
            .replaceAll("(?m)^[\\s\\t]+|[\\s\\t]+$", "")
            // 标准化多个空格为单个空格
            .replaceAll("[\\s\\t]{2,}", " ")
            // 移除多余的制表符
            .replaceAll("\\t+", " ")
            // 最终trim
            .trim();
    }
    
    /**
     * 从字节数组解析 PDF
     * 使用 Tika 自动解析
     */
    @Override
    public String parsePDFFromBytes(byte[] fileData) {
        return parseFromBytes(fileData, "application/pdf");
    }
    
    /**
     * 从字节数组解析 Word
     * 使用 Tika 自动解析
     */
    @Override
    public String parseWordFromBytes(byte[] fileData) {
        return parseFromBytes(fileData, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }
    
    /**
     * 从字节数组解析文本
     * 使用 Tika 自动解析
     */
    @Override
    public String parseTextFromBytes(byte[] fileData) {
        return parseFromBytes(fileData, "text/plain");
    }
    
    /**
     * 从字节数组解析 Markdown
     * 使用 Tika 自动解析
     */
    @Override
    public String parseMarkdownFromBytes(byte[] fileData) {
        return parseFromBytes(fileData, "text/markdown");
    }

    /**
     * 通用字节数组解析方法
     * 使用 Tika 自动检测并解析
     *
     * @param fileData 文件字节数组
     * @param mimeType 文件 MIME 类型（可选，用于提示）
     * @return 解析后的文本
     */
    private String parseFromBytes(byte[] fileData, String mimeType) {
        try {
            log.info("开始从字节数组解析文档, 大小={} bytes, MIME={}", fileData.length, mimeType);
            
            try (InputStream inputStream = new ByteArrayInputStream(fileData)) {
                String text = tika.parseToString(inputStream);
                text = cleanText(text);
                
                log.info("文档解析完成, 字符数={}", text.length());
                return text;
            }
        } catch (Exception e) {
            log.error("文档字节数组解析失败", e);
            throw new BusinessException("文档解析失败: " + e.getMessage());
        }
    }
}

