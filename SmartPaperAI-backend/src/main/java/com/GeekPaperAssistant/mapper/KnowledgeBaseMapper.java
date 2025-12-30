package com.GeekPaperAssistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.GeekPaperAssistant.model.entity.KnowledgeBaseDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库 Mapper
 * 
 * @author ican
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBaseDO> {
}
