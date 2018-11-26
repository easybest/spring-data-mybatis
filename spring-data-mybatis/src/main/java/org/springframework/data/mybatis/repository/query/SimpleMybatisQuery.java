package org.springframework.data.mybatis.repository.query;

import org.mybatis.spring.SqlSessionTemplate;

public class SimpleMybatisQuery extends AbstractMybatisQuery {

	protected SimpleMybatisQuery(MybatisQueryMethod method,
			SqlSessionTemplate sqlSessionTemplate) {
		super(method, sqlSessionTemplate);
	}

	@Override
	protected MybatisQueryExecution getExecution() {
		return null;
	}

}
