/*
 * Copyright 2019-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.easybest.mybatis.repository.query;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import io.easybest.mybatis.auxiliary.Syntax;
import io.easybest.mybatis.dialect.Dialect;
import io.easybest.mybatis.mapping.EntityManager;
import io.easybest.mybatis.mapping.precompile.Choose;
import io.easybest.mybatis.mapping.precompile.Delete;
import io.easybest.mybatis.mapping.precompile.Foreach;
import io.easybest.mybatis.mapping.precompile.Function;
import io.easybest.mybatis.mapping.precompile.Insert;
import io.easybest.mybatis.mapping.precompile.MethodInvocation;
import io.easybest.mybatis.mapping.precompile.Page;
import io.easybest.mybatis.mapping.precompile.Parameter;
import io.easybest.mybatis.mapping.precompile.SQL;
import io.easybest.mybatis.mapping.precompile.Segment;
import io.easybest.mybatis.mapping.precompile.Select;
import io.easybest.mybatis.mapping.precompile.SqlDefinition;
import io.easybest.mybatis.mapping.precompile.Update;
import io.easybest.mybatis.repository.query.MybatisParameters.MybatisParameter;
import io.easybest.mybatis.repository.query.StringQuery.InParameterBinding;
import io.easybest.mybatis.repository.query.StringQuery.LikeParameterBinding;
import io.easybest.mybatis.repository.query.StringQuery.ParameterBinding;
import io.easybest.mybatis.repository.support.MybatisContext;
import io.easybest.mybatis.repository.support.ResidentStatementName;
import org.apache.ibatis.mapping.SqlCommandType;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.util.Lazy;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import static io.easybest.mybatis.repository.support.MybatisContext.PARAM_ADDITIONAL_VALUES_PREFIX;
import static io.easybest.mybatis.repository.support.ResidentParameterName.POSITION_PREFIX;

/**
 * .
 *
 * @author Jarvis Song
 */
abstract class AbstractStringBasedMybatisQuery extends AbstractMybatisQuery {

	private static final Pattern SELECT_ALL_FROM = Pattern.compile("^\\s*select\\s+\\*\\s+from\\s+.*",
			Pattern.CASE_INSENSITIVE);

	private final DeclaredQuery query;

	private final DeclaredQuery countQuery;

	private final QueryMethodEvaluationContextProvider evaluationContextProvider;

	private final SpelExpressionParser parser;

	private final Lazy<SqlCommandType> sqlCommandType;

	private final Lazy<Map<String, Expression>> expressions;

	public AbstractStringBasedMybatisQuery(EntityManager entityManager, SpelExpressionParser parser,
			QueryMethodEvaluationContextProvider evaluationContextProvider, MybatisQueryMethod method,
			String queryString, String countQueryString) {

		super(entityManager, method);

		Assert.hasText(queryString, "Query string must not be null or empty!");
		Assert.notNull(evaluationContextProvider, "ExpressionEvaluationContextProvider must not be null!");
		Assert.notNull(parser, "Parser must not be null!");

		this.evaluationContextProvider = evaluationContextProvider;

		this.query = new ExpressionBasedStringQuery(queryString, method.getEntityInformation(), parser);

		DeclaredQuery countQuery = this.query.deriveCountQuery(countQueryString, method.getCountQueryProjection());
		this.countQuery = ExpressionBasedStringQuery.from(countQuery, method.getEntityInformation(), parser);

		this.parser = parser;

		this.sqlCommandType = Lazy.of(() -> {

			if (method.isModifyingQuery() && method.getModifyingType() != SqlCommandType.SELECT) {
				return method.getModifyingType();
			}

			return Arrays.stream(SqlCommandType.values())
					.filter(type -> queryString.toUpperCase().startsWith(type.name() + " ")).findFirst()
					.orElse(SqlCommandType.UNKNOWN);
		});

		this.expressions = Lazy.of(this::parseExpressions);
	}

	private Map<String, Expression> getExpressions() {
		return this.expressions.get();
	}

	@Override
	protected void processAdditionalParams(MybatisParametersParameterAccessor accessor, MybatisContext<?, ?> context) {

		Map<String, Expression> expressionMap = this.getExpressions();
		if (!CollectionUtils.isEmpty(expressionMap)) {
			EvaluationContext evaluationContext = this.evaluationContextProvider
					.getEvaluationContext(accessor.getParameters(), accessor.getValues());
			Map<String, Object> params = context.getAdditionalValues();
			for (String key : expressionMap.keySet()) {
				Expression expression = expressionMap.get(key);
				Object value = expression.getValue(evaluationContext);
				params.put(key, value);
			}

		}
	}

