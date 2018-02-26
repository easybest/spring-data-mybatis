package org.springframework.data.mybatis.repository.support;

import lombok.extern.slf4j.Slf4j;

import org.apache.ibatis.session.Configuration;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.mybatis.mapping.MyBatisMappingContext;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryProxyPostProcessor;

/**
 * @author Jarvis Song
 */
@Slf4j
public class MyBatisMapperBuildProcessor implements RepositoryProxyPostProcessor {

	private final Configuration configuration;
	private final MyBatisMappingContext mappingContext;

	public MyBatisMapperBuildProcessor(Configuration configuration, MyBatisMappingContext mappingContext) {

		this.configuration = configuration;
		this.mappingContext = mappingContext;
	}

	@Override
	public void postProcess(ProxyFactory factory, RepositoryInformation repositoryInformation) {

		SimpleMyBatisMapperBuilderAssistant assistant = new SimpleMyBatisMapperBuilderAssistant(configuration,
				mappingContext, repositoryInformation);
		assistant.prepare();
	}
}
