package org.springframework.data.mybatis.repository.support;

import static org.apache.ibatis.mapping.SqlCommandType.DELETE;
import static org.apache.ibatis.mapping.SqlCommandType.SELECT;
import static org.springframework.data.repository.query.parser.Part.Type.IN;
import static org.springframework.data.repository.query.parser.Part.Type.NOT_IN;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mybatis.dialect.RowSelection;
import org.springframework.data.mybatis.mapping.MybatisPersistentProperty;
import org.springframework.data.mybatis.repository.query.MybatisParameters;
import org.springframework.data.mybatis.repository.query.MybatisQueryMethod;
import org.springframework.data.mybatis.repository.query.PartTreeMybatisQuery;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.util.StringUtils;

import org.apache.ibatis.session.Configuration;

public class MybatisPartTreeMapperBuilder extends MybatisMapperBuildAssistant {

	private final PartTree tree;

	private final MybatisQueryMethod method;

	public MybatisPartTreeMapperBuilder(Configuration configuration,
			PersistentEntity<?, ?> persistentEntity, PartTreeMybatisQuery query) {
		super(configuration, persistentEntity, query.getQueryMethod().getNamespace());

		this.tree = query.getTree();
		this.method = query.getQueryMethod();
	}

	@Override
	protected void doBuild() {

		if (tree.isDelete()) {
			addDeleteStatement();
		}
		else if (tree.isCountProjection()) {
			addCountQueryStatement(getStatementName());
		}
		else if (tree.isExistsProjection()) {
			addCountQueryStatement(getStatementName());
		}
		else if (method.isPageQuery()) {

			addPageQueryStatement(true);

		}
		else if (method.isSliceQuery()) {

			addPageQueryStatement(true);

		}
		else if (method.isCollectionQuery()) {

			addCollectionQueryStatement();

		}
		else if (method.isStreamQuery()) {

		}
		else if (method.isQueryForEntity()) {

			addSelectQueryStatement(getStatementName(), false);
		}

	}

	@Override
	protected List<MybatisPersistentProperty> findNormalColumns() {
		if (null != method.getSelectColumns()) {

			return Stream.of(method.getSelectColumns().split(","))
					.map(c -> entity.getRequiredPersistentProperty(c))
					.collect(Collectors.toList());
		}
		return super.findNormalColumns();
	}

	private void addCollectionQueryStatement() {

		Class<?> returnedObjectType = method.getReturnedObjectType();

		if (returnedObjectType != entity.getType()
				&& !returnedObjectType.isAssignableFrom(entity.getType())) {
			throw new IllegalArgumentException(
					"return object type must be or assignable from " + entity.getType());
		}

		addSelectQueryStatement(getStatementName(), false);
	}

	private void addPageQueryStatement(boolean includeCount) {

		Class<?> returnedObjectType = method.getReturnedObjectType();

		if (returnedObjectType != entity.getType()
				&& !returnedObjectType.isAssignableFrom(entity.getType())) {
			throw new IllegalArgumentException(
					"return object type must be or assignable from " + entity.getType());
		}
		addSelectQueryStatement(getStatementName(), true);
		addSelectQueryStatement("unpaged_" + getStatementName(), false);
		if (includeCount) {
			addCountQueryStatement("count_" + getStatementName());
		}
	}

	private void addDeleteStatement() {

		StringBuilder builder = new StringBuilder();
		builder.append("delete from ").append(entity.getTableName());
		String condition = buildQueryCondition();
		if (StringUtils.hasText(condition)) {
			builder.append(" where ").append(condition);
		}
		String sql = builder.toString();
		String[] sqls;
		if (XML_PATTERN.matcher(sql).find()) {
			sqls = new String[] { "<script>", sql, "</script>" };
		}
		else {
			sqls = new String[] { sql.replace("<![CDATA[", "").replace("]]>", "") };
		}
		addMappedStatement(method.getStatementName(), sqls, DELETE, Map.class,
				long.class);

		if (method.isCollectionQuery()) {
			addSelectQueryStatement("query_" + method.getStatementName(), false);
		}

	}