	private Map<String, Expression> parseExpressions() {

		List<ParameterBinding> parameterBindings = this.query.getParameterBindings();
		if (CollectionUtils.isEmpty(parameterBindings)) {
			return Collections.emptyMap();
		}

		Map<String, Expression> expressionMap = new HashMap<>();

		for (ParameterBinding parameterBinding : parameterBindings) {

			if (!parameterBinding.isExpression()) {
				continue;
			}
			if (null == parameterBinding.getExpression()) {
				continue;
			}

			Expression expression = this.parser.parseExpression(parameterBinding.getExpression());

			String parameterName = StringUtils.hasText(parameterBinding.getName()) ? parameterBinding.getName()
					: (POSITION_PREFIX + parameterBinding.getRequiredPosition());
			expressionMap.put(parameterName, expression);

		}
		return expressionMap;
	}

	@Override
	protected SqlDefinition doCreateSqlDefinition() {

		switch (this.getSqlCommandType()) {

		case INSERT:
			return this.insert();
		case UPDATE:
			return this.update();
		case DELETE:
			return this.delete();
		case SELECT:
			return this.select();

		}

		return null;
	}

	private Select select() {

		Segment settled = this.settle(this.query);
		String queryString = settled.toString();
		Select.SelectBuilder<?, ?> builder = Select.builder().id(this.method.getStatementName());

		if (this.method.getResultMap().isPresent() || SELECT_ALL_FROM.matcher(queryString.toLowerCase()).matches()) {
			builder.resultMap(this.method.getResultMap().orElse(ResidentStatementName.RESULT_MAP));
		}
		else {
			builder.resultType(this.method.getActualResultType());
		}

		if (QueryUtils.hasConstructorExpression(queryString)) {
			queryString = this.constructor(queryString);
		}
		else {
			// parsing JPA style, such as `select u from User u`
			queryString = this.regulate(queryString);

		}

		MybatisParameters parameters = this.method.getParameters();

		if (parameters.hasSortParameter()) {
			// TODO
		}

		SQL sql = SQL.of(queryString);

		if (this.method.isPageQuery() || this.method.isSliceQuery() || parameters.hasPageableParameter()) {
			// TODO

			builder.contents(Collections.singletonList(this.query.usesPaging() ? sql
					: Page.of(this.entityManager.getDialect(), Parameter.pageOffset(), Parameter.pageSize(), sql)));

			if (this.method.isPageQuery()) {
				Segment settledCountQuery = this.settle(this.countQuery);
				Select count = Select.builder().id(this.method.getCountStatementName()).resultType("long")
						.contents(Collections.singletonList(SQL.of(this.regulate(settledCountQuery.toString()))))
						.build();
				builder.derived(Collections.singletonList(count));
			}
		}
		else {
			builder.contents(Collections.singletonList(sql));
		}

		return builder.build();
	}

	private String regulate(String queryString) {

		String projection = QueryUtils.getProjection(queryString);
		String alias = this.getQuery().getAlias();
		if (projection.equals(alias)) {
			queryString = QueryUtils.replaceProjection(queryString, projection + ".*");
		}
		else if (projection.equalsIgnoreCase("count(" + alias + ")")) { // FIXME use
			// regexp
			queryString = QueryUtils
					.replaceProjection(
							queryString, "count("
									+ ((this.entity.hasIdProperty() && !this.entity.isCompositeId())
											? (alias + "."
													+ this.entity.getRequiredIdProperty().getColumnName().getReference(
															this.entityManager.getDialect().getIdentifierProcessing()))
											: "*")
									+ ")");
		}
		return queryString;
	}

	private String constructor(String queryString) {

		String projection = QueryUtils.getProjection(queryString);
		String className = projection.substring(3, projection.lastIndexOf('(')).trim();
		Class<?> clz;
		try {
			clz = ClassUtils.forName(className, this.getClass().getClassLoader());
		}
		catch (ClassNotFoundException ex) {
			throw new MappingException(
					"Could not find class " + className + " form constructor expression: " + queryString);
		}

		String columns = projection.substring(projection.lastIndexOf('(') + 1, projection.length() - 1);

		// TODO validate columns

		queryString = queryString.replace(projection, columns);

		return queryString;
	}

