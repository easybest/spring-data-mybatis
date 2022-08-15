# Spring Data MyBatis

[![Build](https://github.com/easybest/spring-data-mybatis/actions/workflows/github-actions-ci.yml/badge.svg)](https://github.com/easybest/spring-data-mybatis/actions/workflows/github-actions-ci.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.easybest/spring-data-mybatis/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.easybest/spring-data-mybatis)
[![License: Apache 2.0](https://img.shields.io/badge/license-Apache_2.0-brightgreen.svg)](https://github.com/hatunet/spring-data-mybatis/blob/main/LICENSE)

[Simplified Chinese](README_zh.md)

[Documentation](https://sdm.easybest.io)

[Example Project](https://github.com/easybest/spring-data-mybatis-samples)

The primary goal of the Spring Data project is to make it easier to build Spring-powered applications that use data
access technologies.
This module deals with enhanced support for MyBatis based data access layers.
**_This module does not make any modifications to MyBatis, generate Mybatis Mapper Statements by precompiling _**,
Non-invasive enhancement of MyBatis functionality take developers more flexibility.
At the same time, you can use the Spring Data pattern to reduce the amount of code and improve development efficiency.

> To put it bluntly, it is to help you automatically generate the Mapper files in advance, all the SQLs are ready, and
> then, the essence is still MyBatis. Developers who use MyBatis no longer need to envy Spring Data JPA and the like ^_^

## Features ##

* Do not invade MyBatis, completely native use
* Use standard Java Persistence API (JPA) annotations
* Supports full CRUD operations on Entities, as well as more complex queries
* Support to generate corresponding query by method name in interface (Spring Data)
* Support associated queries, support automatic identification of associated query conditions
* Entity base class that provides basic properties
* Support transparent auditing (such as creation time, last modification)
* Self-sustained custom writing of MyBatis-based queries, convenient and flexible
* Easy integration with Spring and Spring Boot
* Support MySQL, Oracle, SQL Server, H2, PostgreSQL, DB2, Derby, HSQL, Phoenix, MariaDB, Sqlite, Informix, HerdDB,
  Clickhouse, PolarDB, DM, OSCAR, HighGO, XUGU, Kingbase etc.

## Getting Help ##

Here is a document to help you learn Spring Data Mybatis quickly. [reference documentation](https://sdm.easybest.io)

If you have any questions or suggestions, you can record
an [issue](https://github.com/easybest/spring-data-mybatis/issues) for me.

[PR](https://github.com/easybest/spring-data-mybatis/pulls) welcome.

## Quick Start ##

### Installation

#### Maven
```xml
<dependency>
    <groupId>io.easybest</groupId>
    <artifactId>spring-data-mybatis</artifactId>
    <version>2.1.0</version>
</dependency>
```

In Spring Boot, use the following starter directly:
```xml
<dependency>
    <groupId>io.easybest</groupId>
    <artifactId>spring-data-mybatis-starter</artifactId>
    <version>2.1.0</version>
</dependency>
```

#### Gradle
```groovy
implementation('io.easybest:spring-data-mybatis:2.1.0')
```
In Spring Boot, use the following starter directly:
```groovy
implementation('io.easybest:spring-data-mybatis-starter:2.1.0')
```

