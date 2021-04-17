[TOC]

# 分布式事务学习

## 1.什么是分布式事务？

要了解分布式事务，首先要了解本地事物。

###    1.1本地事物

> 本地事物：是指传统的单机数据库事物，必须具备ACID原则：

````
1. 原子性（A）
	在整个事物的所有操作，要么都完成，要么都不做，没有中间装填。
	对于事物在执行过程中发生错误，所有操作都将会被回滚，整个事物就像没有执行过一样；
2. 一致性（C）
	事物的执行必须保证系统的一致性，在事物开始之前和事务结束以后，数据库的完整性没有被破坏。
	比方说银行转账：A有1000元，B有1000元，A想向B转账，经过一系列操作后，两个账户的总数是不变的。
3. 隔离性（I）
	所谓的隔离性，就是说事物与事物之间不会相互影响，一个事物的中间状态不会被其他事物感知。
	数据库有四种隔离级别：
		3.1：Read Uncommitted （读未提交） 
		3.2：Read Committed （读已提交）
		3.3：Repeatable Read （可重复度）
		3.4：Serializable （串行化）
4.持久性（D）
	所谓的持久性，就是说一旦事物提交了，那么事物对数据所做的变更就完全保存在了数据库中，即使发生了停电、宕机也是如此。
````

### 1.2 undo和redo日志

> undo和redo日志是「原子性」和「持久性」的保证。

#### 1.2.1 undo日志

> undo日志简介

````
数据库事物具有原子性，如果事物执行失败，需要将数据进行回滚。
事物同时还具有持久性，事物对数据所做的变更就完整的保存在数据库中，不能因为故障而丢失。
而原子性和持久性就可以利用undo日志来实现。
````

> undo日志原理

````
undo log的原理很简单：
	1.为了满足事务的原子性，在操作任何数据之前，首先将数据备份到Undo Log中；
	2.然后进行数据修改；
	3.如果出现了错误，或者用户执行了rollback语句，系统就可以利用Undo Log中的备份将数据还原。
````

**注：数据库将数据写入磁盘之前，会吧数据线缓存到内存中，事务提交时才会写入到磁盘中。**

> 简单举例

````
假设有A,B两个数据，值分别为1，2
1.事物开始
2.记录A=1到Undo Log日志
3.修改A=3
4.记录B=2到Undo Log日志
5.修改B=4
6.将undo log日志写到磁盘
7.将数据写到磁盘
8.事物提交
````

> undo log缺点

````
对磁盘写操作太频繁，IO操作耗费资源比较严重！SO:引用redo log。
````

#### 1.2.2 redo日志

> 简介

````
和undo log相反，Redo Log记录的新数据的备份。在事物提交前，只需要将Redo Log持久化即可，不需要将数据持久化，减少IO次数
````

> 原理示例

````
假设有A,B两个数据，值分别为1，2
1.事物开始
2.记录A=1到Undo Log Buffer；
3.修改A=3
4.记录A=3到Redo Log Buffer；
5.记录B=2到Undo Log Buffer；
6.修改B=4
7.记录B=4到Redo Log Buffer；
8.将Undo Log写入磁盘
9.将Redo Log写入磁盘
10.事物提交
写磁盘现在是异步的，将RedoLog写入磁盘后已经丢失不了了，采用异步写入磁盘，增加性能。
````

### 1.3 分布式事务

分布式事务就是指不在单个服务或单个数据库框架下，产生的事物：

	* 跨数据源的分布式事务
	* 跨服务的分布式事务
	* 综合情况

#### 1.3.1 跨数据源

随着业务规模的快速发展，数据量越来越大，单库单表逐渐遇到瓶颈。所以我们对数据库进行了水平拆分，将原数据库拆分成数据库分片，于是就产生了跨数据库事物问题。

#### 1.3.2 跨服务

将单业务系统拆分成多个业务系统，降低了各系统之间的耦合度，使不同的业务系统专注于自身业务，更有利于业务的发展和系统容量的伸缩。

## 2.分布式事务解题思路

### 2.1 CAP定理

**分布式的三个指标**

	* Consistency（一致性）
	* Availability （可用性）
	* Partition tolerance （分区容错性）

#### 2.1.1 Partition tolerance

> 大多数分布式系统都分布在多个子网络中，每个子网络就叫做一个去（Partition）。
>
> 分区容错的意思就是说：区间通信可能会失败，并且这种可能是无法避免的

