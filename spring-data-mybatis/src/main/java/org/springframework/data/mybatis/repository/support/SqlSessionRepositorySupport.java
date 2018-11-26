package org.springframework.data.mybatis.repository.support;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Convenient super class for MyBatis SqlSession data access objects. It gives you access
 * to the template which can then be used to execute SQL methods.
 *
 * @author JARVIS SONG
 */
public abstract class SqlSessionRepositorySupport {

	public static final char DOT = '.';

	private final SqlSessionTemplate sqlSession;

	protected SqlSessionRepositorySupport(SqlSessionTemplate sqlSessionTemplate) {
		Assert.notNull(sqlSessionTemplate, "SqlSessionTemplate must not be null!");
		this.sqlSession = sqlSessionTemplate;
	}

	public SqlSessionTemplate getSqlSession() {
		return sqlSession;
	}

	/**
	 * Sub class can override this method.
	 * @return Namespace
	 */
	protected abstract String getNamespace();

	/**
	 * get the mapper statement include namespace.
	 * @param partStatement partStatement
	 * @return Statement
	 */
	protected String getStatement(String partStatement) {
		return getNamespace() + DOT + partStatement;
	}

	/**
	 * select one query.
	 * @param statement statement
	 * @param <T> entity class
	 * @return result
	 */
	protected <T> T selectOne(String statement) {
		return getSqlSession().selectOne(getStatement(statement));
	}

	protected <T> T selectOne(String statement, Object parameter) {
		return getSqlSession().selectOne(getStatement(statement), parameter);
	}

	protected <T> List<T> selectList(String statement) {
		return getSqlSession().selectList(getStatement(statement));
	}

	protected <T> List<T> selectList(String statement, Object parameter) {
		return getSqlSession().selectList(getStatement(statement), parameter);
	}

	protected int insert(String statement, Object parameter) {
		return getSqlSession().insert(getStatement(statement), parameter);
	}

	protected int update(String statement, Object parameter) {
		return getSqlSession().update(getStatement(statement), parameter);
	}

	protected int delete(String statement) {
		return getSqlSession().delete(getStatement(statement));
	}

	protected int delete(String statement, Object parameter) {
		return getSqlSession().delete(getStatement(statement), parameter);
	}

	/**
	 * Calculate total mount.
	 * @return if return -1 means can not judge ,need count from database.
	 */
	protected <X> long calculateTotal(Pageable pager, List<X> result) {
		if (pager.hasPrevious()) {
			if (CollectionUtils.isEmpty(result)) {
				return -1;
			}
			if (result.size() == pager.getPageSize()) {
				return -1;
			}
			return (pager.getPageNumber() - 1) * pager.getPageSize() + result.size();
		}
		if (result.size() < pager.getPageSize()) {
			return result.size();
		}
		return -1;
	}

	protected <X, Y> Page<X> findByPager(Pageable pager, String selectStatement,
			String countStatement, Y condition, Map<String, Object> otherParams) {
		Map<String, Object> params = new HashMap<>();
		params.put("__offset", pager.getOffset());
		params.put("__pageSize", pager.getPageSize());
		params.put("__offsetEnd", pager.getOffset() + pager.getPageSize());
		if (condition instanceof Sort && ((Sort) condition).isSorted()) {
			params.put("__sort", condition);
		}
		else if (null != pager && null != pager.getSort() && pager.getSort().isSorted()) {
			params.put("__sort", pager.getSort());
		}
		params.put("__condition", condition);

		if (!CollectionUtils.isEmpty(otherParams)) {
			params.putAll(otherParams);
		}
		List<X> result = selectList(selectStatement, params);

		long total = calculateTotal(pager, result);
		if (total < 0) {
			total = selectOne(countStatement, params);
		}

		return new PageImpl<>(result, pager, total);
	}

	protected <X, Y> Page<X> findByPager(Pageable pager, String selectStatement,
			String countStatement, Y condition) {
		return findByPager(pager, selectStatement, countStatement, condition, null);
	}

}
