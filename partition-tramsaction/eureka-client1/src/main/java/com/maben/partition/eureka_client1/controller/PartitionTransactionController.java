package com.maben.partition.eureka_client1.controller;

import com.maben.partition.eureka_client1.service.UserService;
import com.maben.partition.pojo.User;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 分布式事务测试类
 */
@RestController
public class PartitionTransactionController {

    @Resource
    private UserService userService;

    @RequestMapping("test")
    public String partitionTransactionTest(User user){
        try {
            userService.partitionTransactionTest(user);
            return "success";
        }catch (Exception e){
            e.printStackTrace();
            return "error msg is "+e.getMessage();
        }
    }

}
