package org.springframework.data.mybatis.autoconfigure;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mybatis.id.Snowflake;
import org.springframework.data.mybatis.repository.MybatisRepository;
import org.springframework.data.mybatis.repository.config.MybatisRepositoryConfigExtension;
import org.springframework.data.mybatis.repository.support.MybatisRepositoryFactoryBean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's MyBatis
 * Repositories.
 * 
 * @author Jarvis Song
 */
@Configuration
@ConditionalOnBean(DataSource.class)
@ConditionalOnClass(MybatisRepository.class)
@ConditionalOnMissingBean({ MybatisRepositoryFactoryBean.class,
		MybatisRepositoryConfigExtension.class })
@ConditionalOnProperty(prefix = "spring.data.mybatis.repositories", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SpringDataMybatisProperties.class)
@Import(MyBatisRepositoriesAutoConfigureRegistrar.class)
public class MyBatisRepositoriesAutoConfiguration {

	private SpringDataMybatisProperties properties;

	public MyBatisRepositoriesAutoConfiguration(SpringDataMybatisProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnProperty(prefix = "spring.data.mybatis.repositories.snowflake", name = "enabled", havingValue = "true")
	@ConditionalOnMissingBean(Snowflake.class)
	public Snowflake snowflake() {
		return new Snowflake(properties.getSnowflake().getWorkerId(),
				properties.getSnowflake().getDatacenterId());
	}

}
