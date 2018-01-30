package org.springframework.data.mybatis.repository.query;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.mybatis.mapping.MyBatisMappingContext;
import org.springframework.data.mybatis.repository.support.MyBatisMapperBuilderAssistant;

/**
 * @author Jarvis Song
 */
abstract class AbstractStringBasedMyBatisQuery extends AbstractMyBatisQuery {

	public AbstractStringBasedMyBatisQuery(MyBatisQueryMethod method, MyBatisMapperBuilderAssistant mapperSupport,
			MyBatisMappingContext context, SqlSessionTemplate template, Dialect dialect) {
		super(method, mapperSupport, context, template, dialect);
	}
}
