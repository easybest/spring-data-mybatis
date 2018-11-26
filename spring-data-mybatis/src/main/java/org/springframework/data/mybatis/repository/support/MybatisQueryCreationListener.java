package org.springframework.data.mybatis.repository.support;

import org.apache.ibatis.session.Configuration;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mybatis.repository.query.PartTreeMybatisQuery;
import org.springframework.data.mybatis.repository.query.SimpleMybatisQuery;
import org.springframework.data.repository.core.support.QueryCreationListener;
import org.springframework.data.repository.query.RepositoryQuery;

public class MybatisQueryCreationListener
		implements QueryCreationListener<RepositoryQuery> {

	private final Configuration configuration;

	private final MappingContext<?, ?> mappingContext;

	public MybatisQueryCreationListener(Configuration configuration,
			MappingContext<?, ?> mappingContext) {

		this.configuration = configuration;
		this.mappingContext = mappingContext;
	}

	@Override
	public void onCreation(RepositoryQuery query) {

		if (query instanceof PartTreeMybatisQuery) {
			new MybatisPartTreeMapperBuilder(configuration,
					mappingContext.getPersistentEntity(
							query.getQueryMethod().getEntityInformation().getJavaType()),
					(PartTreeMybatisQuery) query).build();
		}
		else if (query instanceof SimpleMybatisQuery) {
			new MybatisSimpleQueryMapperBuilder(configuration,
					mappingContext.getPersistentEntity(
							query.getQueryMethod().getEntityInformation().getJavaType()),
					(SimpleMybatisQuery) query).build();
		}
	}

}
