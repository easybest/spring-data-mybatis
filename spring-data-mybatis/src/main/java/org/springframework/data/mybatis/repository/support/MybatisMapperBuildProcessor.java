package org.springframework.data.mybatis.repository.support;

import org.apache.ibatis.session.Configuration;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryProxyPostProcessor;

public class MybatisMapperBuildProcessor implements RepositoryProxyPostProcessor {

	private final Configuration configuration;

	private final MappingContext<?, ?> mappingContext;

	public MybatisMapperBuildProcessor(Configuration configuration,
			MappingContext<?, ?> mappingContext) {

		this.configuration = configuration;
		this.mappingContext = mappingContext;
	}

	@Override
	public void postProcess(ProxyFactory factory,
			RepositoryInformation repositoryInformation) {

		new MybatisBasicMapperBuilder(configuration, repositoryInformation, mappingContext
				.getRequiredPersistentEntity(repositoryInformation.getDomainType()))
						.build();

	}

}
