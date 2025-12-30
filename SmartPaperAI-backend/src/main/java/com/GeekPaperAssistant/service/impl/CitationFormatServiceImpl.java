package com.GeekPaperAssistant.service.impl;

import com.GeekPaperAssistant.model.vo.PaperMetadataVO;
import com.GeekPaperAssistant.service.CitationFormatService;
import com.GeekPaperAssistant.service.PaperAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 引用格式服务实现
 * 
 * @author 席崇援
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CitationFormatServiceImpl implements CitationFormatService {

    private final PaperAnalysisService paperAnalysisService;

    @Override
    public String generateBibTeX(PaperMetadataVO metadata) {
        if (metadata == null) {
            return "";
        }
        
        StringBuilder bibtex = new StringBuilder();
        
        // 生成引用键（作者姓氏+年份）
        String citationKey = generateCitationKey(metadata);
        
        bibtex.append("@article{").append(citationKey).append(",\n");
        
        if (metadata.getTitle() != null && !metadata.getTitle().isBlank()) {
            bibtex.append("  title={").append(metadata.getTitle()).append("},\n");
        }
        
        if (metadata.getAuthors() != null && !metadata.getAuthors().isEmpty()) {
            String authors = String.join(" and ", metadata.getAuthors());
            bibtex.append("  author={").append(authors).append("},\n");
        }
        
        if (metadata.getYear() != null && !metadata.getYear().isBlank()) {
            bibtex.append("  year={").append(metadata.getYear()).append("},\n");
        }
        
        if (metadata.getPublication() != null && !metadata.getPublication().isBlank()) {
            bibtex.append("  journal={").append(metadata.getPublication()).append("},\n");
        }
        
        if (metadata.getVolume() != null && !metadata.getVolume().isBlank()) {
            bibtex.append("  volume={").append(metadata.getVolume()).append("},\n");
        }
        
        if (metadata.getIssue() != null && !metadata.getIssue().isBlank()) {
            bibtex.append("  number={").append(metadata.getIssue()).append("},\n");
        }
        
        if (metadata.getPages() != null && !metadata.getPages().isBlank()) {
            bibtex.append("  pages={").append(metadata.getPages()).append("},\n");
        }
        
        if (metadata.getDoi() != null && !metadata.getDoi().isBlank()) {
            bibtex.append("  doi={").append(metadata.getDoi()).append("},\n");
        }
        
        bibtex.append("}\n");
        
        return bibtex.toString();
    }
    
    @Override
    public String generateAPA(PaperMetadataVO metadata) {
        if (metadata == null) {
            return "";
        }
        
        StringBuilder apa = new StringBuilder();
        
        // 作者 (年份). 标题. 期刊名, 卷(期), 页码. DOI
        
        // 作者
        if (metadata.getAuthors() != null && !metadata.getAuthors().isEmpty()) {
            List<String> formattedAuthors = new ArrayList<>();
            for (int i = 0; i < metadata.getAuthors().size(); i++) {
                String author = metadata.getAuthors().get(i);
                if (i == metadata.getAuthors().size() - 1 && metadata.getAuthors().size() > 1) {
                    formattedAuthors.add("& " + author);
                } else {
                    formattedAuthors.add(author);
                }
            }
            apa.append(String.join(", ", formattedAuthors));
        }
        
        // 年份
        if (metadata.getYear() != null && !metadata.getYear().isBlank()) {
            apa.append(" (").append(metadata.getYear()).append(")");
        }
        apa.append(". ");
        
        // 标题
        if (metadata.getTitle() != null && !metadata.getTitle().isBlank()) {
            apa.append(metadata.getTitle()).append(". ");
        }
        
        // 期刊名
        if (metadata.getPublication() != null && !metadata.getPublication().isBlank()) {
            apa.append("*").append(metadata.getPublication()).append("*");
        }
        
        // 卷期
        if (metadata.getVolume() != null && !metadata.getVolume().isBlank()) {
            apa.append(", ").append(metadata.getVolume());
            if (metadata.getIssue() != null && !metadata.getIssue().isBlank()) {
                apa.append("(").append(metadata.getIssue()).append(")");
            }
        }
        
        // 页码
        if (metadata.getPages() != null && !metadata.getPages().isBlank()) {
            apa.append(", ").append(metadata.getPages());
        }
        
        apa.append(".");
        
        // DOI
        if (metadata.getDoi() != null && !metadata.getDoi().isBlank()) {
            apa.append(" https://doi.org/").append(metadata.getDoi());
        }
        
        return apa.toString();
    }
    
    @Override
    public String generateMLA(PaperMetadataVO metadata) {
        if (metadata == null) {
            return "";
        }
        
        StringBuilder mla = new StringBuilder();
        
        // 作者姓, 名. "标题." 期刊名 卷.期 (年份): 页码. Web.
        
        // 作者
        if (metadata.getAuthors() != null && !metadata.getAuthors().isEmpty()) {
            mla.append(metadata.getAuthors().get(0));
            if (metadata.getAuthors().size() > 1) {
                mla.append(", et al");
            }
        }
        mla.append(". ");
        
        // 标题
        if (metadata.getTitle() != null && !metadata.getTitle().isBlank()) {
            mla.append("\"").append(metadata.getTitle()).append(".\" ");
        }
        
        // 期刊名
        if (metadata.getPublication() != null && !metadata.getPublication().isBlank()) {
            mla.append("*").append(metadata.getPublication()).append("*");
        }
        
        // 卷期
        if (metadata.getVolume() != null && !metadata.getVolume().isBlank()) {
            mla.append(" ").append(metadata.getVolume());
            if (metadata.getIssue() != null && !metadata.getIssue().isBlank()) {
                mla.append(".").append(metadata.getIssue());
            }
        }
        
        // 年份
        if (metadata.getYear() != null && !metadata.getYear().isBlank()) {
            mla.append(" (").append(metadata.getYear()).append(")");
        }
        
        // 页码
        if (metadata.getPages() != null && !metadata.getPages().isBlank()) {
            mla.append(": ").append(metadata.getPages());
        }
        
        mla.append(". Web.");
        
        return mla.toString();
    }
    
    @Override
    public String generateGBT7714(PaperMetadataVO metadata) {
        if (metadata == null) {
            return "";
        }
        
        StringBuilder gbt = new StringBuilder();
        
        // 作者. 标题[J]. 期刊名, 年份, 卷(期): 页码.
        
        // 作者
        if (metadata.getAuthors() != null && !metadata.getAuthors().isEmpty()) {
            gbt.append(String.join(", ", metadata.getAuthors()));
        }
        gbt.append(". ");
        
        // 标题
        if (metadata.getTitle() != null && !metadata.getTitle().isBlank()) {
            gbt.append(metadata.getTitle()).append("[J]");
        }
        gbt.append(". ");
        
        // 期刊名
        if (metadata.getPublication() != null && !metadata.getPublication().isBlank()) {
            gbt.append(metadata.getPublication());
        }
        gbt.append(", ");
        
        // 年份
        if (metadata.getYear() != null && !metadata.getYear().isBlank()) {
            gbt.append(metadata.getYear());
        }
        
        // 卷期
        if (metadata.getVolume() != null && !metadata.getVolume().isBlank()) {
            gbt.append(", ").append(metadata.getVolume());
            if (metadata.getIssue() != null && !metadata.getIssue().isBlank()) {
                gbt.append("(").append(metadata.getIssue()).append(")");
            }
        }
        
        // 页码
        if (metadata.getPages() != null && !metadata.getPages().isBlank()) {
            gbt.append(": ").append(metadata.getPages());
        }
        
        gbt.append(".");
        
        return gbt.toString();
    }
    
    @Override
    public List<String> batchExportCitations(List<Long> documentIds, String format) {
        log.info("批量导出引用: documentIds={}, format={}", documentIds, format);
        
        return documentIds.stream()
            .map(documentId -> {
                try {
                    PaperMetadataVO metadata = paperAnalysisService.extractPaperMetadata(documentId);
                    return formatCitation(metadata, format);
                } catch (Exception e) {
                    log.error("导出引用失败: documentId={}", documentId, e);
                    return null;
                }
            })
            .filter(citation -> citation != null && !citation.isBlank())
            .collect(Collectors.toList());
    }
    
    /**
     * 根据格式类型生成引用
     */
    private String formatCitation(PaperMetadataVO metadata, String format) {
        if (metadata == null) {
            return "";
        }
        
        return switch (format.toLowerCase()) {
            case "bibtex", "bib" -> generateBibTeX(metadata);
            case "apa" -> generateAPA(metadata);
            case "mla" -> generateMLA(metadata);
            case "gbt7714", "gb" -> generateGBT7714(metadata);
            default -> generateAPA(metadata); // 默认使用 APA
        };
    }
    
    /**
     * 生成引用键
     */
    private String generateCitationKey(PaperMetadataVO metadata) {
        StringBuilder key = new StringBuilder();
        
        // 第一作者姓氏（简化处理）
        if (metadata.getAuthors() != null && !metadata.getAuthors().isEmpty()) {
            String firstAuthor = metadata.getAuthors().get(0);
            String[] parts = firstAuthor.split("\\s+");
            key.append(parts[parts.length - 1].replaceAll("[^a-zA-Z]", ""));
        } else {
            key.append("unknown");
        }
        
        // 年份
        if (metadata.getYear() != null && !metadata.getYear().isBlank()) {
            key.append(metadata.getYear());
        }
        
        return key.toString().toLowerCase();
    }
}
