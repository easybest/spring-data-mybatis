package org.springframework.data.mybatis.repository.support;

import org.apache.ibatis.session.Configuration;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.mybatis.mapping.MyBatisMappingContext;
import org.springframework.data.mybatis.mapping.MyBatisPersistentEntity;
import org.springframework.data.mybatis.mapping.MyBatisPersistentProperty;
import org.springframework.data.mybatis.repository.query.MyBatisParameters;
import org.springframework.data.mybatis.repository.query.MyBatisQueryMethod;
import org.springframework.data.mybatis.repository.query.PartTreeMyBatisQuery;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.Part.IgnoreCaseType;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

import static org.apache.ibatis.mapping.SqlCommandType.*;
import static org.springframework.data.repository.query.parser.Part.IgnoreCaseType.*;
import static org.springframework.data.repository.query.parser.Part.Type.*;

/**
 * MyBatis Mapper Builder Assistant for Part Tree Query.
 * 
 * @author Jarvis Song
 */
class PartTreeMyBatisMapperBuilderAssistant extends AbstractMyBatisMapperBuilderAssistant {

	private static final Pattern XML_PATTERN = Pattern.compile("<[^>]+>");

	private PartTreeMyBatisQuery query;

	public PartTreeMyBatisMapperBuilderAssistant(Configuration configuration, MyBatisMappingContext mappingContext,
			PartTreeMyBatisQuery repositoryQuery) {

		super(configuration, mappingContext, repositoryQuery.getQueryMethod().getNamespace(),
				repositoryQuery.getQueryMethod().getEntityInformation().getJavaType());

		this.query = repositoryQuery;
	}

	@Override
	protected void doPrepare() {

		PartTree tree = query.getTree();
		MyBatisQueryMethod method = query.getQueryMethod();

		if (tree.isDelete()) {

			doPrepareDeleteStatement();
		} else if (tree.isCountProjection()) {

			doPrepareCountQueryStatement(getStatementName());

		} else if (tree.isExistsProjection()) {

			doPrepareCountQueryStatement(getStatementName());

		} else if (method.isPageQuery()) {

			doPreparePageQueryStatement(true);

		} else if (method.isSliceQuery()) {

			doPreparePageQueryStatement(false);

		} else if (method.isCollectionQuery()) {

			doPrepareCollectionQueryStatement();

		} else if (method.isStreamQuery()) {

		} else if (method.isQueryForEntity()) {

			doPrepareSelectQueryStatement(getStatementName(), false);
		}

		// last clear
		query = null;
	}

	private void doPrepareCollectionQueryStatement() {

		MyBatisQueryMethod method = query.getQueryMethod();
		Class<?> returnedObjectType = method.getReturnedObjectType();

		if (returnedObjectType != domainClass && !returnedObjectType.isAssignableFrom(domainClass)) {
			throw new IllegalArgumentException("return object type must be or assignable from " + domainClass);
		}

		doPrepareSelectQueryStatement(getStatementName(), false);
	}

	private void doPreparePageQueryStatement(boolean includeCount) {

		MyBatisQueryMethod method = query.getQueryMethod();
		Class<?> returnedObjectType = method.getReturnedObjectType();

		if (returnedObjectType != domainClass && !returnedObjectType.isAssignableFrom(domainClass)) {
			throw new IllegalArgumentException("return object type must be or assignable from " + domainClass);
		}
		doPrepareSelectQueryStatement(getStatementName(), true);
		doPrepareSelectQueryStatement("unpaged_" + getStatementName(), false);
		if (includeCount) {
			doPrepareCountQueryStatement("count_" + getStatementName());
		}

	}

	private void doPrepareCountQueryStatement(String statementName) {

		MyBatisQueryMethod method = query.getQueryMethod();
		String sql = new SQL(persistentEntity, dialect, mappingContext) {
			{
				SELECT("count(*)");
				FROM_WITH_LEFT_OUTER_JOIN(method.isComplexQuery());
				QUERY_CONDITION();
			}
		}.toString();
		String[] sqls;
		if (XML_PATTERN.matcher(sql).find()) {
			sqls = new String[] { "<script>", sql, "</script>" };
		} else {
			sqls = new String[] { sql.replace("<![CDATA[", "").replace("]]>", "") };
		}

		addMappedStatement(statementName, sqls, SELECT, Map.class, long.class);
	}

