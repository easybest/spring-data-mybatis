package org.springframework.data.mybatis.repository.support;

import lombok.extern.slf4j.Slf4j;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.mybatis.mapping.MyBatisMappingContext;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryProxyPostProcessor;

/**
 * @author Jarvis Song
 */
@Slf4j
public class MyBatisMapperBuildProcessor implements RepositoryProxyPostProcessor {

	private final SqlSessionTemplate sqlSessionTemplate;
	private final MyBatisMappingContext mappingContext;
	private final Dialect dialect;

	public MyBatisMapperBuildProcessor(SqlSessionTemplate sqlSessionTemplate, MyBatisMappingContext mappingContext,
			Dialect dialect) {
		this.sqlSessionTemplate = sqlSessionTemplate;
		this.mappingContext = mappingContext;
		this.dialect = dialect;
	}

	@Override
	public void postProcess(ProxyFactory factory, RepositoryInformation repositoryInformation) {

		SimpleMyBatisMapperBuilderAssistant assistant = new SimpleMyBatisMapperBuilderAssistant(sqlSessionTemplate,
				mappingContext, dialect, repositoryInformation);
		assistant.prepare();
	}
}
