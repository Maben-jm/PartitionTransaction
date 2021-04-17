package com.maben.partition.eureka_client1;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableEurekaClient
@SpringBootApplication
@EnableHystrix
@EnableFeignClients(basePackages = {"com.maben"})
@MapperScan("com.maben.partition.eureka_client1.mapper")
public class EurekaClient1Application {
    public static void main(String[] args){
        SpringApplication.run(EurekaClient1Application.class,args);
        System.out.println("*****************启动成功！！**************");
    }
}
