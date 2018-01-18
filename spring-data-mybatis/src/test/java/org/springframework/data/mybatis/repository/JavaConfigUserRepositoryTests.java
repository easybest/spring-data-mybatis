package org.springframework.data.mybatis.repository;

import org.junit.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.mybatis.domain.sample.User;
import org.springframework.data.mybatis.repository.config.EnableMyBatisRepositories;
import org.springframework.data.mybatis.repository.sample.UserRepository;
import org.springframework.data.mybatis.repository.support.MyBatisRepositoryFactoryBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@ContextConfiguration(inheritLocations = false, loader = AnnotationConfigContextLoader.class)
public class JavaConfigUserRepositoryTests extends UserRepositoryTests {

	@Configuration
	@ImportResource("classpath:infrastructure.xml")
	static class Config {

		@Autowired ApplicationContext applicationContext;
		@Autowired SqlSessionTemplate sqlSessionTemplate;

		@Bean
		public UserRepository userRepository() {
			MyBatisRepositoryFactoryBean<UserRepository, User, Long> factory = new MyBatisRepositoryFactoryBean<>(
					UserRepository.class);
			factory.setSqlSessionTemplate(sqlSessionTemplate);
			factory.setBeanFactory(applicationContext);
			factory.afterPropertiesSet();
			return factory.getObject();
		}
	}

	@Test(expected = NoSuchBeanDefinitionException.class)
	public void doesNotPickUpJpaRepository() {

		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(MyBatisRepositoryConfig.class);
		context.getBean("mybatisRepository");
		context.close();
	}

	@Configuration
	@EnableMyBatisRepositories(basePackageClasses = UserRepository.class)
	// @EnableTransactionManagement
	@ImportResource("classpath:infrastructure.xml")
	static class MyBatisRepositoryConfig {

	}
}
