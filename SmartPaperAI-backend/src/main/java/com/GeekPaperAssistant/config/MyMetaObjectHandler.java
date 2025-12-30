package com.GeekPaperAssistant.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 元数据处理器
 * 用于自动填充创建时间、更新时间、创建者、更新者等字段
 *
 * @author 席崇援
 * @since 2025-10-03
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 创建时间
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        // 更新时间
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        // 创建者
        this.strictInsertFill(metaObject, "createBy", String.class, "system");
        // 更新者
        this.strictInsertFill(metaObject, "updateBy", String.class, "system");
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新时间
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        // 更新者
        this.strictUpdateFill(metaObject, "updateBy", String.class, "system");
    }
}
