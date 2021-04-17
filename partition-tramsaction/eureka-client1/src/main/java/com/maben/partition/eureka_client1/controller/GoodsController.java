package com.maben.partition.eureka_client1.controller;

import com.maben.partition.eureka_client1.feign.EurekaClient2Feign;
import com.maben.partition.pojo.Goods;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 测试open feign
 */
@RestController
@RequestMapping("uGoods")
public class GoodsController {

    @Resource
    private EurekaClient2Feign eurekaClient2Feign;

    @RequestMapping("save")
    public String save(Goods goods){
        final String result = eurekaClient2Feign.saveJson(goods);
        if (result.contains("success")){
            return "success";
        }else {
            return result;
        }
    }

}
