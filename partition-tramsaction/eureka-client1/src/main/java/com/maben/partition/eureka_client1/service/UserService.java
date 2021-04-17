package com.maben.partition.eureka_client1.service;

import com.maben.partition.pojo.User;

public interface UserService {

    public void save(User user)throws Exception;

    void partitionTransactionTest(User user);
}
