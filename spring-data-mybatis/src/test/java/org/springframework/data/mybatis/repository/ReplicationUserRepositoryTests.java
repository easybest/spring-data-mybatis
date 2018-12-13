package org.springframework.data.mybatis.repository;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.mybatis.domain.sample.User;
import org.springframework.data.mybatis.repository.config.EnableMybatisAuditing;
import org.springframework.data.mybatis.repository.config.EnableMybatisRepositories;
import org.springframework.data.mybatis.repository.sample.UserRepository;
import org.springframework.data.mybatis.repository.support.MybatisRepositoryFactoryBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import org.junit.Test;
import org.mybatis.spring.SqlSessionTemplate;

@ContextConfiguration(inheritLocations = false, loader = AnnotationConfigContextLoader.class)
public class ReplicationUserRepositoryTests extends UserRepositoryTests {

	@Configuration
	@ImportResource("classpath:infrastructure-replication.xml")
	static class Config {

		@Autowired
		ApplicationContext applicationContext;

		@Autowired
		SqlSessionTemplate sqlSessionTemplate;

		@Bean
		public UserRepository userRepository() {
			MybatisRepositoryFactoryBean<UserRepository, User, Integer> factory = new MybatisRepositoryFactoryBean<>(
					UserRepository.class);
			factory.setSqlSessionTemplate(sqlSessionTemplate);
			factory.setBeanFactory(applicationContext);
			factory.afterPropertiesSet();
			return factory.getObject();
		}

		@Bean
		public CurrentUserAuditorAware auditorAware() {
			return new CurrentUserAuditorAware();
		}

	}

	@Test(expected = NoSuchBeanDefinitionException.class)
	public void doesNotPickUpMyBatisRepository() {

		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				MyBatisRepositoryConfig.class);
		context.getBean("mybatisRepository");
		context.close();
	}

	@Configuration
	@EnableMybatisAuditing
	@EnableMybatisRepositories(basePackageClasses = UserRepository.class)
	// @EnableTransactionManagement
	@ImportResource("classpath:infrastructure-replication.xml")
	static class MyBatisRepositoryConfig {

	}

}
