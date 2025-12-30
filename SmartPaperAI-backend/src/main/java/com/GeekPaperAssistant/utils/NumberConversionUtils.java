package com.GeekPaperAssistant.utils;

import cn.hutool.core.codec.Base62;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * 数字转换工具类
 * 使用 Apache Commons Lang NumberUtils 替代手写解析逻辑
 * 
 * 功能：
 * - 安全地将字符串转换为 Long/Integer
 * - 支持纯数字和 Base62 编码
 * - 提供默认值fallback
 * - 不抛出异常，返回 null 或默认值
 * 
 * @author 席崇援
 */
@Slf4j
public class NumberConversionUtils {

    /**
     * 将字符串转换为 Long
     * 支持纯数字和 Base62 编码
     * 
     * @param input 输入字符串
     * @return Long 值，失败返回 null
     */
    public static Long toLong(String input) {
        return toLong(input, null);
    }

    /**
     * 将字符串转换为 Long（带默认值）
     * 
     * @param input        输入字符串
     * @param defaultValue 默认值
     * @return Long 值，失败返回默认值
     */
    public static Long toLong(String input, Long defaultValue) {
        if (StringUtils.isBlank(input)) {
            return defaultValue;
        }

        String trimmed = input.trim();

        // 1. 尝试纯数字解析（包括科学计数法）
        if (NumberUtils.isCreatable(trimmed)) {
            try {
                return NumberUtils.createNumber(trimmed).longValue();
            } catch (Exception e) {
                log.warn("数字解析失败: {}", trimmed, e);
            }
        }

        // 2. 尝试 Base62 解码
        try {
            byte[] decoded = Base62.decode(trimmed);
            String decodedStr = new String(decoded);
            
            if (NumberUtils.isDigits(decodedStr)) {
                return Long.parseLong(decodedStr);
            }
        } catch (Exception e) {
            log.debug("Base62 解码失败: {}", trimmed);
        }

        log.warn("无法转换为 Long: {}", input);
        return defaultValue;
    }

    /**
     * 将字符串转换为 Integer
     * 
     * @param input 输入字符串
     * @return Integer 值，失败返回 null
     */
    public static Integer toInteger(String input) {
        return toInteger(input, null);
    }

    /**
     * 将字符串转换为 Integer（带默认值）
     * 
     * @param input        输入字符串
     * @param defaultValue 默认值
     * @return Integer 值，失败返回默认值
     */
    public static Integer toInteger(String input, Integer defaultValue) {
        if (StringUtils.isBlank(input)) {
            return defaultValue;
        }

        String trimmed = input.trim();

        // 1. 尝试纯数字解析
        if (NumberUtils.isCreatable(trimmed)) {
            try {
                return NumberUtils.createNumber(trimmed).intValue();
            } catch (Exception e) {
                log.warn("整数解析失败: {}", trimmed, e);
            }
        }

        // 2. 尝试 Base62 解码
        try {
            byte[] decoded = Base62.decode(trimmed);
            String decodedStr = new String(decoded);
            
            if (NumberUtils.isDigits(decodedStr)) {
                return Integer.parseInt(decodedStr);
            }
        } catch (Exception e) {
            log.debug("Base62 解码失败: {}", trimmed);
        }

        log.warn("无法转换为 Integer: {}", input);
        return defaultValue;
    }

    /**
     * 将字符串转换为 Double
     * 
     * @param input 输入字符串
     * @return Double 值，失败返回 null
     */
    public static Double toDouble(String input) {
        return toDouble(input, null);
    }

    /**
     * 将字符串转换为 Double（带默认值）
     * 
     * @param input        输入字符串
     * @param defaultValue 默认值
     * @return Double 值，失败返回默认值
     */
    public static Double toDouble(String input, Double defaultValue) {
        if (StringUtils.isBlank(input)) {
            return defaultValue;
        }

        String trimmed = input.trim();

        if (NumberUtils.isCreatable(trimmed)) {
            try {
                return NumberUtils.createDouble(trimmed);
            } catch (Exception e) {
                log.warn("浮点数解析失败: {}", trimmed, e);
            }
        }

        log.warn("无法转换为 Double: {}", input);
        return defaultValue;
    }

    /**
     * 检查字符串是否为有效数字
     * 
     * @param input 输入字符串
     * @return 是否为数字
     */
    public static boolean isNumeric(String input) {
        if (StringUtils.isBlank(input)) {
            return false;
        }
        return NumberUtils.isCreatable(input.trim());
    }

    /**
     * 检查字符串是否为纯数字（不包括科学计数法）
     * 
     * @param input 输入字符串
     * @return 是否为纯数字
     */
    public static boolean isDigits(String input) {
        if (StringUtils.isBlank(input)) {
            return false;
        }
        return NumberUtils.isDigits(input.trim());
    }

    /**
     * 安全地将 Number 转换为 Long
     * 
     * @param number 数字对象
     * @return Long 值，null 返回 null
     */
    public static Long numberToLong(Number number) {
        return number == null ? null : number.longValue();
    }

    /**
     * 安全地将 Number 转换为 Integer
     * 
     * @param number 数字对象
     * @return Integer 值，null 返回 null
     */
    public static Integer numberToInteger(Number number) {
        return number == null ? null : number.intValue();
    }
}