	private void doPrepareSelectQueryStatement(String statementName, boolean pageable) {

		PartTree tree = query.getTree();
		MyBatisQueryMethod method = query.getQueryMethod();

		String sql = new SQL(persistentEntity, dialect, mappingContext) {
			{
				if (tree.isDistinct()) {
					SELECT_DISTINCT(COLUMNS(method.isComplexQuery()));
				} else {
					SELECT_WITH_COLUMNS(method.isComplexQuery());
				}
				FROM_WITH_LEFT_OUTER_JOIN(method.isComplexQuery());
				QUERY_CONDITION();
				ORDER_BY(method.isComplexQuery(), tree.getSort());

			}
		}.toString();
		if (pageable) {
			sql += SORT_SQL(method.isComplexQuery());
		}
		if (pageable) {
			sql = dialect.getLimitHandler().processSql(sql, null);
		}
		sql += SORT_SQL(method.isComplexQuery());
		String[] sqls = new String[] { "<script>", sql, "</script>" };
		addMappedStatement(statementName, sqls, SELECT, Map.class, domainClass);
	}

	private void doPrepareDeleteStatement() {
		MyBatisQueryMethod method = query.getQueryMethod();

		String sql = new SQL(persistentEntity, dialect, mappingContext) {
			{
				DELETE_FROM(persistentEntity.getTableName() + " " + dialect.quote(persistentEntity.getEntityName()));
				QUERY_CONDITION();

			}
		}.toString();
		String[] sqls;
		if (XML_PATTERN.matcher(sql).find()) {
			sqls = new String[] { "<script>", sql, "</script>" };
		} else {
			sqls = new String[] { sql.replace("<![CDATA[", "").replace("]]>", "") };
		}
		addMappedStatement(method.getStatementName(), sqls, DELETE, Map.class, long.class);

		if (method.isCollectionQuery()) {
			doPrepareSelectQueryStatement("query_" + method.getStatementName(), false);
		}

	}

	private String getStatementName() {
		return query.getQueryMethod().getStatementName();
	}

	public String SORT_SQL(boolean complex) {
		final StringBuilder builder = new StringBuilder();

		builder.append("<if test=\"_sorts != null\">");
		builder.append("<bind name=\"_columnsMap\" value='#{");
		String[] arr = new SQL(persistentEntity, dialect, mappingContext).COLUMNS(complex);
		Arrays.stream(arr).filter(c -> StringUtils.hasText(c)).forEach(s -> {
			String[] ss = s.split(" as ");
			String key = ss[ss.length - 1];
			String val = ss[0];
			key = key.replace(String.valueOf(dialect.openQuote()), "").replace(String.valueOf(dialect.closeQuote()), "");
			val = val.replace("\"", "\\\"");
			builder.append(String.format("\"%s\":\"%s\",", key, val));
		});

		if (builder.length() > 0 && builder.charAt(builder.length() - 1) == ',') {
			builder.deleteCharAt(builder.length() - 1);
		}
		builder.append("}' />");
		builder.append(" order by ");
		builder.append("<foreach item=\"item\" index=\"idx\" collection=\"_sorts\" open=\"\" separator=\",\" close=\"\">");
		builder.append("<if test=\"item.ignoreCase\">" + dialect.getLowercaseFunction() + "(</if>")
				.append("${_columnsMap[item.property]}").append("<if test=\"item.ignoreCase\">)</if>")
				.append(" ${item.direction}");
		builder.append("</foreach>");
		builder.append("</if>");

		return builder.toString();
	}

	class SQL extends AbstractMyBatisMapperBuilderAssistant.SQL {

		public SQL(MyBatisPersistentEntity<?> persistentEntity, Dialect dialect, MyBatisMappingContext mappingContext) {
			super(persistentEntity, dialect, mappingContext);
		}

		public SQL() {}

		@Override
		public SQL getSelf() {
			return this;
		}

