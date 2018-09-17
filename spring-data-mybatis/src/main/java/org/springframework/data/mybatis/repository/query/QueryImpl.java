package org.springframework.data.mybatis.repository.query;

import com.mysema.commons.lang.Assert;
import org.mybatis.spring.SqlSessionTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryImpl<R> implements Query<R> {

	private final SqlSessionTemplate sqlSessionTemplate;
	private final String statement;

	private Map<String, Integer> paramsCache = new HashMap<>();

	public QueryImpl(SqlSessionTemplate sqlSessionTemplate, String statement) {
		Assert.notNull(sqlSessionTemplate, "sqlSessionTemplate can not be null.");
		Assert.notNull(statement, "statement can not be null.");
		this.sqlSessionTemplate = sqlSessionTemplate;
		this.statement = statement;
	}

	@Override
	public List<R> getResultList(Object[] values) {
		return null;
	}

	@Override
	public R getSingleResult(Object[] values) {
		if (null == values) {
			return sqlSessionTemplate.selectOne(statement);
		}

		Map<String,Object> params = paramsCache.entrySet().stream().map(entry->)


		return null;
	}

	@Override
	public int executeUpdate(Object[] values) {
		return 0;
	}
}
