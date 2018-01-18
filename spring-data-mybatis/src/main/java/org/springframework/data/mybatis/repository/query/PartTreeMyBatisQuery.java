package org.springframework.data.mybatis.repository.query;

import org.mybatis.spring.SqlSessionTemplate;

/**
 * @author Jarvis Song
 */
public class PartTreeMyBatisQuery extends AbstractMyBatisQuery {

	PartTreeMyBatisQuery(MybatisQueryMethod method, SqlSessionTemplate template) {
		super(method, template);
	}
}
