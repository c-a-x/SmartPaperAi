
package com.GeekPaperAssistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.GeekPaperAssistant.model.entity.ChatMessageDO;

/**
 * AI 聊天消息 Mapper
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessageDO> {
}
