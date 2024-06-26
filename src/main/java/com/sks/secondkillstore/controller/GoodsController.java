package com.sks.secondkillstore.controller;


import com.sks.secondkillstore.entity.User;
import com.sks.secondkillstore.service.IGoodsService;
import com.sks.secondkillstore.service.IUserService;
import com.sks.secondkillstore.vo.DetailVo;
import com.sks.secondkillstore.vo.GoodsVo;
import com.sks.secondkillstore.vo.RespBean;
import com.sks.secondkillstore.vo.RespBeanEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @Author HQD
 * @Date 2024/4/15 9:32
 * @Version 1.0
 */
@Controller
@RequestMapping("/goods")
public class GoodsController {
    @Autowired
    private IUserService userService;
    @Autowired
    private IGoodsService goodsService;
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    private ThymeleafViewResolver thymeleafViewResolver;

    /**
     * 调整商品也页面
     *
     * @param model 传输到前端
     * @param user  通过配置文件直接获取
     * @return 商品列表网页
     */
    @RequestMapping(value = "/toList", produces = "text/html;charset=utf-8")
    @ResponseBody
    public String toList(Model model, User user, HttpServletRequest request, HttpServletResponse response) {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String html = (String) valueOperations.get("goodsList");
        if (!org.thymeleaf.util.StringUtils.isEmpty(html)) {
            return html;
        }

        model.addAttribute("user", user);
        model.addAttribute("goodsList", goodsService.findGoodsVo());

        WebContext webContext = new WebContext(request, response, request.getServletContext(), request.getLocale(), model.asMap());
        html = thymeleafViewResolver.getTemplateEngine().process("goodsList", webContext);
        if (!StringUtils.isEmpty(html)) {
            valueOperations.set("goodsList", html, 60, TimeUnit.SECONDS);
        }
        return html;
    }

    /**
     * 跳转商品详情页，优化版
     *
     * @param goodsId 商品ID
     * @return 返回前端
     */
    @RequestMapping("/Detail/{goodsId}")
    @ResponseBody
    public RespBean Detail(User user, @PathVariable Long goodsId) {
        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
        if (goodsVo != null) {
            Date startDate = goodsVo.getStartDate();
            Date endDate = goodsVo.getEndDate();
            Date nowDate = new Date();
            //秒杀状态
            int secKillStatus = 0;
            //秒杀倒计时
            int remainSeconds = 0;
            //如果秒杀还未开始
            if (nowDate.before(startDate)) {
                remainSeconds = (int) ((startDate.getTime() - nowDate.getTime()) / 1000);
            } else if (nowDate.after(endDate)) {
                secKillStatus = 2;
                remainSeconds = -1;
            } else {
                secKillStatus = 1;
            }
            DetailVo detailVo = new DetailVo();
            detailVo.setUser(user);
            detailVo.setGoodsVo(goodsVo);
            detailVo.setSecKillStatus(secKillStatus);
            detailVo.setRemainSeconds(remainSeconds);
            return RespBean.success(detailVo);
        }else {
            return RespBean.error(RespBeanEnum.ERROR);
        }
    }

    /**
     * 跳转商品详情页
     *
     * @param goodsId 商品ID
     * @return 返回前端
     */
    @RequestMapping(value = "/toDetail2/{goodsId}", produces = "text/html;charset=utf-8")
    @ResponseBody
    public String toDetail2(Model model, User user, @PathVariable Long goodsId, HttpServletRequest request, HttpServletResponse response) {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String html = (String) valueOperations.get("goodsDetail:" + goodsId);
        if (!StringUtils.isEmpty(html)) {
            return html;
        }
        model.addAttribute("user", user);
        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
        Date startDate = goodsVo.getStartDate();
        Date endDate = goodsVo.getEndDate();
        Date nowDate = new Date();
        //秒杀状态
        int secKillStatus = 0;
        //秒杀倒计时
        int remainSeconds = 0;
        //如果秒杀还未开始
        if (nowDate.before(startDate)) {
            remainSeconds = (int) ((startDate.getTime() - nowDate.getTime()) / 1000);
        } else if (nowDate.after(endDate)) {
            secKillStatus = 2;
            remainSeconds = -1;
        } else {
            secKillStatus = 1;
        }
        model.addAttribute("secKillStatus", secKillStatus);
        model.addAttribute("remainSeconds", remainSeconds);
        model.addAttribute("goods", goodsVo);
        WebContext webContext = new WebContext(request, response, request.getServletContext(), request.getLocale(), model.asMap());
        html = thymeleafViewResolver.getTemplateEngine().process("goodsDetail", webContext);
        if (!StringUtils.isEmpty(html)) {
            valueOperations.set("goodsDetail:" + goodsId, html, 60, TimeUnit.SECONDS);
        }
        return html;
    }
}
