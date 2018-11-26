package org.springframework.data.mybatis.repository.support;

import java.util.Map;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mybatis.repository.query.InvalidMybatisQueryMethodException;
import org.springframework.data.mybatis.repository.query.MybatisQueryMethod;
import org.springframework.data.mybatis.repository.query.SimpleMybatisQuery;
import org.springframework.data.mybatis.repository.query.StringQuery;
import org.springframework.data.mybatis.repository.query.StringQuery.ParameterBinding;
import org.springframework.data.repository.query.Parameters;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public class MybatisSimpleQueryMapperBuilder extends MybatisMapperBuildAssistant {

	private final MybatisQueryMethod method;

	private final StringQuery stringQuery;

	private final SqlCommandType commandType;

	public MybatisSimpleQueryMapperBuilder(Configuration configuration,
			PersistentEntity<?, ?> persistentEntity, SimpleMybatisQuery query) {
		super(configuration, persistentEntity, query.getQueryMethod().getNamespace());

		method = query.getQueryMethod();
		stringQuery = query.getStringQuery();
		commandType = query.getCommandType();
	}

	@Override
	protected void doBuild() {

		if (StringUtils.hasText(method.getAnnotatedQuery())) {
			buildQuery();
		}

	}

	private void buildQuery() {

		String sql = method.getAnnotatedQuery();

		Parameters<?, ?> parameters = method.getParameters();

		if (parameters.hasSortParameter() && !sql.contains("#sort")) {
			throw new InvalidMybatisQueryMethodException(
					"Cannot use native queries with dynamic sorting in method " + method);
		}

		if (!CollectionUtils.isEmpty(stringQuery.getParameterBindings())) {
			for (ParameterBinding parameterBinding : stringQuery.getParameterBindings()) {

				if (StringUtils.hasText(parameterBinding.getName())) {
					sql = sql.replace(":" + parameterBinding.getName(),
							"#{" + parameterBinding.getName() + "}");
				}
				else if (null != parameterBinding.getPosition()) {
					sql = sql.replace("?" + parameterBinding.getPosition(),
							"#{p" + (parameterBinding.getPosition() - 1) + "}");
				}
			}

		}

		if (null != parameters && !parameters.isEmpty()) {

		}

		addMappedStatement(method.getStatementId(), new String[] { sql }, commandType,
				Map.class, method.getReturnedObjectType());

	}

}