**分区容错性是肯定存在的，所以我们一般只是权衡Consistency和Availability中考虑**

#### 2.1.2 Consistency

> 示例：

````
1.有两个分区A和B，最开始v=1
2.A分区修改操作v=2
3.下一次请求来了，
	如果负载均衡继续分配给A区，那么读取的是v=2
	如果负载均衡将请求分配给B去，那么读取的是V=1
4.这样就造成了不一致的现象。
补充：如果要保证分区一致性，那么在A区变动的时候，需要同步修改B区。
````

#### 2.1.3 Availability

> 「可用性」是说：只要接收到了用户的请求，就必须给客户返回结果，不论对错。
>
>   如果一直卡着不动就不满足可用性。

**注意：系统是无法同时满足AC的，理论上来说即使网速再快，想其他分区同步信息也是需要时间的，而在这段时间内，就无法保证一致性，如果想要在这段时间系统可用，就无法保证可用性。**

> Zookeeper是满足CP的，也就是说在服务节点间数据同步的同时是对外不可用的。
>
> Eureka是满足AP的，也就是说要首先保证系统的可用性，但是数据有可能是旧数据。

### 2.2 base理论

> base理论是为了解决CAP的问题而提出的理论。具体如下：

* Basically Available（基本可用-在一个可接受的时间范围内）
* Soft state （软状态）
* Eventually consistent （最终一致性）

我们解决分布式事务就是根据上述理论来实现，

> 实例：

````
以下单减库存和扣款为例：
	订单服务、库存服务、用户服务及他们对应的数据库就是分布式应用中的三个部分。
	* CP方式：们现在如果要满足事务的强一致性，就必须在订单服务数据库锁定的同时，对库存服务、用户服务的数据资源也同时锁定。等是那个服务业务全部处理完成，才可以释放资源。此时如果有其他请求想要操作被锁定的资源就会阻塞，这样就满足了CP。
	* AP方式：三个服务的对应数据库各自独立执行自己的业务，执行本地事物，不要求互相锁定资源。但是这个中间状态下，我们去访问数据库，可能遇到数据不一致的情况，不过我们需要做一些补救措施，保证在经过一段时间后，数据最终满足一致性。这就是高可用，但弱一致（最终一致）。
````

由上面的两种思想，延伸出了很多的分布式事务解决方案：

	* XA
	* TCC
	* 可靠消息最终一致
	* AT

## 3. 分布式事务几种常见模式

### 3.1 分阶段提交模式

#### 3.1.1 DTP和XA

> 分布式事务的解决手段之一，就是两阶段提交协议（2PC:Two-Phase Commit）。

1994年，X/OPEN组织定义了分布式事务处理的DTP模型。该模型包括这样几个角色：

	* 应用程序（AP）：我们的微服务
	* 事务管理器（TM）: 全局事务管理者
	* 资源管理器（RM）: 一般是数据库
	* 通讯资源管理器（CRM）: 是TM和RM间的通讯中间件

在该模型中，一个分布式事务（全局事务）可以被拆分成许多本地事物，运行在不同的AP和RM上。每个本地事物的ACID很好实现，但是全局事务必须保证其中包含的每个本地事物都能同事成功，若有一个本地事物失败，则其他所有的事物都必须回滚。

但问题是，本地事物处理过程中，并不知道其他事物的运行状态。因此就必须通过CRM来通知各地本地事物，同步事物执行状态。

因此，各个本地事物的通信必须有统一的标准，否则不同数据库建就无法通讯。XA就是X/OPEN DTP中通讯中间件与TM间联系的接口规范，定义了用于通知事物开始，提交，终止，回滚等接口，各个数据库厂商都必须实现这些接口。

#### 3.1.2 二阶段提交

**二阶段提交是说将：全局事务拆分为两个阶段来执行：**

	* 阶段一：准备阶段，各个本地事物完成本地事物的准备工作。
	* 阶段二：执行阶段，各个本地事物根据上一阶段执行结果，进行提交或回滚。

这个过程需要一个协调者（coordinator），还有事物的参与者（voter）；

>  1) 正常情况：

![](./images/1.png)

**投票阶段**：协调组询问各个事物参与者，是否可以执行事物。么个事物参与者执行事物，写入redo和undo日志，然后反馈事物执行成功信息（<font color='red'>agree</font>）。

