package com.GeekPaperAssistant.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

/**
 * 论文对比请求 DTO
 * 
 * @author ican
 */
@Data
@Schema(description = "论文对比请求")
public class PaperComparisonDTO {

    @NotEmpty(message = "文档ID列表不能为空")
    @Size(min = 2, max = 10, message = "对比文档数量应在2-10之间")
    @Schema(description = "文档ID列表")
    private List<Long> documentIds;

    @Schema(description = "对比维度（可选，默认：研究方法、数据集、创新点、结论）")
    private List<String> dimensions;
}
