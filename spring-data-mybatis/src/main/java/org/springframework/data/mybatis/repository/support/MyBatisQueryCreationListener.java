package org.springframework.data.mybatis.repository.support;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.mybatis.mapping.MyBatisMappingContext;
import org.springframework.data.mybatis.repository.query.PartTreeMyBatisQuery;
import org.springframework.data.repository.core.support.QueryCreationListener;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * @author Jarvis Song
 */
public class MyBatisQueryCreationListener implements QueryCreationListener<RepositoryQuery> {

	private final SqlSessionTemplate sqlSessionTemplate;
	private final MyBatisMappingContext mappingContext;
	private final Dialect dialect;

	public MyBatisQueryCreationListener(SqlSessionTemplate sqlSessionTemplate, MyBatisMappingContext mappingContext,
			Dialect dialect) {
		this.sqlSessionTemplate = sqlSessionTemplate;
		this.mappingContext = mappingContext;
		this.dialect = dialect;
	}

	@Override
	public void onCreation(RepositoryQuery query) {
		if (query instanceof PartTreeMyBatisQuery) {
			PartTreeMyBatisMapperBuilderAssistant assistant = new PartTreeMyBatisMapperBuilderAssistant(sqlSessionTemplate,
					mappingContext, dialect, (PartTreeMyBatisQuery) query);
			assistant.prepare();
		}

	}
}
