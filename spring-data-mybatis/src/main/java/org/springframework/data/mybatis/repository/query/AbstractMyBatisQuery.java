package org.springframework.data.mybatis.repository.query;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * @author songjiawei
 */
public abstract class AbstractMyBatisQuery implements RepositoryQuery {

	private final MybatisQueryMethod method;
	private final SqlSessionTemplate template;

	public AbstractMyBatisQuery(MybatisQueryMethod method, SqlSessionTemplate template) {
		this.method = method;
		this.template = template;
	}

	@Override
	public Object execute(Object[] parameters) {
		return null;
	}

	@Override
	public QueryMethod getQueryMethod() {
		return method;
	}
}