**提交阶段**：协调组发现每个参与者都可以执行事物（<font color='red'>agree</font>），于是向各个事物参与者发出<font color='red'>commit</font>指令，各个事物参与者提交事物。

>   2) 异常情况

![](./images/2.png)

**投票阶段**：协调组询问各个事物参与者，是否可以执行事物。每个事物参与者执行事物，写入redo和undo日志，然后反馈事物执行结果，但是只要有一个参与者返回的是<font color='red'>Disagree</font>，则说明执行失败。

**提交阶段**：协调组发现有一个或多个参与者返回的是<font color='red'>Disagree</font>，认为执行失败。于是想哥哥事物参与者发出<font color='red'>abort</font>指令，各个事物参与者回滚事物。

> 3) 缺陷

* 单点故障问题

  2PC的缺点就在于不能处理fail-stop形式的节点failure。也就是说当协调者已经拿到每个事物参与者返回的信息了，结果突然宕机了，后面的流程就进行不下去了。

* 资源锁定问题

  协调者在询问的过程中，每个分区之间的资源都是处于锁定的状态，如果协调者一直不回答，就会出现资源锁定过长的问题。

> 4)优势

​	技术相对来说比较成熟了，并且能够保证强一致性！

### 3.2 TCC模式

TCC模式可以解决2PC中的资源锁定和阻塞问题，减少资源锁定时间。

#### 3.2.1 基本原理

![](./images/4.png)

它本质是一种补偿思路。事物运行过程包括三个方法：

	* Try：资源的检测和预留；
	* Confirm：执行的业务操作提交；要求Try成功，confirm一定要能成功；
	* Concel：预留资源释放。

执行分两个阶段：

	* 准备阶段（try）：资源的检测和预留
	* 执行阶段（confirm/concel）：根据上一步结果，判断下面的执行方法。如果上一步中所有的参与者都成功，则这里执行confirm。反之，执行concel。

**粗看似乎与两阶段提交没什么区别，但其实差别很大：**

	* try,confirm,concel都是独立的事物，不收其他参与者影响，也不会阻塞等待他人
	* try,confirm,concel由程序员在业务层编写，锁粒度由代码控制

#### 3.2.2 实例

````
我们以之前的下单业务的扣款为例，假设用户A账户余额是100，需要扣除30：
	1. 一阶段（Try）：余额检查，并冻结用户部分金额，此阶段执行完毕，事物已经提交。
		* 检查用户余额是否充足，如果充足，冻结部分余额
		* 在账户表中添加冻结金额字段，值为30，余额不变
	2. 二阶段
		* 提交（Confirm）：真正的扣款，把冻结金额从余额中扣除，冻结金额清空
		* 补偿（Concel）：释放之前的冻结金额，并非回滚
````

#### 3.2.3 优劣势

> 优势

TCC执行的每一个阶段都会提交本地事物并释放锁，并不需要等待其他事物的执行结果，如果其他事物执行失败，最后不是回滚，而是执行补偿操作。这样就避免了资源的长期锁定和阻塞等待，执行效率比较高，属于性能比较好的分布式事务方式。

> 缺陷

1. 代码侵入： 需要人为编写代码实现try,confirm,concel，代码侵入较多
2. 开发成本高： 一个业务需要拆分成3个步骤，分别编写业务实现，业务编写比较复杂
3. 安全性考虑：concel动作如果执行失败，资源就无法释放，需要引用重试机制，而重试机制可能导致重复执行，同时还要考虑重试时的幂等问题。

#### 3.2.4 使用场景

1. 对事物有一定的一致性要求（最终一致）
2. 对性能要求较高
3. 开发人员具有较高的编码能力和幂等处理经验

### 3.3 可靠消息服务模式

这种实现方式的思路，其实源于ebay，其基本的设计思想是将远程分布式事务拆分成一系列本地事物。

##### 3.3.1 基本原理

一般分为事物的发起者A和事务的其他参与者B

	* 事物发起者A执行本地事物
	* 事物发起者A通过MQ将需要执行的事物信息发送给事物参与者B
	* 事物参与者B接收到消息后执行本地事物

这种方式是说如果A执行成功之后，给B发MQ消息，如果B执行失败，那么MQ会一直尝试继续消费这个消息，而不会说反过头来让A回滚，因为A已经提交了。

##### 3.3.2 优劣势

