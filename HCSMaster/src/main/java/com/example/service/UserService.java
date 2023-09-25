package com.example.service;

import com.example.pojo.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.vo.LoginVO;
import com.example.vo.RespBean;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
* @author pcy
* @description 针对表【t_user】的数据库操作Service
* @createDate 2023-09-14 16:11:43
*/
public interface UserService extends IService<User> {

    RespBean doLogin(LoginVO loginVO, HttpServletRequest request, HttpServletResponse response);
    // 根据Cookie获取用户
    User getUserByCookie(String userTicket, HttpServletRequest request, HttpServletResponse response);
    void insert();
    RespBean updatePassword(String userTicket, String password, HttpServletRequest request, HttpServletResponse response);
}
