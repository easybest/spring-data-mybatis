package org.springframework.data.mybatis.autoconfigure;

import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.data.mybatis.repository.config.EnableMyBatisRepositories;
import org.springframework.data.mybatis.repository.config.MyBatisRepositoryConfigExtension;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

import java.lang.annotation.Annotation;

/**
 * {@link ImportBeanDefinitionRegistrar} used to auto-configure Spring Data MyBatis Repositories.
 * 
 * @author Jarvis Song
 */
class MyBatisRepositoriesAutoConfigureRegistrar extends AbstractRepositoryConfigurationSourceSupport {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableMyBatisRepositories.class;
	}

	@Override
	protected Class<?> getConfiguration() {
		return EnableMyBatisRepositoriesConfiguration.class;
	}

	@Override
	protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
		return new MyBatisRepositoryConfigExtension();
	}

	@EnableMyBatisRepositories
	private static class EnableMyBatisRepositoriesConfiguration {

	}
}
