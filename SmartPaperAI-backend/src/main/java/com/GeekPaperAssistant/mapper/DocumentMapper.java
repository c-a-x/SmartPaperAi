package com.GeekPaperAssistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.GeekPaperAssistant.model.entity.DocumentDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档 Mapper
 * 
 * @author 席崇援
 * @since 2025-10-06
 */
@Mapper
public interface DocumentMapper extends BaseMapper<DocumentDO> {
}

