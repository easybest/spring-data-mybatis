package org.springframework.data.mybatis.repository.query;

import org.mybatis.spring.SqlSessionTemplate;

/**
 * @author Jarvis Song
 */
abstract class AbstractStringBasedMyBatisQuery extends AbstractMyBatisQuery {

	public AbstractStringBasedMyBatisQuery(SqlSessionTemplate template, MyBatisQueryMethod method) {
		super(template, method);
	}
}
