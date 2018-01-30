package org.springframework.data.mybatis.repository.query;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.mybatis.mapping.MyBatisMappingContext;
import org.springframework.data.mybatis.mapping.MyBatisPersistentEntity;
import org.springframework.data.mybatis.mapping.MyBatisPersistentProperty;
import org.springframework.data.mybatis.repository.query.MyBatisQueryExecution.DeleteExecution;
import org.springframework.data.mybatis.repository.support.MyBatisMapperBuilderAssistant;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.Part.IgnoreCaseType;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.data.repository.query.parser.PartTree;

import java.util.Map;
import java.util.regex.Pattern;

import static org.apache.ibatis.mapping.SqlCommandType.*;
import static org.springframework.data.repository.query.parser.Part.IgnoreCaseType.*;

/**
 * @author Jarvis Song
 */
@Slf4j
public class PartTreeMyBatisQuery extends AbstractMyBatisQuery {

	private static final Pattern XML_PATTERN = Pattern.compile("<[^>]+>");

	private final PartTree tree;
	private final MyBatisParameters parameters;
	private final Class<?> domainClass;

	private final MyBatisPersistentEntity<?> persistentEntity;

	public PartTreeMyBatisQuery(MyBatisQueryMethod method, MyBatisMapperBuilderAssistant mapperSupport,
			MyBatisMappingContext context, SqlSessionTemplate template, Dialect dialect) {

		super(method, mapperSupport, context, template, dialect);

		this.domainClass = method.getEntityInformation().getJavaType();
		this.parameters = method.getParameters();
		this.persistentEntity = context.getPersistentEntity(this.domainClass);

		boolean recreationRequired = parameters.hasDynamicProjection() || parameters.potentiallySortsDynamically();

		try {

			this.tree = new PartTree(method.getName(), domainClass);

		} catch (Exception e) {
			throw new IllegalArgumentException(
					String.format("Failed to create query for method %s! %s", method, e.getMessage()), e);
		}

		prepare();
	}

	protected void prepare() {

		if (tree.isDelete()) {

		} else if (tree.isCountProjection()) {

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
		if (includeCount) {
			doPrepareCountQueryStatement("count_" + getStatementName());
		}

	}

	private void doPrepareCountQueryStatement(String statementName) {
		String sql = new SQL(persistentEntity, dialect, context) {
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

		assistant.addStatement(statementName, sqls, SELECT, Map.class, null, long.class, NoKeyGenerator.INSTANCE, null,
				null);
	}

	private void doPrepareSelectQueryStatement(String statementName, boolean pageable) {

		String sql = new SQL(persistentEntity, dialect, context) {
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
			sql += assistant.SORT_SQL(method.isComplexQuery());
		}
		if (pageable) {
			sql = dialect.getLimitHandler().processSql(sql, null);
		}

		String[] sqls;
		if (XML_PATTERN.matcher(sql).find()) {
			sqls = new String[] { "<script>", sql, "</script>" };
		} else {
			sqls = new String[] { sql.replace("<![CDATA[", "").replace("]]>", "") };
		}
		assistant.addStatement(statementName, sqls, SELECT, Map.class, null, domainClass, NoKeyGenerator.INSTANCE, null,
				null);
	}

	class SQL extends MyBatisMapperBuilderAssistant.SQL {

		public SQL(MyBatisPersistentEntity<?> persistentEntity, Dialect dialect, MyBatisMappingContext mappingContext) {
			super(persistentEntity, dialect, mappingContext);
		}

		@Override
		public SQL getSelf() {
			return this;
		}

		SQL QUERY_CONDITION() {
			int c = 0;
			int count = 0;

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
						columnName = dialect.quote(persistentEntity.getEntityName()) + "." + assistant.column(property);
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

					WHERE(columnName + operate + queryConditionRight(part, properties));
				}
			}

			return getSelf();
		}

		private String resolveParameterName(int position) {
			if (parameters.hasParameterAt(position)) {
				return parameters.getParameter(position).getName().orElse("p" + position);
			}
			return "p" + position;
		}

		/**
		 * <table class="tableblock frame-all grid-all spread">
		 * <col style="width: 25%;"> <col style="width: 75%;"> </colgroup> <thead>
		 * <tr>
		 * <th class="tableblock halign-left valign-top">Logical keyword</th>
		 * <th class="tableblock halign-left valign-top">Keyword expressions</th>
		 * </tr>
		 * </thead> <tbody>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>AND</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>And</code>
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>OR</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>Or</code>
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>AFTER</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>After</code>, <code>IsAfter</code>
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>BEFORE</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>Before</code>, <code>IsBefore</code>
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>CONTAINING</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>Containing</code>, <code>IsContaining</code>, <code>Contains</code>
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>BETWEEN</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>Between</code>, <code>IsBetween</code>
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>ENDING_WITH</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>EndingWith</code>, <code>IsEndingWith</code>, <code>EndsWith</code>
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>EXISTS</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>Exists</code>
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>FALSE</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>False</code>, <code>IsFalse</code>
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>GREATER_THAN</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>GreaterThan</code>, <code>IsGreaterThan</code>
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>GREATER_THAN_EQUALS</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>GreaterThanEqual</code>, <code>IsGreaterThanEqual</code>
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>IN</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>In</code>, <code>IsIn</code>
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>IS</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>Is</code>, <code>Equals</code>, (or no keyword)
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>IS_EMPTY</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>IsEmpty</code>, <code>Empty</code>
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>IS_NOT_EMPTY</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>IsNotEmpty</code>, <code>NotEmpty</code>
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>IS_NOT_NULL</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>NotNull</code>, <code>IsNotNull</code>
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>IS_NULL</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>Null</code>, <code>IsNull</code>
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>LESS_THAN</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>LessThan</code>, <code>IsLessThan</code>
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>LESS_THAN_EQUAL</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>LessThanEqual</code>, <code>IsLessThanEqual</code>
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>LIKE</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>Like</code>, <code>IsLike</code>
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>NEAR</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>Near</code>, <code>IsNear</code>
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>NOT</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>Not</code>, <code>IsNot</code>
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>NOT_IN</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>NotIn</code>, <code>IsNotIn</code>
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>NOT_LIKE</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>NotLike</code>, <code>IsNotLike</code>
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>REGEX</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>Regex</code>, <code>MatchesRegex</code>, <code>Matches</code>
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>STARTING_WITH</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>StartingWith</code>, <code>IsStartingWith</code>, <code>StartsWith</code>
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>TRUE</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>True</code>, <code>IsTrue</code>
		 * </p>
		 * </td>
		 * </tr>
		 * <tr>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>WITHIN</code>
		 * </p>
		 * </td>
		 * <td class="tableblock halign-left valign-top">
		 * <p class="tableblock">
		 * <code>Within</code>, <code>IsWithin</code>
		 * </p>
		 * </td>
		 * </tr>
		 * </tbody>
		 * </table>
		 * 
		 * @param type
		 * @return
		 */
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

	private String getStatementName() {
		return method.getStatementName();
	}

	@Override
	protected MyBatisQueryExecution getExecution() {
		if (tree.isDelete()) {
			return new DeleteExecution();
		}
		return super.getExecution();
	}

}
