package com.example.tt;

import com.wf.captcha.*;
import com.wf.captcha.base.Captcha;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.awt.*;
import java.io.IOException;

@Controller
@RequestMapping("/yzm")
public class test {

    @ResponseBody
    @RequestMapping("/captcha")
    public String captcha(HttpServletRequest request) {
        // png类型
        SpecCaptcha specCaptcha = new SpecCaptcha(130, 48, 5);
        // gif类型
        GifCaptcha gifCaptcha = new GifCaptcha(130, 48);
        // 中文类型
        ChineseCaptcha chineseCaptcha = new ChineseCaptcha(130, 48);
        // 中文gif类型
        ChineseGifCaptcha chineseGifCaptcha = new ChineseGifCaptcha(130, 48);
        // 算术类型
        ArithmeticCaptcha arithmeticCaptcha = new ArithmeticCaptcha(130, 48);
        // 几位数运算，默认是两位
        arithmeticCaptcha.setLen(3);
        // 获取运算的公式：3+2=?
        arithmeticCaptcha.getArithmeticString();
        // 获取运算的结果：5
        arithmeticCaptcha.text();

        //设置验证码字符类型 只有SpecCaptcha和GifCaptcha设置才有效果。
        specCaptcha.setCharType(Captcha.TYPE_DEFAULT);
        //获取验证码正确值 方便存储redis 如是算数类型就获取到正确的值 如是中文gif就直接获取验证码内值
        String verCode = chineseCaptcha.text().toLowerCase();
        //存储验证码值到session
        HttpSession session=request.getSession();
        session.setAttribute("code",verCode);
        //返回一个base64信息 前端img显示
        return chineseCaptcha.toBase64();
    }

}
