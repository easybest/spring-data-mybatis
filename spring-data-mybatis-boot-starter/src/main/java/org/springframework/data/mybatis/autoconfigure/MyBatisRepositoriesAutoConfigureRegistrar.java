package org.springframework.data.mybatis.autoconfigure;

import java.lang.annotation.Annotation;
import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.data.mybatis.repository.config.EnableMybatisRepositories;
import org.springframework.data.mybatis.repository.config.MybatisRepositoryConfigExtension;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

/**
 * {@link ImportBeanDefinitionRegistrar} used to auto-configure Spring Data MyBatis
 * Repositories.
 * 
 * @author Jarvis Song
 */
class MyBatisRepositoriesAutoConfigureRegistrar
		extends AbstractRepositoryConfigurationSourceSupport {

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableMybatisRepositories.class;
	}

	@Override
	protected Class<?> getConfiguration() {
		return EnableMyBatisRepositoriesConfiguration.class;
	}

	@Override
	protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
		return new MybatisRepositoryConfigExtension();
	}

	@EnableMybatisRepositories
	private static class EnableMyBatisRepositoriesConfiguration {

	}

}