> 优势

	* 业务相对简单，不需要编写三个阶段业务
	* 是多个本地事物的综合，因此资源锁定周期短，性能好

> 缺点

	* 代码入侵
	* 依赖于MQ的可靠性
	* 消息发起者可以回滚，但是消息参与者无法引起事物回滚
	* 事务时效性查，取决于MQ消息发送是否及时，还有消息参与者的执行情况

### 3.4 AT模式（优选）

2019年1月份，<font color="red">seata</font> 开源了AT模式。AT模式是一种无侵入的分布式事务解决方案。可以看做是对TCC或者二阶段提交模型的优化，解决了TCC模式中的代码侵入、编码复杂等问题。

**在AT模式下，用户只需关注自己的‘业务SQL’,用户的‘业务SQL’作为一阶段 <font color="red">seata</font> 框架会自动生成事务的二阶段提交和回滚操作。**

#### 3.4.0 资料链接

> 下载地址

https://seata.io/zh-cn/blog/download.html

> 中文地址

https://seata.io/zh-cn/docs/overview/what-is-seata.html

#### 3.4.1 基本原理图

![](./images/3.png)

AT模式分为两个阶段：

	* 一阶段：执行本地事物，并返回执行结果
	* 二阶段：根据一阶段的结果，判断二阶段的做法：提交或回滚

这个过程和TCC类似，但是AT模式底层做的事情完全不同，做大的区别就是第二阶段不需要我们自己编写，全都有<font color="red">seata</font>自己实现了。也就是说，<font color='red'>我们写的代码与本地事物时的代码是一样的</font>，无需手动处理分布式事务。

#### 3.4.2 基本概念

 * TC （Transaction Coordinator）- 事务协调者

   维护全局和分支事物的状态，驱动全局事务提交或回滚（TM之间的协调者）。

* TM （Transaction Manager） - 事物管理器

  定义全局事务的范围：开始全局事务、提交或回滚全局事务。

* RM （Resource Manager） - 资源管理器

  管理分支事物的资源，与TC交谈注册分支事物和报告分支事物状态，并驱动分支事物提交或回滚。



## 4. springcloud整合seata-at

### 4.0 数据库准备

> MySQL版本

````sql
-- 商品表
CREATE TABLE `goods` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `money` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8;

-- 用户表
CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `money` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8;
````

### 4.1 项目准备

#### 4.1.1 父项目

##### 4.1.1.1 pom.xml

````xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.1.3.RELEASE</version>
        <relativePath/>
    </parent>

    <groupId>com.maben</groupId>
    <artifactId>partition-tramsaction</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <properties>
        <java.version>1.8</java.version>
        <maven-jar-plugin.version>3.1.1</maven-jar-plugin.version>
    </properties>
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>Greenwich.RELEASE</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!--json-->
            <dependency>
                <groupId>com.alibaba</groupId>
                <artifactId>fastjson</artifactId>
                <version>1.2.47</version>
            </dependency>
            <!--lombok-->
            <dependency>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>1.18.0</version>
            </dependency>
        </dependencies>
    </dependencyManagement>


    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

    <modules>
        <module>eureka-server</module>
        <module>eureka-client1</module>
        <module>eureka-client2</module>
        <module>eureka-common</module>
    </modules>

</project>
````

#### 4.1.2 eureka-server

> eureka-server ：springcloud的注册器

##### 4.1.2.1 pom.xml

````xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <artifactId>partition-tramsaction</artifactId>
        <groupId>com.maben</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>eureka-server</artifactId>

    <properties>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-security</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>


</project>
````

##### 4.1.2.2 application.yml

````yaml
server:
  port: 9000
spring:
  application:
    name: euraka
  security:
    user:
      name: maben
      password: maben123

eureka:
  server:
    enable-self-preservation: false    #关闭服务器自我保护，客户端心跳检测15分钟内错误达到80%服务会保护，导致别人还认为是好用的服务
    eviction-interval-timer-in-ms: 10000 #清理间隔（单位毫秒，默认是60*1000）5秒将客户端剔除的服务在服务注册列表中剔除#
    shouldUseReadOnlyResponseCache: true #eureka是CAP理论种基于AP策略，为了保证强一致性关闭此切换CP 默认不关闭 false关闭
  client:
    register-with-eureka: false  #false:不作为一个客户端注册到注册中心
    fetch-registry: false      #为true时，可以启动，但报异常：Cannot execute request on any known server
    instance-info-replication-interval-seconds: 10
    serviceUrl:
      defaultZone: http://${spring.security.user.name}:${spring.security.user.password}@127.0.0.1:9000/eureka/
  instance:
    hostname: ${spring.cloud.client.ip-address}
    prefer-ip-address: true
    instance-id: ${spring.cloud.client.ip-address}:${server.port}
