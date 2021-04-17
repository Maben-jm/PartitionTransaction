package com.maben.partition.eureka_client2.controller;

import com.maben.partition.eureka_client2.service.GoodsService;
import com.maben.partition.pojo.Goods;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 商品controller
 */
@RestController
@RequestMapping("goods")
public class GoodsController {

    @Resource
    private GoodsService goodsService;

    @RequestMapping("save")
    public String save(Goods goods){
        try {
            goodsService.save(goods);
        }catch (Exception e){
            e.printStackTrace();
            return "error -msg:"+e.getMessage();
        }
        return "success";
    }

    @RequestMapping(value = "saveJson",method = RequestMethod.POST)
    public String saveJson(@RequestBody Goods goods){
        try {
            goodsService.save(goods);
        }catch (Exception e){
            e.printStackTrace();
            return "error -msg:"+e.getMessage();
        }
        return "success";
    }

}
