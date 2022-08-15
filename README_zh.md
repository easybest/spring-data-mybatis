<p align="center">
    <a href="https://github.com/easybest/spring-data-mybatis">
        <img src="https://raw.githubusercontent.com/easybest/spring-data-mybatis/dameng/logo.png"/>
    </a>
</p>

----

<p align="center">
    <a href="https://github.com/easybest/spring-data-mybatis/actions/workflows/github-actions-ci.yml" title="Build">
        <img src="https://github.com/easybest/spring-data-mybatis/actions/workflows/github-actions-ci.yml/badge.svg"/>
    </a>
    <a href="https://maven-badges.herokuapp.com/maven-central/io.easybest/spring-data-mybatis" title="Maven Central">
        <img src="https://maven-badges.herokuapp.com/maven-central/io.easybest/spring-data-mybatis/badge.svg"/>
    </a>
    <a href="https://github.com/hatunet/spring-data-mybatis/blob/main/LICENSE" title="License: Apache 2.0">
        <img src="https://img.shields.io/badge/license-Apache_2.0-brightgreen.svg"/>
    </a>
    <a href="https://gitter.im/spring-data-mybatis" title="Gitter chat">
        <img src="https://badges.gitter.im/gitterHQ/gitter.png"/>
    </a>
</p>

Spring Data 项目的主要目标是使构建使用数据访问技术的 Spring 应用程序变得更加容易。此模块处理增强基于 MyBatis 的数据访问层的支持。

通过使用此模块，你可以在基于MyBatis为ORM的结构下使用Spring Data模式带来的便利性， _**本项目不对MyBatis做任何修改，通过前置编译生成MyBatis
Mapper Statement的模式**_，无侵入性的增强MyBatis功能，让各位开发者在享受MyBatis灵活性的同时，使用Spring
Data的快捷构建查询的方式，极大的减少编码量，提升开发效率。

> 说白了，就是帮你自动把Mapper文件都提前生成好了，SQL都准备好了，然后，然后本质MyBatis，该怎么用还怎么用。
> 使用MyBatis的同学不用再羡慕Spring Data JPA之流 ^_^

如果你还没有接触过[Spring Data](https://projects.spring.io/spring-data/)
，建议先了解下该[项目](https://projects.spring.io/spring-data/)。

## 链接

* [文档](https://sdm.easybest.io)
* [例子](https://github.com/easybest/spring-data-mybatis-samples)

## 特性 ##

* 不侵入MyBatis，完全原生利用
* 使用标准的Java Persistence API (JPA)注解
* 对Entity支持完整CRUD操作，以及更复杂的查询动作
* 支持通过接口中的方法名生成对应的查询 (Spring Data)
* 支持关联查询，支持自动识别关联查询条件
* 提供基础属性的实体基类
* 支持透明审计（如创建时间、最后修改)
* 自持自定义编写基于MyBatis的查询，方便而不失灵活性
* 方便的与Spring和Spring Boot集成
* 支持 MySQL, Oracle, SQL Server, H2, PostgreSQL, DB2, Derby, HSQL, Phoenix, MariaDB, Sqlite, Informix, HerdDB,
  Clickhouse, PolarDB, 达梦, 神通, 瀚高, 虚谷, 人大金仓 等数据库
* 支持SpringBoot 2.x

## 获得帮助 ##

这里有一份文档可以帮助你快速学习 Spring Data Mybatis。 [reference documentation](https://sdm.easybest.io)

如果你有任何疑问或者建议，可以录一个[issue](https://github.com/easybest/spring-data-mybatis/issues) 给我。

## 快速开始 ##

## 贡献代码 ##

如果你想帮助维护本项目，可以通过PR的方式提交代码 [forks of this repository](https://help.github.com/forking/).

## 支持与捐赠

[![paypal](https://www.paypal.com/en_US/i/btn/x-click-butcc-donate.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=W7PLNCBK5K8JS)

