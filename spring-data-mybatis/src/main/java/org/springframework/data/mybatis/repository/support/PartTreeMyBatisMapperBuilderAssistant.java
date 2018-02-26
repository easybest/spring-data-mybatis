package org.springframework.data.mybatis.repository.support;

import org.apache.ibatis.session.Configuration;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mybatis.mapping.MyBatisMappingContext;
import org.springframework.data.mybatis.mapping.MyBatisPersistentProperty;
import org.springframework.data.mybatis.repository.query.MyBatisParameters;
import org.springframework.data.mybatis.repository.query.MyBatisQueryMethod;
import org.springframework.data.mybatis.repository.query.PartTreeMyBatisQuery;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.Part.IgnoreCaseType;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.data.repository.query.parser.PartTree;

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

	private PartTree tree;
	private MyBatisQueryMethod method;
	private boolean complex;

	public PartTreeMyBatisMapperBuilderAssistant(Configuration configuration, MyBatisMappingContext mappingContext,
			PartTreeMyBatisQuery repositoryQuery) {

		super(configuration, mappingContext, repositoryQuery.getQueryMethod().getNamespace(),
				repositoryQuery.getQueryMethod().getEntityInformation().getJavaType());

		this.tree = repositoryQuery.getTree();
		this.method = repositoryQuery.getQueryMethod();
		this.complex = this.method.isComplexQuery();
	}

	@Override
	protected void doPrepare() {

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
		tree = null;
		method = null;
	}

	private void doPrepareCollectionQueryStatement() {

		Class<?> returnedObjectType = method.getReturnedObjectType();

		if (returnedObjectType != domainClass && !returnedObjectType.isAssignableFrom(domainClass)) {
			throw new IllegalArgumentException("return object type must be or assignable from " + domainClass);
		}

		doPrepareSelectQueryStatement(getStatementName(), false);
	}

	private void doPreparePageQueryStatement(boolean includeCount) {

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

		String sql = new SQL() {
			{
				SELECT("count(*)");
				FROM_WITH_LEFT_OUTER_JOIN(complex);
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
		SQL ql = new SQL() {
			{
				if (tree.isDistinct()) {
					SELECT_DISTINCT(COLUMNS(complex));
				} else {
					SELECT_WITH_COLUMNS(complex);
				}
				FROM_WITH_LEFT_OUTER_JOIN(complex);
				QUERY_CONDITION();
				ORDER_BY(complex, tree.getSort());

			}
		};
		String sql = ql.toString();

		sql += ql.SORT_SQL(this.complex);
		if (pageable) {
			sql = dialect.getLimitHandler().processSql(sql, null);
		}
		String[] sqls = new String[] { "<script>", sql, "</script>" };
		addMappedStatement(statementName, sqls, SELECT, Map.class, domainClass);
	}

	private void doPrepareDeleteStatement() {

		String sql = new SQL() {
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
		return method.getStatementName();
	}

	class SQL extends AbstractMyBatisMapperBuilderAssistant.SQL {

		@Override
		public SQL getSelf() {
			return this;
		}

		SQL QUERY_CONDITION() {
			int c = 0;
			int count = 0;
			MyBatisParameters parameters = method.getParameters();
			for (PartTree.OrPart orPart : tree) {
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
						columnName = dialect.quote(persistentEntity.getEntityName()) + "." + COLUMN(property);
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
			MyBatisParameters parameters = method.getParameters();
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
