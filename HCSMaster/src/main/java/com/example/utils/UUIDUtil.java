package com.example.utils;


import java.util.UUID;

/**
 * UUID工具类
 *
 * @author: pcy
 * @ClassName: UUIDUtil
 */
public class UUIDUtil {

    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}