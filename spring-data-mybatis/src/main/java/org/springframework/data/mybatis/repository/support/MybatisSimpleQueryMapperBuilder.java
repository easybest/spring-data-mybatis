package org.springframework.data.mybatis.repository.support;

import java.util.Map;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mybatis.repository.query.InvalidMybatisQueryMethodException;
import org.springframework.data.mybatis.repository.query.MybatisParameters.MybatisParameter;
import org.springframework.data.mybatis.repository.query.MybatisQueryMethod;
import org.springframework.data.mybatis.repository.query.SimpleMybatisQuery;
import org.springframework.data.mybatis.repository.query.StringQuery;
import org.springframework.data.mybatis.repository.query.StringQuery.InParameterBinding;
import org.springframework.data.mybatis.repository.query.StringQuery.LikeParameterBinding;
import org.springframework.data.mybatis.repository.query.StringQuery.ParameterBinding;
import org.springframework.data.repository.query.Parameters;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandler;

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

		if (method.getAnnotatedQuery().trim().startsWith("<script>")) {
			if (null != method.getResultMap()) {
				addMappedStatement(method.getStatementId(),
						new String[] { method.getAnnotatedQuery() }, commandType,
						Map.class, method.getResultMap());
			}
			else {
				addMappedStatement(method.getStatementId(),
						new String[] { method.getAnnotatedQuery() }, commandType,
						Map.class, method.getReturnedObjectType());
			}
			return;
		}

		String sql = stringQuery.getQueryString();

		Parameters<?, ?> parameters = method.getParameters();

		if (parameters.hasSortParameter() && !sql.contains("#sort")) {
			throw new InvalidMybatisQueryMethodException(
					"Cannot use native queries with dynamic sorting in method " + method);
		}

		if (!CollectionUtils.isEmpty(stringQuery.getParameterBindings())) {
			for (ParameterBinding parameterBinding : stringQuery.getParameterBindings()) {

				String replace, bindName;

				MybatisParameter mp = method.getParameters()
						.getBindableParameter(parameterBinding.getRequiredPosition() - 1);

				Class<? extends TypeHandler<?>> typeHandler = mp
						.getSpecifiedTypeHandler();

				if (StringUtils.hasText(parameterBinding.getName())) {
					replace = ":" + parameterBinding.getName();
					bindName = parameterBinding.getName();
				}
				else {
					replace = "?" + parameterBinding.getPosition();
					bindName = mp.getName().orElse("__p" + mp.getIndex());
				}

				if (parameterBinding instanceof InParameterBinding) {
					sql = sql.replace(replace,
							"<foreach item=\"__item\" index=\"__index\" collection=\""
									+ bindName
									+ "\" open=\"(\" separator=\",\" close=\")\">#{__item"
									+ (null != typeHandler
											? (",typeHandler=" + typeHandler.getName())
											: "")
									+ "}</foreach>");

					continue;
				}

				if (parameterBinding instanceof LikeParameterBinding) {

					LikeParameterBinding likeParameterBinding = (LikeParameterBinding) parameterBinding;

					// "<bind name=\"").append(bind).append("\" value=\"" + properties[0]
					// + " + '%'\" />"

					String like = bindName;
					switch (likeParameterBinding.getType()) {
					case CONTAINING:
					case LIKE:
						like = "'%' + " + bindName + " + '%'";
						break;
					case STARTING_WITH:
						like = bindName + " + '%'";
						break;
					case ENDING_WITH:
						like = "'%' + " + bindName;
						break;
					}

					sql = sql.replace(replace, "<bind name=\"__bind_" + bindName
							+ "\" value=\"" + like + "\" /> #{__bind_" + bindName + "}");

					continue;

				}

				sql = sql.replace(replace,
						"#{" + bindName
								+ (null != typeHandler
										? (",typeHandler=" + typeHandler.getName()) : "")
								+ "}");

			}

		}

		String[] sqls;
		if (XML_PATTERN.matcher(sql).find()) {
			sqls = new String[] { "<script>", sql, "</script>" };
		}
		else {
			sqls = new String[] { sql.replace("<![CDATA[", "").replace("]]>", "") };
		}

		if (null != method.getResultMap()) {
			addMappedStatement(method.getStatementId(), sqls, commandType, Map.class,
					method.getResultMap());
		}
		else {
			addMappedStatement(method.getStatementId(), sqls, commandType, Map.class,
					method.getReturnedObjectType());
		}
	}

}
