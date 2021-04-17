package com.maben.partition.eureka_client2.mapper;

import com.maben.partition.pojo.Goods;
import org.apache.ibatis.annotations.Insert;

public interface GoodsMapper {

    @Insert("insert into goods (name,money) values (#{name},#{money})")
    public void insert(Goods goods);

}