````

##### 4.1.2.3 启动类

````java
package com.maben.partition.eureka_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@EnableEurekaServer
@SpringBootApplication
public class EurekaServerApplication {
    public static void main(String[] args){
        SpringApplication.run(EurekaServerApplication.class,args);
        System.out.println("******************启动成功！！*****************");
    }

    /**
     *新版的security默认启用了csrf检验，  需要关闭客户端才能完成注册
     */
    @EnableWebSecurity
    public class WebSecurityConfigurer extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.csrf().disable();
            super.configure(http);
        }
    }
}
````

#### 4.1.3 eureka-common

> eureka-common 公共项目

##### 4.1.3.1 pom.xml

````xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>partition-tramsaction</artifactId>
        <groupId>com.maben</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>eureka-common</artifactId>
    <properties>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
        </dependency>
    </dependencies>
</project>
````

##### 4.1.3.2 po类

> Goods

````java
package com.maben.partition.pojo;

import lombok.Data;

/**
 * 商品PO
 */
@Data
public class Goods {
    private Integer id;
    private String name;
    private Integer money;
}

````

> User

````java
package com.maben.partition.pojo;

import lombok.Data;

@Data
public class User {

    private Integer id;
    private String name;
    private Integer money;

}
````

#### 4.1.4 eureka-client1

##### 4.1.4.1 pom.xml

````xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>partition-tramsaction</artifactId>
        <groupId>com.maben</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>eureka-client1</artifactId>
    <properties>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.maben</groupId>
            <artifactId>eureka-common</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-ribbon</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>com.netflix.hystrix</groupId>
            <artifactId>hystrix-javanica</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.retry</groupId>
            <artifactId>spring-retry</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-freemarker</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.data</groupId>
            <artifactId>spring-data-commons</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
        <!--mysql-->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid</artifactId>
            <version>1.1.6</version>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>1.3.1</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
````

4.1.4.2 application.properties

````properties
server.port=9001
spring.application.name=eureka-client1
# eureka config
eureka.client.registry-fetch-interval-seconds=30
eureka.client.serviceUrl.defaultZone=http://maben:maben123@127.0.0.1:9000/eureka/
eureka.instance.lease-expiration-duration-in-seconds=15
eureka.instance.lease-renewal-interval-in-seconds=5
eureka.instance.prefer-ip-address=true
eureka.instance.instance-id=${spring.cloud.client.ip-address}:${server.port}

#open-feign config
feign.hystrix.enabled = true
feign.compression.request.enabled = true
feign.compression.request.mime-types[0] = text/xml
feign.compression.request.mime-types[1] = application/xml
feign.compression.request.mime-types[2] = application/json
feign.compression.request.min-request-size = 2048
feign.compression.response.enabled = true

#mysql config
spring.datasource.type=com.alibaba.druid.pool.DruidDataSource
spring.datasource.driverClassName = com.mysql.jdbc.Driver
spring.datasource.url = jdbc:mysql://127.0.0.1:3306/maben?useUnicode=true&characterEncoding=utf-8
spring.datasource.username = root
spring.datasource.password = 123456
mybatis.type-aliases-package=com.maben.partition.eureka_clent1.pojo
mybatis.configuration.map-underscore-to-camel-case=true
````

##### 4.1.4.2 启动类

````java
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
````

##### 4.1.4.3 controller

> UserController

```java
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
```

> GoodsController

```java
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
```

> PartitionTransactionController

```java
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
```

##### 4.1.4.4 service

> UserService

```java
package com.maben.partition.eureka_client1.service;

import com.maben.partition.pojo.User;

public interface UserService {

    public void save(User user)throws Exception;

    void partitionTransactionTest(User user);
}
```

> UserServiceImpl

