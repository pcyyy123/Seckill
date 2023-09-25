package com.example.controller;

import com.example.pojo.User;
import com.example.service.GoodsService;
import com.example.service.UserService;
import com.example.vo.DetailVo;
import com.example.vo.GoodsVo;
import com.example.vo.RespBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/goods")
public class GoodsController {
    @Autowired
    private UserService userService;
    @Autowired
    private GoodsService goodsService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ThymeleafViewResolver thymeleafViewResolver;
    @Autowired
    private TemplateEngine templateEngine;


//     跳转到商品列表页(原始)
    @RequestMapping("/toList1")
    public String toList1(Model model, User user){
        model.addAttribute("user", user);
        model.addAttribute("goodsList", goodsService.findGoodsVO());
        return "goodsList";
    }


    // 跳转到商品列表页
    @RequestMapping(value = "/toList", produces = "text/html;charset=utf-8")
    @ResponseBody
    public String toList(Model model,User user) {
        // 尝试从缓存中获取页面
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        String cachedHtml = valueOperations.get("goodsList");

        if (cachedHtml != null) {
            // 如果缓存中存在，直接返回缓存的HTML页面
            return cachedHtml;
        }

        // 创建Thymeleaf上下文
        Context thymeleafContext = new Context();

        // 添加要在模板中使用的变量
        thymeleafContext.setVariable("user", user);
        thymeleafContext.setVariable("goodsList", goodsService.findGoodsVO()); // 替换为你的商品数据

        // 渲染HTML页面
        String renderedHtml = templateEngine.process("goodsList", thymeleafContext);

        // 将渲染后的HTML页面存储到Redis缓存中，设置过期时间（例如60分钟）
        valueOperations.set("goodsList", renderedHtml, 60, TimeUnit.MINUTES);

        // 返回渲染后的HTML页面
        return renderedHtml;
    }

    // 商品详情
    @RequestMapping("/toDetail2/{goodsId}")
    public String toDetail2(Model model, User user, @PathVariable Long goodsId){
        model.addAttribute("user", user);
        GoodsVo goodsVo = goodsService.findGoodsVOByGoodsId(goodsId);
        Date startDate = goodsVo.getStartDate();
        Date endDate = goodsVo.getEndDate();
        Date nowDate = new Date();
        //秒杀状态
        int seckillStatus = 0;
        //秒杀倒计时
        int remainSeconds = 0;

        if (nowDate.before(startDate)) {
            //秒杀还未开始0
            remainSeconds = (int) ((startDate.getTime() - nowDate.getTime()) / 1000);
        } else if (nowDate.after(endDate)) {
            //秒杀已经结束
            seckillStatus = 2;
            remainSeconds = -1;
        } else {
            //秒杀进行中
            seckillStatus = 1;
            remainSeconds = 0;
        }
        model.addAttribute("remainSeconds", remainSeconds);
        model.addAttribute("goods", goodsVo);
        model.addAttribute("secKillStatus", seckillStatus);
        return "goodsDetail";
    }

    @RequestMapping("/toDetail/{goodsId}")
    @ResponseBody
    public RespBean toDetail(Model model, User user, @PathVariable Long goodsId){
        GoodsVo goodsVo = goodsService.findGoodsVOByGoodsId(goodsId);
        Date startDate = goodsVo.getStartDate();
        Date endDate = goodsVo.getEndDate();
        Date nowDate = new Date();
        //秒杀状态
        int seckillStatus = 0;
        //秒杀倒计时
        int remainSeconds = 0;

        if (nowDate.before(startDate)) {
            //秒杀还未开始0
            remainSeconds = (int) ((startDate.getTime() - nowDate.getTime()) / 1000);
        } else if (nowDate.after(endDate)) {
            //秒杀已经结束
            seckillStatus = 2;
            remainSeconds = -1;
        } else {
            //秒杀进行中
            seckillStatus = 1;
            remainSeconds = 0;
        }
        DetailVo detailVo = new DetailVo(user,goodsVo,seckillStatus,remainSeconds);
        return RespBean.success(detailVo);
    }

}
