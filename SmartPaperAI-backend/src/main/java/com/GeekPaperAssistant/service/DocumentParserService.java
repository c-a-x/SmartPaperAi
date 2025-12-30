package com.GeekPaperAssistant.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文档解析服务接口
 * 
 * @author 席崇援
 */
public interface DocumentParserService {
    
    /**
     * 解析文档内容
     * 
     * @param file 文件
     * @return 文档文本内容
     */
    String parseDocument(MultipartFile file);
    
    /**
     * 解析PDF文档
     * 
     * @param file PDF文件
     * @return 文本内容
     */
    String parsePDF(MultipartFile file);
    
    /**
     * 解析Word文档
     * 
     * @param file Word文件
     * @return 文本内容
     */
    String parseWord(MultipartFile file);
    
    /**
     * 解析Markdown文档
     * 
     * @param file Markdown文件
     * @return 文本内容
     */
    String parseMarkdown(MultipartFile file);
    
    /**
     * 解析纯文本文档
     * 
     * @param file 文本文件
     * @return 文本内容
     */
    String parseText(MultipartFile file);
    
    /**
     * 从字节数组解析PDF文档
     * 
     * @param fileData PDF文件字节数组
     * @return 文本内容
     */
    String parsePDFFromBytes(byte[] fileData);
    
    /**
     * 从字节数组解析Word文档
     * 
     * @param fileData Word文件字节数组
     * @return 文本内容
     */
    String parseWordFromBytes(byte[] fileData);
    
    /**
     * 从字节数组解析纯文本文档
     * 
     * @param fileData 文本文件字节数组
     * @return 文本内容
     */
    String parseTextFromBytes(byte[] fileData);
    
    /**
     * 从字节数组解析Markdown文档
     * 
     * @param fileData Markdown文件字节数组
     * @return 文本内容
     */
    String parseMarkdownFromBytes(byte[] fileData);
}