```java
package com.maben.partition.eureka_client1.service.impl;

import com.maben.partition.eureka_client1.feign.EurekaClient2Feign;
import com.maben.partition.eureka_client1.mapper.UserMapper;
import com.maben.partition.eureka_client1.service.UserService;
import com.maben.partition.pojo.Goods;
import com.maben.partition.pojo.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
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
    @Transactional(readOnly = false,isolation = Isolation.DEFAULT,propagation = Propagation.REQUIRED)
    public void partitionTransactionTest(User user) {
        if (Objects.isNull(user)){
            return;
        }
        final Goods goods = new Goods();
        goods.setName(user.getName());
        goods.setMoney(user.getMoney());
        eurekaClient2Feign.saveJson(goods);
        userMapper.insert(user);
        int i = 1/0;
    }
}
```

##### 4.1.4.5 mapper

> UserMapper

```java
package com.maben.partition.eureka_client1.mapper;

import com.maben.partition.pojo.User;
import org.apache.ibatis.annotations.Insert;

public interface UserMapper {

    @Insert(value = "insert into user (name,money) values(#{name},#{money})")
    public void insert(User user);

}
```

##### 4.1.4.6 feign

```java
package com.maben.partition.eureka_client1.feign;

import com.maben.partition.pojo.Goods;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient(value = "eureka-client2")
public interface EurekaClient2Feign {

    @RequestMapping("/goods/saveJson")
    String saveJson(Goods goods);

}
```

#### 4.1.5 eureka-client2

##### 4.1.5.1 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>partition-tramsaction</artifactId>
        <groupId>com.maben</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>eureka-client2</artifactId>

    <properties>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.maben</groupId>
            <artifactId>eureka-common</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-ribbon</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>com.netflix.hystrix</groupId>
            <artifactId>hystrix-javanica</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.retry</groupId>
            <artifactId>spring-retry</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-freemarker</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.data</groupId>
            <artifactId>spring-data-commons</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
        <!--mysql-->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid</artifactId>
            <version>1.1.6</version>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>1.3.1</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

##### 4.1.5.2 application.properties

```properties
server.port=9002
spring.application.name=eureka-client2
# eureka config
eureka.client.registry-fetch-interval-seconds=30
eureka.client.serviceUrl.defaultZone=http://maben:maben123@127.0.0.1:9000/eureka/
eureka.instance.lease-expiration-duration-in-seconds=15
eureka.instance.lease-renewal-interval-in-seconds=5
eureka.instance.prefer-ip-address=true
eureka.instance.instance-id=${spring.cloud.client.ip-address}:${server.port}

#open-feign config
feign.hystrix.enabled = true
feign.compression.request.enabled = true
feign.compression.request.mime-types[0] = text/xml
feign.compression.request.mime-types[1] = application/xml
feign.compression.request.mime-types[2] = application/json
feign.compression.request.min-request-size = 2048
feign.compression.response.enabled = true


#mysql config
spring.datasource.type=com.alibaba.druid.pool.DruidDataSource
spring.datasource.driverClassName = com.mysql.jdbc.Driver
spring.datasource.url = jdbc:mysql://127.0.0.1:3306/maben?useUnicode=true&characterEncoding=utf-8
spring.datasource.username = root
spring.datasource.password = 123456
mybatis.type-aliases-package=com.maben.partition.eureka_clent1.pojo
mybatis.configuration.map-underscore-to-camel-case=true
```

##### 4.1.5.3 启动类

```java
package com.maben.partition.eureka_client2;

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
@MapperScan("com.maben.partition.eureka_client2.mapper")
public class EurekaClient2Application {

    public static void main(String[] args){
        SpringApplication.run(EurekaClient2Application.class,args);
        System.out.println("****************启动成功！！！***************");
    }

}
```

##### 4.1.5.4 controller

```java
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
```

##### 4.1.5.5 service

```java
package com.maben.partition.eureka_client2.service;

import com.maben.partition.pojo.Goods;

public interface GoodsService {
    public void save(Goods goods)throws Exception;
}
```

```java
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
```

##### 4.1.5.6 mapper 

```java
package com.maben.partition.eureka_client2.mapper;

import com.maben.partition.pojo.Goods;
import org.apache.ibatis.annotations.Insert;

public interface GoodsMapper {

    @Insert("insert into goods (name,money) values (#{name},#{money})")
    public void insert(Goods goods);

}
```

### 4.2 seata准备

> seata使用版本seata-server-1.0.0

**我自己MySQL是8版本，seata1.0自带的驱动是5的，需要换一下驱动！！！**

#### 4.2.1 registry.conf

