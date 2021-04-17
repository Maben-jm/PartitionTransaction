package com.maben.partition.eureka_client1.controller;

import com.maben.partition.eureka_client1.service.UserService;
import com.maben.partition.pojo.User;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 用户controller
 */
@RestController
@RequestMapping("user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 测试保存
     * http://localhost:9001/user/save?name=maben&money=10
     * @param user
     */
    @RequestMapping("save")
    public String save(User user){
        try {
            userService.save(user);
        }catch (Exception e){
            e.printStackTrace();
            return "error msg:"+e.getMessage();
        }
        return "success";
    }

}
