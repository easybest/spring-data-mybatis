# Spring Data MyBatis [![Build Status](https://travis-ci.org/hatunet/spring-data-mybatis.svg?branch=master)](https://travis-ci.org/hatunet/spring-data-mybatis) 
The primary goal of the Spring Data project is to make it easier to build Spring-powered applications that use data access technologies. 
This module deals with enhanced support for MyBatis based data access layers.


## Features ##

* Implementation of CRUD methods for normal Entities
* Dynamic query generation from query method names
* Implementation domain base classes providing basic properties
* Support for transparent auditing (created, last changed)
* Possibility to integrate custom repository code
* Easy Spring integration with custom namespace


## Getting Help ##
This README as well as the [reference documentation](https://hatunet.github.io/spring-data-mybatis/) are the best places to start learning about Spring Data MyBatis. 

If you have any question, please record a [issue](https://github.com/hatunet/spring-data-mybatis/issues) to me.


## Quick Start ##

Download the jar through Maven:

```xml
<dependency>
  <groupId>com.ifrabbit</groupId>
  <artifactId>spring-data-mybatis</artifactId>
  <version>${version}.RELEASE</version>
</dependency>
```

The simple Spring Data Mybatis configuration with Java-Config looks like this: 
```java
@Configuration
@EnableMybatisRepositories("com.example.repositories")
public class AppConfig {

    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
    }

    @Bean
    public SqlSessionFactoryBean sqlSessionFactoryBean(DataSource dataSource) {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        return factoryBean;
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

}

```

Create an entity:

```java
@Entity
public class User extends LongId {

  private String firstname;
  private String lastname;
       
  // Getters and setters
  // (Firstname, Lastname)-constructor and noargs-constructor
  // equals / hashcode
}

```

Create a repository interface in `com.example.repositories`:

```java
public interface UserRepository extends CrudRepository<User, Long> {
  List<User> findByLastname(String lastname);  
  
}

```

Write a test client:

```java
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class UserRepositoryIntegrationTest {
     
  @Autowired UserRepository repository;
     
  @Test
  public void sampleTestCase() {
    User dave = new User("Dave", "Matthews");
    dave = repository.save(dave);
         
    User carter = new User("Carter", "Beauford");
    carter = repository.save(carter);
         
    List<User> result = repository.findByLastname("Matthews");
    assertThat(result.size(), is(1));
    assertThat(result, hasItem(dave));
  }
}

```

## Contributing to Spring Data MyBatis ##

Here are some ways for you to get involved in the community:

* Github is for social coding: if you want to write code, we encourage contributions through pull requests from [forks of this repository](http://help.github.com/forking/). 
