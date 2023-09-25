package com.example.mapper;

import com.example.pojo.Goods;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.vo.GoodsVo;

import java.util.List;

/**
* @author pcy
* @description 针对表【t_goods】的数据库操作Mapper
* @createDate 2023-09-17 16:32:07
* @Entity com.example.pojo.Goods
*/
public interface GoodsMapper extends BaseMapper<Goods> {

    List<GoodsVo> findGoodsVO();

    GoodsVo findGoodsVOByGoodsId(Long goodsId);
}