````
registry {
  # file 、nacos 、eureka、redis、zk、consul、etcd3、sofa
  type = "eureka"

  nacos {
    serverAddr = "localhost"
    namespace = ""
    cluster = "default"
  }
  eureka {
    serviceUrl = "http://maben:maben123@localhost:9000/eureka/"
    application = "seata_tc_server"
    weight = "1"
  }
  redis {
    serverAddr = "localhost:6379"
    db = "0"
  }
  zk {
    cluster = "default"
    serverAddr = "127.0.0.1:2181"
    session.timeout = 6000
    connect.timeout = 2000
  }
  consul {
    cluster = "default"
    serverAddr = "127.0.0.1:8500"
  }
  etcd3 {
    cluster = "default"
    serverAddr = "http://localhost:2379"
  }
  sofa {
    serverAddr = "127.0.0.1:9603"
    application = "default"
    region = "DEFAULT_ZONE"
    datacenter = "DefaultDataCenter"
    cluster = "default"
    group = "SEATA_GROUP"
    addressWaitTime = "3000"
  }
  file {
    name = "file.conf"
  }
}

config {
  # file、nacos 、apollo、zk、consul、etcd3
  type = "file"

  nacos {
    serverAddr = "localhost"
    namespace = ""
  }
  consul {
    serverAddr = "127.0.0.1:8500"
  }
  apollo {
    app.id = "seata-server"
    apollo.meta = "http://192.168.1.204:8801"
  }
  zk {
    serverAddr = "127.0.0.1:2181"
    session.timeout = 6000
    connect.timeout = 2000
  }
  etcd3 {
    serverAddr = "http://localhost:2379"
  }
  file {
    name = "file.conf"
  }
}
````

#### 4.2.2 file.conf

````
service {
  #transaction service group mapping
  vgroup_mapping.my_test_tx_group = "seata_tc_server"  //事务分组，非常重要，client和tc一定要一致，fsp_tx_group是个自定义的分组名称
  #only support when registry.type=file, please don't set multiple addresses
  default.grouplist = "127.0.0.1:8091"
  #disable seata
  disableGlobalTransaction = false
}

## transaction log store, only used in seata-server
store {
  ## store mode: file、db
  mode = "db"

  ## file store property
  file {
    ## store location dir
    dir = "sessionStore"
  }

  ## database store property
  db {
    ## the implement of javax.sql.DataSource, such as DruidDataSource(druid)/BasicDataSource(dbcp) etc.
    datasource = "druid"
    ## mysql/oracle/h2/oceanbase etc.
    db-type = "mysql"
    driver-class-name = "com.mysql.jdbc.Driver"
    url = "jdbc:mysql://127.0.0.1:3306/maben?useUnicode=true&characterEncoding=utf-8" 
    user = "root"
    password = "123456"
  }
}
````

#### 4.2.3 建表（上面使用db）

全局事务会话信息由3块内容构成：

	* 全局事务 global_table
	* 分支事务 branch_table
	* 全局锁 lock_table

建表语句在GitHub上有：https://github.com/seata/seata/tree/develop/script/server/db

这里粘贴MySQL的：

```sql
-- -------------------------------- The script used when storeMode is 'db' --------------------------------
-- the table to store GlobalSession data
CREATE TABLE IF NOT EXISTS `global_table`
(
    `xid`                       VARCHAR(128) NOT NULL,
    `transaction_id`            BIGINT,
    `status`                    TINYINT      NOT NULL,
    `application_id`            VARCHAR(32),
    `transaction_service_group` VARCHAR(32),
    `transaction_name`          VARCHAR(128),
    `timeout`                   INT,
    `begin_time`                BIGINT,
    `application_data`          VARCHAR(2000),
    `gmt_create`                DATETIME,
    `gmt_modified`              DATETIME,
    PRIMARY KEY (`xid`),
    KEY `idx_gmt_modified_status` (`gmt_modified`, `status`),
    KEY `idx_transaction_id` (`transaction_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

