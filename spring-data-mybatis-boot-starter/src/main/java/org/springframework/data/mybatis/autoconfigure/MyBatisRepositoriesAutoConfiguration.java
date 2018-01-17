package org.springframework.data.mybatis.autoconfigure;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mybatis.repository.MyBatisRepository;
import org.springframework.data.mybatis.repository.config.MyBatisRepositoryConfigExtension;
import org.springframework.data.mybatis.repository.support.MyBatisRepositoryFactoryBean;

import javax.sql.DataSource;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's MyBatis Repositories.
 * 
 * @author Jarvis Song
 */
@Configuration
@ConditionalOnBean(DataSource.class)
@ConditionalOnClass(MyBatisRepository.class)
@ConditionalOnMissingBean({ MyBatisRepositoryFactoryBean.class, MyBatisRepositoryConfigExtension.class })
@ConditionalOnProperty(prefix = "spring.data.mybatis.repositories", name = "enabled", havingValue = "true",
		matchIfMissing = true)
@Import(MyBatisRepositoriesAutoConfigureRegistrar.class)
public class MyBatisRepositoriesAutoConfiguration {}
