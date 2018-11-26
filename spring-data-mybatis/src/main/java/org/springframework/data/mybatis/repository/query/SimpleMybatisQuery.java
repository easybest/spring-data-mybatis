package org.springframework.data.mybatis.repository.query;

import static org.apache.ibatis.mapping.SqlCommandType.DELETE;
import static org.apache.ibatis.mapping.SqlCommandType.INSERT;
import static org.apache.ibatis.mapping.SqlCommandType.SELECT;
import static org.apache.ibatis.mapping.SqlCommandType.UNKNOWN;
import static org.apache.ibatis.mapping.SqlCommandType.UPDATE;

import org.apache.ibatis.mapping.SqlCommandType;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.mybatis.repository.query.MybatisQueryExecution.DeleteExecution;
import org.springframework.util.StringUtils;

public class SimpleMybatisQuery extends AbstractMybatisQuery {

	private StringQuery stringQuery = null;

	private MybatisQueryExecution execution;

	private SqlCommandType commandType;

	protected SimpleMybatisQuery(MybatisQueryMethod method,
			SqlSessionTemplate sqlSessionTemplate) {
		super(method, sqlSessionTemplate);

		commandType = SELECT;

		if (StringUtils.hasText(method.getAnnotatedQuery())) {
			String sql = method.getAnnotatedQuery();
			stringQuery = new StringQuery(sql);

			if (method.isModifyingQuery()) {
				SqlCommandType type = method.getModifyingType();
				if (null == type) {
					// auto judge
					if (sql.toLowerCase().startsWith("insert")) {
						commandType = INSERT;
					}
					else if (sql.toLowerCase().startsWith("update")) {
						commandType = UPDATE;
					}
					else if (sql.toLowerCase().startsWith("delete")) {
						commandType = DELETE;
					}
					else {
						commandType = UNKNOWN;
					}
				}
				else {
					commandType = type;
				}
			}

		}
		this.execution = createExecution();

	}

	@Override
	protected MybatisQueryExecution getExecution() {

		return execution;
	}

	public StringQuery getStringQuery() {
		return stringQuery;
	}

	@Override
	protected MybatisQueryExecution createExecution() {
		if (commandType == DELETE) {
			return new DeleteExecution();
		}
		return super.createExecution();
	}

	public SqlCommandType getCommandType() {
		return commandType;
	}

}