-- the table to store BranchSession data
CREATE TABLE IF NOT EXISTS `branch_table`
(
    `branch_id`         BIGINT       NOT NULL,
    `xid`               VARCHAR(128) NOT NULL,
    `transaction_id`    BIGINT,
    `resource_group_id` VARCHAR(32),
    `resource_id`       VARCHAR(256),
    `branch_type`       VARCHAR(8),
    `status`            TINYINT,
    `client_id`         VARCHAR(64),
    `application_data`  VARCHAR(2000),
    `gmt_create`        DATETIME(6),
    `gmt_modified`      DATETIME(6),
    PRIMARY KEY (`branch_id`),
    KEY `idx_xid` (`xid`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

-- the table to store lock data
CREATE TABLE IF NOT EXISTS `lock_table`
(
    `row_key`        VARCHAR(128) NOT NULL,
    `xid`            VARCHAR(128),
    `transaction_id` BIGINT,
    `branch_id`      BIGINT       NOT NULL,
    `resource_id`    VARCHAR(256),
    `table_name`     VARCHAR(32),
    `pk`             VARCHAR(36),
    `gmt_create`     DATETIME,
    `gmt_modified`   DATETIME,
    PRIMARY KEY (`row_key`),
    KEY `idx_branch_id` (`branch_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;
```

### 4.3 整合



#### 4.3.1 添加maven依赖

```xml
				<!--seata-->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-seata</artifactId>
            <version>2.1.0.RELEASE</version>
            <exclusions>
                <exclusion>
                    <artifactId>seata-all</artifactId>
                    <groupId>io.seata</groupId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>io.seata</groupId>
            <artifactId>seata-all</artifactId>
            <version>1.0.0</version>
        </dependency>
```

#### 4.3.2 配置参数 

```properties
#seata
spring.cloud.alibaba.seata.tx-service-group=my_test_tx_group
```

#### 4.3.3 registry.conf

````
registry {
  # file 、nacos 、eureka、redis、zk
  type = "eureka"
  eureka {
    serviceUrl = "http://maben:maben123@127.0.0.1:9000/eureka/"
    application = "default" 
    weight = "1"
  }
    //省略
}

config {
  # file、nacos 、apollo、zk
  type = "file"
  file {
    name = "file.conf"
  }
    //省略
}
````

#### 4.3.4 file.conf

````
//省略很多
service {
  #transaction service group mapping
  vgroup_mapping.my_test_tx_group = "seata_tc_server"
  #only support when registry.type=file, please don't set multiple addresses
  default.grouplist = "127.0.0.1:8091"
  #degrade, current not support
  enableDegrade = false
  #disable seata
  disableGlobalTransaction = false
}
client {
    //省略很多
  rm {
    report.success.enable = true //一阶段成功后是否上报tc，这个配置可以提高性能
  }
  support {
    # auto proxy the DataSource bean //数据源自动代理
    spring.datasource.autoproxy = false
  }
}
````

#### 4.3.5 数据源代理

````java
package com.maben.partition.eureka_client1.config;

import com.alibaba.druid.pool.DruidDataSource;
import io.seata.rm.datasource.DataSourceProxy;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 数据源代理
 */
@Configuration
public class DataSourceConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource druidDataSource(){
        DruidDataSource druidDataSource = new DruidDataSource();
        return druidDataSource;
    }

    @Primary
    @Bean("dataSource")
    public DataSourceProxy dataSource(DataSource druidDataSource){
        return new DataSourceProxy(druidDataSource);
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSourceProxy dataSourceProxy)throws Exception{
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSourceProxy);
        sqlSessionFactoryBean.setTransactionFactory(new SpringManagedTransactionFactory());
        return sqlSessionFactoryBean.getObject();
    }
}
````

#### 4.3.6 建表

如果要使用seata分布式事务，当前服务就需要建一张undolog表。

建表语句参考：https://github.com/seata/seata/tree/develop/script/client/at/db

这里粘贴MySQL的：

````java
-- for AT mode you must to init this sql for you business database. the seata server not need it.
CREATE TABLE IF NOT EXISTS `undo_log`
(
    `branch_id`     BIGINT       NOT NULL COMMENT 'branch transaction id',
    `xid`           VARCHAR(128) NOT NULL COMMENT 'global transaction id',
    `context`       VARCHAR(128) NOT NULL COMMENT 'undo_log context,such as serialization',
    `rollback_info` LONGBLOB     NOT NULL COMMENT 'rollback info',
    `log_status`    INT(11)      NOT NULL COMMENT '0:normal status,1:defense status',
    `log_created`   DATETIME(6)  NOT NULL COMMENT 'create datetime',
    `log_modified`  DATETIME(6)  NOT NULL COMMENT 'modify datetime',
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8 COMMENT ='AT transaction mode undo table';
````



