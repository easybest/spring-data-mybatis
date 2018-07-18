package org.springframework.data.mybatis.repository.query;

import org.mybatis.spring.SqlSessionTemplate;

/**
 * @author Jarvis Song
 */
final class SimpleMyBatisQuery extends AbstractStringBasedMyBatisQuery {

	public SimpleMyBatisQuery(SqlSessionTemplate template, MyBatisQueryMethod method) {
		super(template, method);
	}

	@Override
	protected MyBatisQueryExecution getExecution() {
		return null;
	}
}
