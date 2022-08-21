<p align="center">
    <a href="https://github.com/easybest/spring-data-mybatis">
        <img src="https://raw.githubusercontent.com/easybest/spring-data-mybatis/main/logo.png"/>
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

The primary goal of the Spring Data project is to make it easier to build Spring-powered applications that use data
access technologies.

This module deals with enhanced support for MyBatis based data access layers.
**This module _does not_ make any modifications to MyBatis**. Non-invasive enhancement of MyBatis functionality by using
the pre-compiled MyBatis Mapper Statement mode take developers more flexibility.
At the same time, you can use the Spring Data pattern to reduce the amount of code and improve development efficiency.

> To put it bluntly, it is to help you automatically generate the Mapper files in advance, all the SQLs are ready, and
> then, the essence is still MyBatis. Developers who use MyBatis no longer need to envy Spring Data JPA and the like ^_^

## Useful Links

* [Documentation](https://sdm.easybest.io)
* [Example Project](https://github.com/easybest/spring-data-mybatis-samples)
* [Simplified Chinese](README_zh.md)

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
  Impala, Clickhouse, CUBRID, EnterpriseDB, Firebird, HANA, Ingres, PolarDB, DM, OSCAR,
  HighGO, XUGU, Kingbase etc.

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

## Contributing

Here are some ways for you to get involved in the community:

* GitHub is for social coding: if you want to write code, we encourage contributions through pull requests
  from [forks of this repository](https://help.github.com/forking/).

## Getting Help ##

Here is a [reference documentation](https://sdm.easybest.io) to help you learn Spring Data Mybatis quickly.

If you have any questions or suggestions, you can record
an [issue](https://github.com/easybest/spring-data-mybatis/issues).

In addition, you can also add a QQ group  to conduct related discussions and seek some help.

QQ Group 1: 497907039

## Donation

[![paypal](https://www.paypal.com/en_US/i/btn/x-click-butcc-donate.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=W7PLNCBK5K8JS)

