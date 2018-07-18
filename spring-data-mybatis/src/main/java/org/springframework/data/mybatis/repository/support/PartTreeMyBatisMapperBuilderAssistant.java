package org.springframework.data.mybatis.repository.support;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.mybatis.dialect.RowSelection;
import org.springframework.data.mybatis.mapping.Association;
import org.springframework.data.mybatis.mapping.Column;
import org.springframework.data.mybatis.mapping.ElementCollectionAssociation;
import org.springframework.data.mybatis.mapping.MyBatisMappingContext;
import org.springframework.data.mybatis.mapping.MyBatisPersistentProperty;
import org.springframework.data.mybatis.mapping.Table;
import org.springframework.data.mybatis.mapping.ToManyAssociation;
import org.springframework.data.mybatis.mapping.ToOneAssociation;
import org.springframework.data.mybatis.repository.query.MyBatisParameters;
import org.springframework.data.mybatis.repository.query.MyBatisQueryMethod;
import org.springframework.data.mybatis.repository.query.PartTreeMyBatisQuery;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.Part.IgnoreCaseType;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.util.StringUtils;

import javax.persistence.Lob;
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

	public PartTreeMyBatisMapperBuilderAssistant(SqlSessionTemplate sqlSessionTemplate,
			MyBatisMappingContext mappingContext, Dialect dialect, PartTreeMyBatisQuery repositoryQuery) {

		super(sqlSessionTemplate, mappingContext, dialect, repositoryQuery.getQueryMethod().getNamespace(),
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

			doPreparePageQueryStatement(true);

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

	/**
	 * <code>
	 *     User {id, name, age, homeAddress, role}
	 *     Role {id, name}
	 *
	 *     simple:
	 *     select
	 *     	id as `id`, name as `name`, age as `age`, home_address as `homeAddress`, role_id as `role.id`,
	 *     	country as `address.country`, street_number as `adress.streetNo`
	 *     from `uc_user`
	 *     where age = 28 and role_id = 10001 and name in ('Apple','Orange')
	 *
	 *	   complex:
	 *     select
	 *     	`user`.id as `id`,
	 *     	`user`.name as `name`,
	 *     	`user`.age as `age`,
	 *     	`user`.home_address as `homeAddress`,
	 *     	`user`.role_id as `role.id`,
	 *     	`user.role`.name as `role.name`
	 *     from uc_user `user`
	 *     left outer join ds_user_ds_role `roles_ds_user_ds_role` on `user`.id = `roles_ds_user_ds_role`.user_id
	 *     left outer join ds_role `user.roles` on `roles_ds_user_ds_role`.role_id = `user.roles`.id
	 *     where
	 *     	`user`.age = 28 and `user.role`.name = 'guest' and `user`.name in ('Apple','Orange')
	 *
	 *      delete:
	 *     	delete from `uc_user` where `age` = 19 and name like 'Poul%'
	 *
	 *     	insert:
	 *     	insert into op_user(id,name,age,home_address,role_id) values(#{id,jdbcType=long},#{name},#{homeAddress},#{role.id})
	 *
	 *     	update:
	 *     	update op_user set name = #{name}, age = #{age,jdbcType=int}, home_address = 'No. 9, Building A', role_id = 10002
	 *     	where id = 10003
	 *
	 * </code>
	 */
	private String buildQueryCondition(boolean complex) {
		StringBuilder builder = new StringBuilder();

		int c = 0, count = 0;
		MyBatisParameters parameters = method.getParameters();
		for (PartTree.OrPart orPart : tree) {
			if (count++ > 0) {
				builder.append(" or ");
			}

			for (Part part : orPart) {
				MyBatisPersistentProperty property = persistentEntity.getPersistentProperty(part.getProperty().getSegment());
				if (null == property) {
					throw new MappingException("can not find property: " + part.getProperty().getSegment() + " in entity: "
							+ persistentEntity.getName());
				}
				String columnName = null;
				if (property.isAnnotationPresent(Lob.class)) {
					columnName = property.getMappedColumn().getQueryColumnName(complex, dialect);
				} else if (property.isAssociation()) {
					String targetProperty = part.getProperty().getLeafProperty().getSegment();
					Association association = property.getMappedAssociation();
					if (null != association && association instanceof ToOneAssociation) {
						Column column = ((ToOneAssociation) association).getInverseJoinTable().findColumn(targetProperty);
						columnName = ((ToOneAssociation) association).getInverseJoinTable().getQuotedAlias(dialect) + '.'
								+ (null != column ? column.getActualName(dialect)
										: ((ToOneAssociation) association).getInverseJoinTable().getIdColumn().getActualName(dialect));

					} else if (null != association && association instanceof ElementCollectionAssociation) {
						columnName = ((ElementCollectionAssociation) association).getMappingColumn().getQuotedPrefix(dialect) + '.'
								+ ((ElementCollectionAssociation) association).getMappingColumn().getActualName(dialect);
					} else if (null != association && association instanceof ToManyAssociation) {
						Column column = ((ToManyAssociation) association).getInverseJoinTable().findColumn(targetProperty);
						columnName = ((ToManyAssociation) association).getInverseJoinTable().getQuotedAlias(dialect) + '.'
								+ (null != column ? column.getActualName(dialect)
										: ((ToManyAssociation) association).getInverseJoinTable().getIdColumn().getActualName(dialect));

					} else {
						columnName = "";
					}
				} else {
					columnName = property.getMappedColumn().getQueryColumnName(complex, dialect);
				}
				if (null == columnName) {
					throw new MappingException(
							"can not find property: " + part.getProperty().getSegment() + " in " + method.getName());
				}

				IgnoreCaseType ignoreCaseType = part.shouldIgnoreCase();
				if (ignoreCaseType == ALWAYS || ignoreCaseType == WHEN_POSSIBLE) {
					columnName = dialect.getLowercaseFunction() + '(' + columnName + ')';
				}
				String operation = buildOperation(part.getType());
				String[] properties = new String[part.getType().getNumberOfArguments()];
				for (int i = 0; i < properties.length; i++) {
					properties[i] = this.resolveParameterName(c++);
				}

				if (part.getType() == IN || part.getType() == NOT_IN) {
					StringBuilder inBuilder = new StringBuilder();
					Class<?> typeClass = parameters.getParameter(c - properties.length).getType();
					String judgeEmpty = null;
					if (typeClass.isArray()) {
						judgeEmpty = "length == 0";
					} else if (Collection.class.isAssignableFrom(typeClass)) {
						judgeEmpty = "isEmpty()";
					}
					inBuilder.append("<choose><when test=\"").append(properties[0]).append(" == null");
					if (null != judgeEmpty) {
						inBuilder.append(" || ").append(properties[0]).append('.').append(judgeEmpty);
					}
					inBuilder.append("\">");
					if (part.getType() == IN) {
						inBuilder.append(" 1 = 0 ");
					} else {
						inBuilder.append(" 1 = 1");
					}
					inBuilder.append("</when><otherwise>");
					inBuilder.append(columnName + operation + queryConditionRight(part, properties));
					inBuilder.append("</otherwise></choose>");
					builder.append(inBuilder);
				} else {
					builder.append(columnName).append(operation).append(queryConditionRight(part, properties));
				}

			}

		}

		return builder.toString().trim();
	}

	String queryConditionRight(Part part, String[] properties) {
		StringBuilder builder = new StringBuilder();
		IgnoreCaseType ignoreCaseType = part.shouldIgnoreCase();
		switch (part.getType()) {
			case CONTAINING:
			case NOT_CONTAINING:
				String bind = "__bind_" + properties[0];
				builder.append("<bind name=\"").append(bind).append("\" value=\"'%' + " + properties[0] + " + '%'\" />");
				if (ignoreCaseType == ALWAYS || ignoreCaseType == WHEN_POSSIBLE) {
					builder.append(dialect.getLowercaseFunction()).append("(#{").append(bind).append("})");
				} else {
					builder.append("#{").append(bind).append("}");
				}
				return builder.toString();
			case STARTING_WITH:
				bind = "__bind_" + properties[0];
				builder.append("<bind name=\"").append(bind).append("\" value=\"" + properties[0] + " + '%'\" />");
				if (ignoreCaseType == ALWAYS || ignoreCaseType == WHEN_POSSIBLE) {
					builder.append(dialect.getLowercaseFunction()).append("(#{").append(bind).append("})");
				} else {
					builder.append("#{").append(bind).append("}");
				}
				return builder.toString();
			case ENDING_WITH:
				bind = "__bind_" + properties[0];
				builder.append("<bind name=\"").append(bind).append("\" value=\"'%' + " + properties[0] + "\" />");
				if (ignoreCaseType == ALWAYS || ignoreCaseType == WHEN_POSSIBLE) {
					builder.append(dialect.getLowercaseFunction()).append("(#{").append(bind).append("})");
				} else {
					builder.append("#{").append(bind).append("}");
				}
				return builder.toString();
			case IN:
			case NOT_IN:
				builder.append("<foreach item=\"item\" index=\"index\" collection=\"").append(properties[0])
						.append("\" open=\"(\" separator=\",\" close=\")\">#{item}</foreach>");
				return builder.toString();
			case IS_NOT_NULL:
				return " is not null";
			case IS_NULL:
				return " is null";
			case TRUE:
				return " = true";
			case FALSE:
				return " = false";
			default:
				if (ignoreCaseType == ALWAYS || ignoreCaseType == WHEN_POSSIBLE) {
					builder.append(dialect.getLowercaseFunction()).append("(#{").append(properties[0]).append("})");
				} else {
					builder.append("#{").append(properties[0]).append("}");
				}
				return builder.toString();
		}
	}

	private String resolveParameterName(int position) {
		MyBatisParameters parameters = method.getParameters();
		if (parameters.hasParameterAt(position)) {
			return parameters.getParameter(position).getName().orElse("p" + position);
		}
		return "p" + position;
	}

	private String buildOperation(Type type) {
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

	private void doPrepareCountQueryStatement(String statementName) {
		StringBuilder builder = new StringBuilder();
		StringBuilder limit = new StringBuilder();
		if (tree.isLimiting()) {
			limit.append("select count(*) from (");
		}

		builder.append("select ");
		if (tree.isLimiting()) {
			builder.append("1");
		} else {
			builder.append("count(*)");
		}

		builder.append(" from ").append(buildStandardFrom(this.complex));

		String condition = buildQueryCondition(this.complex);
		if (StringUtils.hasText(condition)) {
			builder.append(" where ").append(condition);
		}

		String sql = builder.toString();

		if (tree.isLimiting()) {

			RowSelection rowSelection = new RowSelection();
			rowSelection.setMaxRows(tree.getMaxResults());
			sql = limit.toString() + dialect.getLimitHandler().processSql(sql, rowSelection) + ") __a";
		}

		String[] sqls;
		if (XML_PATTERN.matcher(sql).find()) {
			sqls = new String[] { "<script>", sql, "</script>" };
		} else {
			sqls = new String[] { sql.replace("<![CDATA[", "").replace("]]>", "") };
		}

		addMappedStatement(statementName, sqls, SELECT, Map.class, long.class);
	}

	private void doPrepareSelectQueryStatement(String statementName, boolean pageable) {
		StringBuilder builder = new StringBuilder();
		builder.append("select ");
		if (tree.isDistinct()) {
			builder.append("distinct ");
		}

		builder.append(buildStandardSelectColumns(complex));
		builder.append(" from ").append(buildStandardFrom(complex));
		String condition = buildQueryCondition(this.complex);
		if (StringUtils.hasText(condition)) {
			builder.append(" where ").append(condition);
		}

		builder.append(buildStandardOrderBy(complex, tree.getSort()));
		builder.append(buildStandardOrderBy(complex));
		String sql = builder.toString();
		/*
		 * In order to return the correct results, we have to adjust the first result offset to be returned if:
		 * - a Pageable parameter is present
		 * - AND the requested page number > 0
		 * - AND the requested page size was bigger than the derived result limitation via the First/Top keyword.
		 */
		RowSelection rowSelection = null;
		if (tree.isLimiting()) {
			if (!pageable) {
				rowSelection = new RowSelection();
				rowSelection.setMaxRows(tree.getMaxResults());
			}
			method.setLimitSize(tree.getMaxResults());
			pageable = true;
		}

		if (pageable) {
			sql = dialect.getLimitHandler().processSql(sql, rowSelection);
		}
		addMappedStatement(statementName, new String[] { "<script>", sql, "</script>" }, SELECT, Map.class, domainClass);
	}

	private void doPrepareDeleteStatement() {
		Table table = persistentEntity.getTable();
		StringBuilder builder = new StringBuilder();
		builder.append("delete from ").append(table.getFullName(dialect));

		String condition = buildQueryCondition(false);
		if (StringUtils.hasText(condition)) {
			builder.append(" where ").append(condition);
		}

		String sql = builder.toString();
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

}
