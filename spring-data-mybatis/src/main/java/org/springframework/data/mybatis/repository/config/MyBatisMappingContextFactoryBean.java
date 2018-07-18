package org.springframework.data.mybatis.repository.config;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.mybatis.mapping.MyBatisMappingContext;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Jarvis Song
 */
public class MyBatisMappingContextFactoryBean extends AbstractFactoryBean<MyBatisMappingContext>
		implements ResourceLoaderAware {

	private @Nullable RepositoryConfigurationSource repositoryConfigurationSource;
	private @Nullable Set<? extends Class<?>> initialEntitySet;
	private ResourceLoader resourceLoader;
	private ClassLoader classLoader;

	@Override
	public Class<?> getObjectType() {
		return MyBatisMappingContext.class;
	}

	@Override
	protected MyBatisMappingContext createInstance() {
		MyBatisMappingContext context = new MyBatisMappingContext();

		if (null != repositoryConfigurationSource) {
			Set<Class<?>> entitySources = repositoryConfigurationSource.getCandidates(resourceLoader)
					.map(candidate -> loadRepositoryInterface(candidate.getBeanClassName()))
					.filter(repositoryInterface -> null != repositoryInterface).map(AbstractRepositoryMetadata::getMetadata)
					.filter(metadata -> null != metadata).map(metadata -> metadata.getDomainType()).stream()
					.collect(Collectors.toSet());
			context.setInitialEntitySet(entitySources);
		} else if (null != initialEntitySet) {
			context.setInitialEntitySet(initialEntitySet);
		}

		context.initialize();
		return context;
	}

	private Class<?> loadRepositoryInterface(String repositoryInterface) {
		try {
			return ClassUtils.forName(repositoryInterface, classLoader);
		} catch (ClassNotFoundException e) {}
		return null;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {

		this.classLoader = classLoader;
		super.setBeanClassLoader(classLoader);
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {

		this.resourceLoader = resourceLoader;
	}

	public void setRepositoryConfigurationSource(@Nullable RepositoryConfigurationSource repositoryConfigurationSource) {
		Assert.notNull(repositoryConfigurationSource, "RepositoryConfigurationSource must not be null!");
		this.repositoryConfigurationSource = repositoryConfigurationSource;
	}

	public void setInitialEntitySet(@Nullable Set<? extends Class<?>> initialEntitySet) {
		this.initialEntitySet = initialEntitySet;
	}

}
