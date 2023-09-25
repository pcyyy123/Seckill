package com.example.service;

import com.example.pojo.Order;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.pojo.User;
import com.example.vo.GoodsVo;
import com.example.vo.OrderDetailVo;

/**
* @author pcy
* @description 针对表【t_order】的数据库操作Service
* @createDate 2023-09-17 16:32:07
*/
public interface OrderService extends IService<Order> {

    Order seckill(User user, GoodsVo goods);

    OrderDetailVo detail(Long orderId);

    String createPath(User user, Long goodsId);

    boolean checkPath(User user, Long goodsId, String path);

    boolean checkCaptcha(User user, Long goodsId, String captcha);
}
