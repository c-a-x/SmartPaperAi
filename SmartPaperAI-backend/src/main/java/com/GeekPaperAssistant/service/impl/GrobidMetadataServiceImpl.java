package com.GeekPaperAssistant.service.impl;

import cn.hutool.core.util.StrUtil;
import com.GeekPaperAssistant.model.vo.DocumentMetadataVO;
import com.GeekPaperAssistant.service.GrobidMetadataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * GROBID 元数据解析服务实现
 * 
 * <p>GROBID 部署说明:</p>
 * <pre>
 * # Docker 快速部署
 * docker pull lfoppiano/grobid:0.8.0
 * docker run -d -p 8070:8070 lfoppiano/grobid:0.8.0
 * 
 * # 本地部署
 * git clone https://github.com/kermitt2/grobid.git
 * cd grobid
 * ./gradlew run
 * </pre>
 * 
 * @author 席崇援
 */
@Slf4j
@Service
public class GrobidMetadataServiceImpl implements GrobidMetadataService {
    
    @Value("${grobid.server.url:http://localhost:8070}")
    private String grobidServerUrl;
    
    @Value("${grobid.enabled:false}")
    private boolean grobidEnabled;
    
    private final RestTemplate restTemplate;

    public GrobidMetadataServiceImpl(@Qualifier("grobidRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    @Override
    public DocumentMetadataVO extractMetadata(MultipartFile pdfFile) {
        if (!grobidEnabled) {
            log.warn("GROBID服务未启用,返回空元数据");
            return DocumentMetadataVO.builder().build();
        }
        
        if (!isAvailable()) {
            log.error("GROBID服务不可用: {}", grobidServerUrl);
            return DocumentMetadataVO.builder().build();
        }
        
        try {
            // 1. 调用 GROBID API 获取 TEI XML
            String teiXml = callGrobidApi(pdfFile, "/api/processFulltextDocument");
            
            if (StrUtil.isBlank(teiXml)) {
                log.warn("GROBID返回空结果");
                return DocumentMetadataVO.builder().build();
            }
            
            // 2. 解析 TEI XML
            DocumentMetadataVO metadata = parseTeiXml(teiXml);
            
            log.info("GROBID元数据解析完成: title={}, authors={}, sections={}", 
                metadata.getTitle(), 
                metadata.getAuthors() != null ? metadata.getAuthors().size() : 0,
                metadata.getSections() != null ? metadata.getSections().size() : 0
            );
            
            return metadata;
            
        } catch (Exception e) {
            log.error("GROBID元数据解析失败", e);
            return DocumentMetadataVO.builder().build();
        }
    }
    
    @Override
    public DocumentMetadataVO extractMetadata(byte[] pdfData, String filename) {
        if (!grobidEnabled) {
            log.warn("GROBID服务未启用,返回空元数据");
            return DocumentMetadataVO.builder().build();
        }
        
        if (!isAvailable()) {
            log.error("GROBID服务不可用: {}", grobidServerUrl);
            return DocumentMetadataVO.builder().build();
        }
        
        try {
            // 1. 调用 GROBID API 获取 TEI XML
            String teiXml = callGrobidApi(pdfData, filename, "/api/processFulltextDocument");
            
            if (StrUtil.isBlank(teiXml)) {
                log.warn("GROBID返回空结果");
                return DocumentMetadataVO.builder().build();
            }
            
            // 2. 解析 TEI XML
            DocumentMetadataVO metadata = parseTeiXml(teiXml);
            
            log.info("GROBID元数据解析完成: title={}, authors={}, sections={}", 
                metadata.getTitle(), 
                metadata.getAuthors() != null ? metadata.getAuthors().size() : 0,
                metadata.getSections() != null ? metadata.getSections().size() : 0
            );
            
            return metadata;
            
        } catch (Exception e) {
            log.error("GROBID元数据解析失败", e);
            return DocumentMetadataVO.builder().build();
        }
    }
    
    @Override
    public String extractStructure(MultipartFile pdfFile) {
        if (!grobidEnabled || !isAvailable()) {
            return "";
        }
        
        try {
            return callGrobidApi(pdfFile, "/api/processFulltextDocument");
        } catch (Exception e) {
            log.error("提取文档结构失败", e);
            return "";
        }
    }
    
    @Override
    public boolean isAvailable() {
        if (!grobidEnabled) {
            return false;
        }
        
        try {
            String healthUrl = grobidServerUrl + "/api/isalive";
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.debug("GROBID服务不可用: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 调用 GROBID API (MultipartFile版本)
     */
    private String callGrobidApi(MultipartFile pdfFile, String endpoint) throws Exception {
        return callGrobidApi(pdfFile.getBytes(), pdfFile.getOriginalFilename(), endpoint);
    }
    
    /**
     * 调用 GROBID API (byte[]版本)
     */
    private String callGrobidApi(byte[] pdfData, String filename, String endpoint) throws Exception {
        String url = grobidServerUrl + endpoint;
        
        // 构建 multipart/form-data 请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("input", new org.springframework.core.io.ByteArrayResource(pdfData) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        
        // 发送请求
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("GROBID API调用失败: " + response.getStatusCode());
        }
        
        return response.getBody();
    }
    
    /**
     * 解析 TEI XML 格式的文档
     */
    private DocumentMetadataVO parseTeiXml(String teiXml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(teiXml.getBytes("UTF-8")));
        
        DocumentMetadataVO.DocumentMetadataVOBuilder metadata = DocumentMetadataVO.builder();
        
        // 1. 提取标题
        NodeList titleNodes = doc.getElementsByTagName("title");
        if (titleNodes.getLength() > 0) {
            Element titleElement = (Element) titleNodes.item(0);
            metadata.title(titleElement.getTextContent().trim());
        }
        
        // 2. 提取作者
        List<DocumentMetadataVO.Author> authors = extractAuthors(doc);
        metadata.authors(authors);
        
        // 3. 提取摘要
        NodeList abstractNodes = doc.getElementsByTagName("abstract");
        if (abstractNodes.getLength() > 0) {
            Element abstractElement = (Element) abstractNodes.item(0);
            metadata.abstractText(abstractElement.getTextContent().trim());
        }
        
        // 4. 提取关键词
        List<String> keywords = extractKeywords(doc);
        metadata.keywords(keywords);
        
        // 5. 提取章节结构
        List<DocumentMetadataVO.Section> sections = extractSections(doc);
        metadata.sections(sections);
        
        // 6. 提取引用数量
        NodeList biblNodes = doc.getElementsByTagName("biblStruct");
        metadata.referenceCount(biblNodes.getLength());
        
        // 7. 提取出版年份
        NodeList dateNodes = doc.getElementsByTagName("date");
        if (dateNodes.getLength() > 0) {
            Element dateElement = (Element) dateNodes.item(0);
            String when = dateElement.getAttribute("when");
            if (StrUtil.isNotBlank(when) && when.length() >= 4) {
                metadata.publicationYear(when.substring(0, 4));
            }
        }
        
        return metadata.build();
    }
    
    /**
     * 提取作者信息
     */
    private List<DocumentMetadataVO.Author> extractAuthors(Document doc) {
        List<DocumentMetadataVO.Author> authors = new ArrayList<>();
        NodeList authorNodes = doc.getElementsByTagName("author");
        
        for (int i = 0; i < authorNodes.getLength(); i++) {
            Element authorElement = (Element) authorNodes.item(i);
            
            // 提取姓名
            String name = "";
            NodeList persNameNodes = authorElement.getElementsByTagName("persName");
            if (persNameNodes.getLength() > 0) {
                Element persName = (Element) persNameNodes.item(0);
                
                String forename = getTextContent(persName, "forename");
                String surname = getTextContent(persName, "surname");
                name = (forename + " " + surname).trim();
            }
            
            // 提取机构
            String affiliation = "";
            NodeList affiliationNodes = authorElement.getElementsByTagName("affiliation");
            if (affiliationNodes.getLength() > 0) {
                Element affiliationElement = (Element) affiliationNodes.item(0);
                affiliation = affiliationElement.getTextContent().trim();
            }
            
            // 提取邮箱
            String email = "";
            NodeList emailNodes = authorElement.getElementsByTagName("email");
            if (emailNodes.getLength() > 0) {
                email = emailNodes.item(0).getTextContent().trim();
            }
            
            if (StrUtil.isNotBlank(name)) {
                authors.add(DocumentMetadataVO.Author.builder()
                    .name(name)
                    .affiliation(affiliation)
                    .email(email)
                    .build());
            }
        }
        
        return authors;
    }
    
    /**
     * 提取关键词
     */
    private List<String> extractKeywords(Document doc) {
        List<String> keywords = new ArrayList<>();
        NodeList termNodes = doc.getElementsByTagName("term");
        
        for (int i = 0; i < termNodes.getLength(); i++) {
            String keyword = termNodes.item(i).getTextContent().trim();
            if (StrUtil.isNotBlank(keyword)) {
                keywords.add(keyword);
            }
        }
        
        return keywords;
    }
    
    /**
     * 提取章节结构
     */
    private List<DocumentMetadataVO.Section> extractSections(Document doc) {
        List<DocumentMetadataVO.Section> sections = new ArrayList<>();
        NodeList divNodes = doc.getElementsByTagName("div");
        
        int position = 0;
        for (int i = 0; i < divNodes.getLength(); i++) {
            Element divElement = (Element) divNodes.item(i);
            
            // 提取章节标题
            String sectionTitle = "";
            NodeList headNodes = divElement.getElementsByTagName("head");
            if (headNodes.getLength() > 0) {
                sectionTitle = headNodes.item(0).getTextContent().trim();
            }
            
            // 提取章节内容
            String content = divElement.getTextContent().trim();
            
            // 判断层级(简化版,实际可以通过n属性判断)
            int level = 1;
            if (divElement.hasAttribute("n")) {
                String nAttr = divElement.getAttribute("n");
                level = nAttr.split("\\.").length;
            }
            
            if (StrUtil.isNotBlank(sectionTitle) && StrUtil.isNotBlank(content)) {
                int startPos = position;
                int endPos = position + content.length();
                
                sections.add(DocumentMetadataVO.Section.builder()
                    .title(sectionTitle)
                    .content(content)
                    .level(level)
                    .startPosition(startPos)
                    .endPosition(endPos)
                    .build());
                
                position = endPos;
            }
        }
        
        return sections;
    }
    
    /**
     * 辅助方法: 获取元素的文本内容
     */
    private String getTextContent(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return "";
    }
}
