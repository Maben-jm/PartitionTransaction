package com.maben.partition.eureka_client2.service.impl;

import com.maben.partition.eureka_client2.mapper.GoodsMapper;
import com.maben.partition.eureka_client2.service.GoodsService;
import com.maben.partition.pojo.Goods;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class GoodsServiceImpl implements GoodsService {

    @Resource
    private GoodsMapper goodsMapper;


    @Override
    @Transactional(readOnly = false,isolation = Isolation.DEFAULT,propagation = Propagation.REQUIRED)
    public void save(Goods goods) throws Exception {
        if (Objects.isNull(goods)){
            return;
        }
        goodsMapper.insert(goods);
    }
}
