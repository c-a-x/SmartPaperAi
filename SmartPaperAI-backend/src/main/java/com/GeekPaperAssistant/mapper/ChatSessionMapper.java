
package com.GeekPaperAssistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.GeekPaperAssistant.model.entity.ChatSessionDO;

/**
 * AI 聊天会话 Mapper
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSessionDO> {
}
