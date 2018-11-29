# Spring Data MyBatis 
[![Build Status](https://travis-ci.org/hatunet/spring-data-mybatis.svg?branch=master)](https://travis-ci.org/hatunet/spring-data-mybatis)   [![Gitter chat](https://badges.gitter.im/gitterHQ/gitter.png)](https://gitter.im/spring-data-mybatis)
                                                                                                                                                                 


[1.x Document](README_1.x.md)

The primary goal of the Spring Data project is to make it easier to build Spring-powered applications that use data access technologies. 
This module deals with enhanced support for MyBatis based data access layers.


## Features ##

* Implementation of CRUD methods for normal Entities
* Dynamic query generation from query method names
* Implementation domain base classes providing basic properties
* Support for transparent auditing (created, last changed)
* Possibility to integrate custom repository code
* Easy Spring integration with custom namespace
* Support MySQL, Oracle, Sql Server, H2, etc.
* Support SpringBoot 2.x
* Due to some reason we no longer support Features about query data from more than one tables, like: @ManyToOne, @OneToMany etc. we suggest to write sql in mapper files or use @Query with sql in repository methods.



## Getting Help ##
This README as well as the [reference documentation](https://hatunet.github.io/spring-data-mybatis/) are the best places to start learning about Spring Data MyBatis. 

If you have any question, please record a [issue](https://github.com/hatunet/spring-data-mybatis/issues) to me.


## Quick Start ##

Download the jar through Maven:

```xml
<dependency>
  <groupId>com.ifrabbit</groupId>
  <artifactId>spring-data-mybatis</artifactId>
  <version>2.0.0.BUILD-SNAPSHOT</version>
</dependency>
```


you should add repository configuration to your pom.xml like this:

```xml
<repository>
 <id>oss-snapshots-repo</id>
 <url>https://oss.sonatype.org/content/repositories/snapshots</url>
 <releases><enabled>false</enabled></releases>
 <snapshots><enabled>true</enabled></snapshots>
</repository>
```


The simple Spring Data Mybatis configuration with Java-Config looks like this: 
```java
@Configuration
@EnableMybatisRepositories(
        value = "org.springframework.data.mybatis.repository.sample",
        mapperLocations = "classpath*:/org/springframework/data/mybatis/repository/sample/mappers/*Mapper.xml"
)
public class TestConfig {

    @Bean
    public DataSource dataSource() throws SQLException {
        return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).addScript("classpath:/test-init.sql").build();
    }

    @Bean
    public SqlSessionFactoryBean sqlSessionFactory(DataSource dataSource) {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        return factoryBean;
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

}

```

Create an entity:

```java
@Entity
@Table(name = "user")
public class User extends LongId {

  
  @Condition
  private String firstname;
  @Condition(type=Condition.Type.CONTAINING)  private String lastname;
  private String fullName;
  @Conditions({@Condition, @Condition(type=Condition.Type.CONTAINING,properties = "fuzzyName")})
  private String fullName;
  @Transient
  private String fuzzyName;
  @Column(name = "usertype")
  private String status;
  // Getters and setters
  // (Firstname, Lastname)-constructor and noargs-constructor
  // equals / hashcode
}

```
When using findAll method of MybatisRepository the @Condition or @Conditions annotations will work


Create a repository interface in `com.example.repositories`:

```java
public interface UserRepository extends MybatisRepository<User, Long> {
  List<User> findByLastname(String lastname);  
  
  @Query("select firstname from user")
  List<String> findUsersFirstName();
  
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


## Use Spring Boot

add the jar through Maven:
   
   ```xml
   <dependency>
       <groupId>com.ifrabbit</groupId>
       <artifactId>spring-boot-starter-data-mybatis</artifactId>
       <version>2.0.0.BUILD-SNAPSHOT</version>
   </dependency>
   ```

If you need custom Mapper, you should add property in your application.properties like this:
```
mybatis.mapper-locations=classpath*:/mapper/**/**Mapper.xml
```

And you need not to define SqlSessionFactory manually.

The full test code like this:

```java
@SpringBootApplication
public class SpringDataMybatisSamplesApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringDataMybatisSamplesApplication.class, args);
    }

    @Bean
    public CommandLineRunner dummyCLR(ReservationRepository reservationRepository) {
        return args -> {
            Stream.of("Tom", "Jack", "Apple")
                    .forEach(name -> reservationRepository.save(new Reservation(name)));
        };
    }

}

@RepositoryRestResource // here we use RepositoryRestResource
interface ReservationRepository extends MybatisRepository<Reservation, Long> {
}

@Entity
@Table(name = "user")
class Reservation extends LongId {

    private String reservationName;

    public Reservation() {
    }

    public Reservation(String reservationName) {
        this.reservationName = reservationName;
    }

    public String getReservationName() {
        return reservationName;
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "reservationName='" + reservationName + '\'' +
                '}';
    }
}
```

The full example you can find in [https://github.com/hatunet/spring-data-mybatis-samples](https://github.com/hatunet/spring-data-mybatis-samples)


## Contributing to Spring Data MyBatis ##

Here are some ways for you to get involved in the community:

* Github is for social coding: if you want to write code, we encourage contributions through pull requests from [forks of this repository](http://help.github.com/forking/). 

## Help me better - Donation
[![paypal](https://www.paypal.com/en_US/i/btn/x-click-butcc-donate.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=W7PLNCBK5K8JS)

