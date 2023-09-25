package com.example.service;

import com.example.pojo.Goods;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.vo.GoodsVo;

import java.util.List;

/**
* @author pcy
* @description 针对表【t_goods】的数据库操作Service
* @createDate 2023-09-17 16:32:07
*/
public interface GoodsService extends IService<Goods> {

    // 获取商品列表
    List<GoodsVo> findGoodsVO();
    // 商品详情
    GoodsVo findGoodsVOByGoodsId(Long goodsId);
}
