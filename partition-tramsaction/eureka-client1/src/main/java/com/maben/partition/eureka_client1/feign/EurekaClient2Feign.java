package com.maben.partition.eureka_client1.feign;

import com.maben.partition.pojo.Goods;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient(value = "eureka-client2")
public interface EurekaClient2Feign {

    @RequestMapping("/goods/saveJson")
    String saveJson(Goods goods);

}
