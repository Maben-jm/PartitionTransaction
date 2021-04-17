package com.maben.partition.eureka_client1.service.impl;

import com.maben.partition.eureka_client1.feign.EurekaClient2Feign;
import com.maben.partition.eureka_client1.mapper.UserMapper;
import com.maben.partition.eureka_client1.service.UserService;
import com.maben.partition.pojo.Goods;
import com.maben.partition.pojo.User;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Service
public class UserServiceImpl implements UserService {
    @Resource
    private UserMapper userMapper;

    @Resource
    private EurekaClient2Feign eurekaClient2Feign;

    @Override
    @Transactional(readOnly = false,isolation = Isolation.DEFAULT,propagation = Propagation.REQUIRED)
    public void save(User user) throws Exception {
        if (Objects.isNull(user)){
            return ;
        }
        userMapper.insert(user);
    }

    @Override
    @GlobalTransactional()
    public void partitionTransactionTest(User user) {
        if (Objects.isNull(user)){
            return;
        }
        final Goods goods = new Goods();
        goods.setName(user.getName());
        goods.setMoney(user.getMoney());
        eurekaClient2Feign.saveJson(goods);
        userMapper.insert(user);
        if (user.getMoney()%2==1){
            int i = 1/0;
        }
    }
}
