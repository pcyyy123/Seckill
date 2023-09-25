package com.example.service;

import com.example.pojo.SeckillOrder;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.pojo.User;

/**
* @author pcy
* @description 针对表【t_seckill_order】的数据库操作Service
* @createDate 2023-09-17 16:32:07
*/
public interface SeckillOrderService extends IService<SeckillOrder> {

    Long getResult(User user, Long goodsId);
}
