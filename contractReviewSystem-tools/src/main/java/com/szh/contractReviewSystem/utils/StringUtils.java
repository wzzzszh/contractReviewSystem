package com.szh.contractReviewSystem.utils;

import java.util.Collection;
import java.util.Map;
import java.util.Random;

/**
 * 字符串工具类
 */
public class StringUtils {
    
    /** 空字符串 */
    private static final String NULLSTR = "";
    
    /** 下划线 */
    private static final char SEPARATOR = '_';
    
    /**
     * 获取参数不为空
     *
     * @param value defaultValue 要判断的value
     * @return value 返回值
     */
    public static <T> T nvl(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
    
    /**
     * 判断字符串是否为空
     *
     * @param str 字符串
     * @return 是否为空
     */
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }
    
    /**
     * 判断字符串是否非空
     *
     * @param str 字符串
     * @return 是否非空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    
    /**
     * 判断集合是否为空
     *
     * @param coll 集合
     * @return 是否为空
     */
    public static boolean isEmpty(Collection<?> coll) {
        return coll == null || coll.isEmpty();
    }
    
    /**
     * 判断Map是否为空
     *
     * @param map Map
     * @return 是否为空
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }
    
    /**
     * 驼峰命名转下划线命名
     *
     * @param camelCaseName 驼峰命名
     * @return 下划线命名
     */
    public static String camelToUnderline(String camelCaseName) {
        if (isEmpty(camelCaseName)) {
            return NULLSTR;
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camelCaseName.length(); i++) {
            char c = camelCaseName.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append(SEPARATOR);
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
    /**
     * 下划线命名转驼峰命名
     *
     * @param underlineName 下划线命名
     * @return 驼峰命名
     */
    public static String underlineToCamel(String underlineName) {
        if (isEmpty(underlineName)) {
            return NULLSTR;
        }
        
        StringBuilder sb = new StringBuilder();
        boolean upperCase = false;
        for (int i = 0; i < underlineName.length(); i++) {
            char c = underlineName.charAt(i);
            
            if (c == SEPARATOR) {
                upperCase = true;
            } else if (upperCase) {
                sb.append(Character.toUpperCase(c));
                upperCase = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
    /**
     * 生成指定长度的随机字符串
     *
     * @param length 长度
     * @return 随机字符串
     */
    public static String randomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(str.length());
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }
}