	private Segment settle(DeclaredQuery query) {
		String queryString = query.getQueryString();

		List<ParameterBinding> parameterBindings = query.getParameterBindings();
		if (CollectionUtils.isEmpty(parameterBindings)) {
			return SQL.of(queryString);
		}

		for (ParameterBinding parameterBinding : parameterBindings) {

			String toReplaceParameter;
			String replacedParameter;

			MybatisParameter bindableParameter = null;

			if (parameterBinding.isExpression()) {

				String parameterName = StringUtils.hasText(parameterBinding.getName())
						? parameterBinding.getRequiredName()
						: (POSITION_PREFIX + parameterBinding.getRequiredPosition());

				toReplaceParameter = StringUtils.hasText(parameterBinding.getName())
						? (':' + parameterBinding.getRequiredName()) : ("?" + parameterBinding.getRequiredPosition());
				replacedParameter = parameterName;

			}
			else if (StringUtils.hasText(parameterBinding.getName())) {

				String parameterName = parameterBinding.getRequiredName();

				toReplaceParameter = ':' + parameterName;
				replacedParameter = parameterName;

				Optional<MybatisParameter> mybatisParameter = this.method.getParameters().stream()
						.filter(mp -> parameterName.equals(mp.getName().orElse(null))).findFirst();
				bindableParameter = mybatisParameter
						.orElseThrow(() -> new MappingException("The binding parameter [" + parameterBinding
								+ "] was not found in any method parameters.\n" + this.method + "\n" + this.query));
			}
			else if (null != parameterBinding.getPosition()) {

				toReplaceParameter = "?" + parameterBinding.getRequiredPosition();
				bindableParameter = this.method.getParameters()
						.getBindableParameter(parameterBinding.getRequiredPosition() - 1);

				if (parameterBinding.isExpression()) {
					replacedParameter = POSITION_PREFIX + parameterBinding.getRequiredPosition();
				}
				else {
					replacedParameter = bindableParameter.getName()
							.orElse(POSITION_PREFIX + parameterBinding.getRequiredPosition());
				}
			}
			else {
				throw new MappingException("No bind name or position found!");
			}

			if (!StringUtils.hasText(toReplaceParameter)) {
				continue;
			}

			if (parameterBinding instanceof InParameterBinding) {

				String paramName = PARAM_ADDITIONAL_VALUES_PREFIX + replacedParameter;

				queryString = queryString
						.replace(toReplaceParameter, Choose
								.of(MethodInvocation.of(Syntax.class, "isEmpty", paramName).toString(),
										SQL.of("(NULL)"),
										Foreach.builder().collection(paramName)
												.contents(Collections.singletonList(Parameter.of("item"))).build())
								.toString());
				continue;
			}

			Parameter.ParameterBuilder<?, ?> parameterBuilder = Parameter.builder()
					.property(PARAM_ADDITIONAL_VALUES_PREFIX + replacedParameter);
			if (null != bindableParameter) {
				parameterBuilder.javaType(bindableParameter.getType().getName())
						.jdbcType(bindableParameter.getJdbcType()).typeHandler(bindableParameter.getTypeHandler());
			}
			Parameter parameter = parameterBuilder.build();

			if (parameterBinding instanceof LikeParameterBinding) {
				Dialect dialect = this.entityManager.getDialect();

				Segment like = parameter;

				switch (((LikeParameterBinding) parameterBinding).getType()) {
				case LIKE:
					break;
				case CONTAINING:
					like = Function.of(dialect.getFunction("concat"), "'%'", parameter.toString(), "'%'");
					break;
				case STARTING_WITH:
					like = Function.of(dialect.getFunction("concat"), parameter.toString(), "'%'");
					break;
				case ENDING_WITH:
					like = Function.of(dialect.getFunction("concat"), "'%'", parameter.toString());
					break;
				}

				queryString = queryString.replace(toReplaceParameter, like.toString());
				continue;
			}

			if (null != replacedParameter) {
				queryString = queryString.replace(toReplaceParameter, parameter.toString());
			}
		}

		return SQL.of(queryString);

	}

	private Delete delete() {

		// TODO logic delete?

		return Delete.builder().id(this.method.getStatementName())
				.contents(Collections.singletonList(this.settle(this.query))).build();
	}

	private Update update() {

		return Update.builder().id(this.method.getStatementName())
				.contents(Collections.singletonList(this.settle(this.query))).build();
	}

	private Insert insert() {

		return Insert.builder().id(this.method.getStatementName())
				.contents(Collections.singletonList(this.settle(this.query))).build();
	}

	public DeclaredQuery getQuery() {
		return this.query;
	}

	public DeclaredQuery getCountQuery() {
		return this.countQuery;
	}

	public SqlCommandType getSqlCommandType() {
		return this.sqlCommandType.get();
	}

}
