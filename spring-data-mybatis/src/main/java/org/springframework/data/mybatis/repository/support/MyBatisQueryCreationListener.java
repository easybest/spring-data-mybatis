package org.springframework.data.mybatis.repository.support;

import org.apache.ibatis.session.Configuration;
import org.springframework.data.mybatis.mapping.MyBatisMappingContext;
import org.springframework.data.mybatis.repository.query.PartTreeMyBatisQuery;
import org.springframework.data.repository.core.support.QueryCreationListener;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * @author Jarvis Song
 */
public class MyBatisQueryCreationListener implements QueryCreationListener<RepositoryQuery> {

	private final Configuration configuration;
	private final MyBatisMappingContext mappingContext;

	public MyBatisQueryCreationListener(Configuration configuration, MyBatisMappingContext mappingContext) {

		this.configuration = configuration;
		this.mappingContext = mappingContext;
	}

	@Override
	public void onCreation(RepositoryQuery query) {
		if (query instanceof PartTreeMyBatisQuery) {
			PartTreeMyBatisMapperBuilderAssistant assistant = new PartTreeMyBatisMapperBuilderAssistant(configuration,
					mappingContext, (PartTreeMyBatisQuery) query);
			assistant.prepare();
		}

	}
}
