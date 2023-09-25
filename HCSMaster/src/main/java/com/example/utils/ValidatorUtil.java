package com.example.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 手机号码校验类
 */
public class ValidatorUtil {


    private static final Pattern mobile_patten = Pattern.compile("[1]([3-9])[0-9]{9}$");

    /**
     * 手机号码校验
     * @author pcy
     * @operation add
     * @param mobile
     * @return boolean
     **/
    public static boolean isMobile(String mobile) {
        if (StringUtils.isEmpty(mobile)) {
            return false;
        }
        Matcher matcher = mobile_patten.matcher(mobile);
        return matcher.matches();
    }
}
