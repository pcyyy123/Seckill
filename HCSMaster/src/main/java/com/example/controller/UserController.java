package com.example.controller;

import com.example.pojo.User;
import com.example.rabbitmq.MQSender;
import com.example.service.UserService;
import com.example.vo.RespBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private MQSender mqSender;

    @RequestMapping("/info")
    @ResponseBody
    public RespBean info(User user){
        return RespBean.success(user);
    }
    @RequestMapping("/insert")
    public String insert(){
        userService.insert();
        return "success";
    }

    // 测试RabbitMQ，发送消息
//    @RequestMapping("/mq")
//    @ResponseBody
//    public void mq(){
//        mqSender.sendSeckillMessage("hello");
//    }


}