		SQL QUERY_CONDITION() {
			int c = 0;
			int count = 0;
			MyBatisParameters parameters = query.getQueryMethod().getParameters();
			MyBatisQueryMethod method = query.getQueryMethod();
			for (PartTree.OrPart orPart : query.getTree()) {
				if (count++ > 0) {
					OR();
				}
				for (Part part : orPart) {
					MyBatisPersistentProperty property = persistentEntity.getPersistentProperty(part.getProperty().getSegment());
					if (null == property) {
						throw new MappingException("can not find property: " + part.getProperty().getSegment() + " in entity: "
								+ persistentEntity.getName());
					}
					String columnName = null;
					if (property.isEntity()) {
						// TODO
						columnName = "";
					} else {
						// just a simple property direct mapping a column
						columnName = dialect.quote(persistentEntity.getEntityName()) + "." + column(property);
					}

					if (null == columnName) {
						throw new MappingException(
								"can not find property: " + part.getProperty().getSegment() + " in " + method.getName());
					}

					IgnoreCaseType ignoreCaseType = part.shouldIgnoreCase();
					if (ignoreCaseType == ALWAYS || ignoreCaseType == WHEN_POSSIBLE) {
						columnName = dialect.getLowercaseFunction() + "(" + columnName + ")";
					}
					String operate = OPERATE(part.getType());
					String[] properties = new String[part.getType().getNumberOfArguments()];
					for (int i = 0; i < properties.length; i++) {
						properties[i] = resolveParameterName(c++);
					}

					if (part.getType() == IN || part.getType() == NOT_IN) {
						StringBuilder builder = new StringBuilder();
						Class<?> typeClass = parameters.getParameter(c - properties.length).getType();
						String judgeEmpty = null;
						if (typeClass.isArray()) {
							judgeEmpty = "length==0";
						} else if (typeClass.isAssignableFrom(Collection.class)) {
							judgeEmpty = "isEmpty()";
						}
						builder.append("<choose>").append("<when test=\"" + properties[0] + "==null ");
						if (null != judgeEmpty) {
							builder.append("|| " + properties[0] + ".").append(judgeEmpty);
						}
						builder.append("\">");
						if (part.getType() == IN) {
							builder.append("1=0");
						} else {
							builder.append("1=1");
						}
						builder.append("</when><otherwise>");
						builder.append(columnName + operate + queryConditionRight(part, properties));
						builder.append("</otherwise></choose>");
						WHERE(builder.toString());
					} else {
						WHERE(columnName + operate + queryConditionRight(part, properties));
					}
				}
			}

			return getSelf();
		}

		private String resolveParameterName(int position) {
			MyBatisParameters parameters = query.getQueryMethod().getParameters();
			if (parameters.hasParameterAt(position)) {
				return parameters.getParameter(position).getName().orElse("p" + position);
			}
			return "p" + position;
		}

		String OPERATE(Type type) {
			switch (type) {

				case SIMPLE_PROPERTY:
					return "=";
				case NEGATING_SIMPLE_PROPERTY:
					return "<![CDATA[<>]]>";
				case LESS_THAN:
				case BEFORE:
					return "<![CDATA[<]]>";
				case LESS_THAN_EQUAL:
					return "<![CDATA[<=]]>";
				case GREATER_THAN:
				case AFTER:
					return "<![CDATA[>]]>";
				case GREATER_THAN_EQUAL:
					return ">=";
				case NOT_LIKE:
					return " not like ";
				case LIKE:
				case STARTING_WITH:
				case ENDING_WITH:
					return " like ";
				case CONTAINING:
					return " like ";
				case NOT_CONTAINING:
					return " not like ";
				case IN:
					return " in ";
				case NOT_IN:
					return " not in ";

			}
			return "";
		}

		String queryConditionRight(Part part, String[] properties) {
			IgnoreCaseType ignoreCaseType = part.shouldIgnoreCase();
			switch (part.getType()) {
				case CONTAINING:
				case NOT_CONTAINING:
					if (ignoreCaseType == ALWAYS || ignoreCaseType == WHEN_POSSIBLE) {
						return ("concat('%'," + dialect.getLowercaseFunction() + "(#{" + properties[0] + "}),'%')");
					} else {
						return ("concat('%',#{" + properties[0] + "},'%')");
					}
				case STARTING_WITH:
					if (ignoreCaseType == ALWAYS || ignoreCaseType == WHEN_POSSIBLE) {
						return ("concat(" + dialect.getLowercaseFunction() + "(#{" + properties[0] + "}),'%')");
					} else {
						return ("concat(#{" + properties[0] + "},'%')");
					}
				case ENDING_WITH:
					if (ignoreCaseType == ALWAYS || ignoreCaseType == WHEN_POSSIBLE) {
						return ("concat('%'," + dialect.getLowercaseFunction() + "(#{" + properties[0] + "}))");
					} else {
						return ("concat('%',#{" + properties[0] + "})");
					}
				case IN:
				case NOT_IN:
					return ("<foreach item=\"item\" index=\"index\" collection=\"" + properties[0]
							+ "\" open=\"(\" separator=\",\" close=\")\">#{item}</foreach>");
				case IS_NOT_NULL:
					return (" is not null");
				case IS_NULL:
					return (" is null");
				case TRUE:
					return (" = true");

				case FALSE:
					return (" = false");

				default:
					if (ignoreCaseType == ALWAYS || ignoreCaseType == WHEN_POSSIBLE) {
						return (dialect.getLowercaseFunction() + "(#{" + properties[0] + "})");
					} else {
						return ("#{" + properties[0] + "}");
					}
			}
		}
	}

}