	private void addCountQueryStatement(String statementName) {
		StringBuilder builder = new StringBuilder();
		StringBuilder limit = new StringBuilder();
		if (tree.isLimiting()) {
			limit.append("select count(*) from (");
		}

		builder.append("select ");
		if (tree.isLimiting()) {
			builder.append("1");
		}
		else {
			builder.append("count(*)");
		}

		builder.append(" from ").append(entity.getTableName());

		String condition = buildQueryCondition();
		if (StringUtils.hasText(condition)) {
			builder.append(" where ").append(condition);
		}

		String sql = builder.toString();

		if (tree.isLimiting()) {

			RowSelection rowSelection = new RowSelection();
			rowSelection.setMaxRows(tree.getMaxResults());
			sql = limit.toString()
					+ dialect.getLimitHandler().processSql(sql, rowSelection) + ") __a";
		}

		String[] sqls;
		if (XML_PATTERN.matcher(sql).find()) {
			sqls = new String[] { "<script>", sql, "</script>" };
		}
		else {
			sqls = new String[] { sql.replace("<![CDATA[", "").replace("]]>", "") };
		}

		addMappedStatement(statementName, sqls, SELECT, Map.class, long.class);
	}

	private void addSelectQueryStatement(String statementName, boolean pageable) {
		StringBuilder builder = new StringBuilder();
		builder.append("select ");
		if (tree.isDistinct()) {
			builder.append("distinct ");
		}

		// builder.append(findNormalColumns().stream()
		// .map(p -> String.format("%s as %s", p.getColumnName(), p.getName()))
		// .collect(Collectors.joining(",")));

		builder.append(" * ");

		builder.append(" from ").append(entity.getTableName());
		String condition = buildQueryCondition();
		if (StringUtils.hasText(condition)) {
			builder.append(" where ").append(condition);
		}

		builder.append(buildStandardOrderBy(tree.getSort()));
		builder.append(buildStandardOrderBy());
		String sql = builder.toString();
		/*
		 * In order to return the correct results, we have to adjust the first result
		 * offset to be returned if: - a Pageable parameter is present - AND the requested
		 * page number > 0 - AND the requested page size was bigger than the derived
		 * result limitation via the First/Top keyword.
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
		addMappedStatement(statementName, new String[] { "<script>", sql, "</script>" },
				SELECT, Map.class,
				null == method.getResultMap() ? RESULT_MAP : method.getResultMap());
	}

	private String buildQueryCondition() {

		StringBuilder builder = new StringBuilder();

		int c = 0, count = 0;
		MybatisParameters parameters = method.getParameters();
		for (PartTree.OrPart orPart : tree) {
			if (count++ > 0) {
				builder.append(" or ");
			}

			int andCount = 0;
			for (Part part : orPart) {
				if (andCount++ > 0) {
					builder.append(" and ");
				}
				MybatisPersistentProperty property = entity
						.getPersistentProperty(part.getProperty().getSegment());
				if (null == property) {
					throw new MappingException(
							"can not find property: " + part.getProperty().getSegment()
									+ " in entity: " + entity.getName());
				}
				String columnName = queryConditionLeft(property.getColumnName(),
						part.shouldIgnoreCase());

				String operation = calculateOperation(part.getType());
				String[] properties = new String[part.getType().getNumberOfArguments()];
				for (int i = 0; i < properties.length; i++) {
					properties[i] = this.resolveParameterName(c++);
				}

				if (part.getType() == IN || part.getType() == NOT_IN) {
					StringBuilder inBuilder = new StringBuilder();
					Class<?> typeClass = parameters.getParameter(c - properties.length)
							.getType();
					String judgeEmpty = null;
					if (typeClass.isArray()) {
						judgeEmpty = "length == 0";
					}
					else if (Collection.class.isAssignableFrom(typeClass)) {
						judgeEmpty = "isEmpty()";
					}
					inBuilder.append("<choose><when test=\"").append(properties[0])
							.append(" == null");
					if (null != judgeEmpty) {
						inBuilder.append(" || ").append(properties[0]).append('.')
								.append(judgeEmpty);
					}
					inBuilder.append("\">");
					if (part.getType() == IN) {
						inBuilder.append(" 1 = 0 ");
					}
					else {
						inBuilder.append(" 1 = 1");
					}
					inBuilder.append("</when><otherwise>");
					inBuilder.append(columnName + operation + queryConditionRight(
							part.getType(), part.shouldIgnoreCase(), properties));
					inBuilder.append("</otherwise></choose>");
					builder.append(inBuilder);
				}
				else {
					builder.append(columnName).append(operation)
							.append(queryConditionRight(part.getType(),
									part.shouldIgnoreCase(), properties));
				}

			}

		}

		return builder.toString().trim();
	}

	private String resolveParameterName(int position) {
		MybatisParameters parameters = method.getParameters();
		if (parameters.hasParameterAt(position)) {
			return parameters.getParameter(position).getName().orElse("__p" + position);
		}
		return "__p" + position;
	}

	private String getStatementName() {
		return method.getStatementName();
	}

}
