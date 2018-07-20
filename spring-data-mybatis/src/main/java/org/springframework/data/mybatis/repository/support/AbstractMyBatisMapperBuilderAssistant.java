package org.springframework.data.mybatis.repository.support;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.domain.Sort;
import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.mybatis.mapping.ElementCollectionAssociation;
import org.springframework.data.mybatis.mapping.MyBatisMappingContext;
import org.springframework.data.mybatis.mapping.MyBatisPersistentEntity;
import org.springframework.data.mybatis.mapping.Table;
import org.springframework.data.mybatis.mapping.ToManyJoinTableAssociation;
import org.springframework.data.mybatis.mapping.ToOneAssociation;
import org.springframework.data.mybatis.mapping.ToOneJoinColumnAssociation;
import org.springframework.data.mybatis.mapping.ToOneJoinTableAssociation;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Jarvis Song
 */
@Slf4j
public abstract class AbstractMyBatisMapperBuilderAssistant implements MyBatisMapperBuilderAssistant {

	protected final SqlSessionTemplate sqlSessionTemplate;
	protected final MyBatisMappingContext mappingContext;
	protected final Dialect dialect;

	protected MyBatisPersistentEntity<?> persistentEntity;
	protected Class<?> domainClass;

	protected MapperBuilderAssistant assistant;

	protected AbstractMyBatisMapperBuilderAssistant(SqlSessionTemplate sqlSessionTemplate,
			MyBatisMappingContext mappingContext, Dialect dialect, String namespace, Class<?> domainClass) {
		this.sqlSessionTemplate = sqlSessionTemplate;
		this.mappingContext = mappingContext;
		this.dialect = dialect;

		this.domainClass = domainClass;
		this.persistentEntity = mappingContext.getPersistentEntity(domainClass);

		this.assistant = new MapperBuilderAssistant(sqlSessionTemplate.getConfiguration(),
				namespace.replace('.', '/') + ".java (mapper)");
		this.assistant.setCurrentNamespace(namespace);
	}

	/**
	 * Prepare statement for MyBatis.
	 */
	@Override
	public void prepare() {

		doPrepare();

		this.assistant = null;
		this.persistentEntity = null;
		this.domainClass = null;
	}

	/**
	 * Prepare mapper.
	 */
	protected abstract void doPrepare();

	/**
	 * mapping property => query column.
	 */
	protected Map<String, String> fetchStandardSelectColumns(boolean complex) {
		Table table = persistentEntity.getTable();
		Map<String, String> map = new LinkedHashMap<>();
		if (complex) {
			table.getAssociations().stream().filter(association -> association instanceof ToOneAssociation)
					.forEach(association -> ((ToOneAssociation) association).getInverseJoinTable().getColumnsIncludeId().stream()
							.forEach(column -> map.put(
									((ToOneAssociation) association).getJoinProperty().getName() + '.' + column.getAlias(),
									((ToOneAssociation) association).getInverseJoinTable().getQuotedAlias(dialect) + '.'
											+ column.getActualName(dialect))));
		}
		table.getColumnsIncludeId()
				.forEach(column -> map.put(column.getAlias(), column.getQueryColumnName(complex, dialect)));
		return map;
	}

	protected Map<String, String> fetchStandardOrderByColumns(boolean complex) {

		Map<String, String> map = new LinkedHashMap<>();

		if (complex) {
			Table table = persistentEntity.getTable();
			table.getAssociations().stream().filter(association -> association instanceof ToManyJoinTableAssociation)
					.forEach(association -> {
						((ToManyJoinTableAssociation) association).getInverseJoinTable().getColumnsIncludeId().forEach(column -> {
							map.put(
									((ToManyJoinTableAssociation) association).getJoinProperty().getName() + '.'
											+ column.getProperty().getName(),
									((ToManyJoinTableAssociation) association).getInverseJoinTable().getQuotedAlias(dialect) + '.'
											+ column.getActualName(dialect));
						});
					});
		}
		map.putAll(fetchStandardSelectColumns(complex));

		return map;

	}

	protected String buildStandardSelectColumns(boolean complex) {

		return fetchStandardSelectColumns(complex).entrySet().stream()
				.map(entry -> entry.getValue() + " as " + dialect.openQuote() + entry.getKey() + dialect.closeQuote())
				.collect(Collectors.joining(","));
	}

	protected String buildStandardFrom(boolean complex) {
		Table table = persistentEntity.getTable();
		StringBuilder builder = new StringBuilder();
		builder.append(table.getFullName(dialect));
		if (complex) {
			builder.append(' ').append(table.getQuotedAlias(dialect));
			builder.append(table.getAssociations().stream().map(association -> {
				StringBuilder join = new StringBuilder();
				// left outer join ds_user_ds_role `roles_ds_user_ds_role` on `user`.id = `roles_ds_user_ds_role`.user_id
				// left outer join ds_role `user.roles` on `roles_ds_user_ds_role`.role_id = `user.roles`.id
				if (association instanceof ToOneJoinTableAssociation) {
					join.append(" left outer join ")
							.append(((ToOneJoinTableAssociation) association).getJoinTable().getFullName(dialect)).append(' ')
							.append(((ToOneJoinTableAssociation) association).getJoinTable().getQuotedAlias(dialect)).append(" on ");

					join.append(Stream.of(((ToOneJoinTableAssociation) association).getJoinColumns()).map(joinTableColumn -> {
						StringBuilder jtc = new StringBuilder();
						jtc.append(joinTableColumn.getQuotedPrefix(dialect)).append('.')
								.append(joinTableColumn.getActualReferencedColumnName(dialect)).append('=');
						jtc.append(((ToOneJoinTableAssociation) association).getJoinTable().getQuotedAlias(dialect)).append('.')
								.append(joinTableColumn.getActualName(dialect));
						return jtc.toString();
					}).collect(Collectors.joining(" and ")));

					join.append(" left outer join ")
							.append(((ToOneJoinTableAssociation) association).getInverseJoinTable().getFullName(dialect)).append(' ')
							.append(((ToOneJoinTableAssociation) association).getInverseJoinTable().getQuotedAlias(dialect))
							.append(" on ");
					join.append(
							Stream.of(((ToOneJoinTableAssociation) association).getInverseJoinColumns()).map(joinTableColumn -> {
								StringBuilder jtc = new StringBuilder();

								jtc.append(((ToOneJoinTableAssociation) association).getJoinTable().getQuotedAlias(dialect)).append('.')
										.append(joinTableColumn.getActualName(dialect)).append("=");
								jtc.append(((ToOneJoinTableAssociation) association).getInverseJoinTable().getQuotedAlias(dialect))
										.append('.').append(joinTableColumn.getActualReferencedColumnName(dialect));
								return jtc.toString();
							}).collect(Collectors.joining(" and ")));
				} else if (association instanceof ToOneJoinColumnAssociation) {
					join.append(" left outer join ")
							.append(((ToOneJoinColumnAssociation) association).getInverseJoinTable().getFullName(dialect)).append(' ')
							.append(((ToOneJoinColumnAssociation) association).getInverseJoinTable().getQuotedAlias(dialect))
							.append(" on ");
					join.append(Stream.of(((ToOneJoinColumnAssociation) association).getJoinColumns()).map(joinColumn -> {
						StringBuilder jtc = new StringBuilder();
						jtc.append(joinColumn.getQuotedPrefix(dialect)).append('.').append(joinColumn.getActualName(dialect))
								.append('=')
								.append(((ToOneJoinColumnAssociation) association).getInverseJoinTable().getQuotedAlias(dialect))
								.append('.').append(joinColumn.getActualReferencedColumnName(dialect));
						return jtc.toString();
					}).collect(Collectors.joining(" and ")));
				} else if (association instanceof ElementCollectionAssociation) {
					join.append(" left outer join ")
							.append(((ElementCollectionAssociation) association).getInverseJoinTable().getFullName(dialect))
							.append(' ')
							.append(((ElementCollectionAssociation) association).getInverseJoinTable().getQuotedAlias(dialect))
							.append(" on ");

					join.append(Stream.of(((ElementCollectionAssociation) association).getJoinColumns()).map(joinTableColumn -> {
						StringBuilder jtc = new StringBuilder();
						jtc.append(joinTableColumn.getQuotedPrefix(dialect)).append('.')
								.append(joinTableColumn.getActualReferencedColumnName(dialect)).append('=');
						jtc.append(((ElementCollectionAssociation) association).getInverseJoinTable().getQuotedAlias(dialect))
								.append('.').append(joinTableColumn.getActualName(dialect));
						return jtc.toString();
					}).collect(Collectors.joining(" and ")));
				} else if (association instanceof ToManyJoinTableAssociation) {
					join.append(" left outer join ")
							.append(((ToManyJoinTableAssociation) association).getJoinTable().getFullName(dialect)).append(' ')
							.append(((ToManyJoinTableAssociation) association).getJoinTable().getQuotedAlias(dialect)).append(" on ");

					join.append(Stream.of(((ToManyJoinTableAssociation) association).getJoinColumns()).map(joinTableColumn -> {
						StringBuilder jtc = new StringBuilder();
						jtc.append(joinTableColumn.getQuotedPrefix(dialect)).append('.')
								.append(joinTableColumn.getActualReferencedColumnName(dialect)).append('=');
						jtc.append(((ToManyJoinTableAssociation) association).getJoinTable().getQuotedAlias(dialect)).append('.')
								.append(joinTableColumn.getActualName(dialect));
						return jtc.toString();
					}).collect(Collectors.joining(" and ")));

					join.append(" left outer join ")
							.append(((ToManyJoinTableAssociation) association).getInverseJoinTable().getFullName(dialect)).append(' ')
							.append(((ToManyJoinTableAssociation) association).getInverseJoinTable().getQuotedAlias(dialect))
							.append(" on ");
					join.append(
							Stream.of(((ToManyJoinTableAssociation) association).getInverseJoinColumns()).map(joinTableColumn -> {
								StringBuilder jtc = new StringBuilder();

								jtc.append(((ToManyJoinTableAssociation) association).getJoinTable().getQuotedAlias(dialect))
										.append('.').append(joinTableColumn.getActualName(dialect)).append("=");
								jtc.append(((ToManyJoinTableAssociation) association).getInverseJoinTable().getQuotedAlias(dialect))
										.append('.').append(joinTableColumn.getActualReferencedColumnName(dialect));
								return jtc.toString();
							}).collect(Collectors.joining(" and ")));
				}

				return join.toString();
			}).filter(StringUtils::hasText).collect(Collectors.joining()));

		}
		return builder.toString();
	}

	protected String buildStandardOrderBy(boolean complex, Sort sort) {
		if (null == sort || sort.isUnsorted()) {
			return "";
		}

		final Map<String, String> map = fetchStandardSelectColumns(complex);

		return " order by " + sort.stream().map(order -> map.getOrDefault(order.getProperty(), order.getProperty()) + ' '
				+ order.getDirection().name().toLowerCase()).collect(Collectors.joining(","));

	}

	protected String buildStandardOrderBy(boolean complex) {
		StringBuilder builder = new StringBuilder();
		builder.append("<if test=\"_sorts != null\">");
		builder.append("<bind name=\"__columnsMap\" value='#{");
		final Map<String, String> columnMap = fetchStandardOrderByColumns(complex);
		builder.append(columnMap.entrySet().stream()
				.map(entry -> "&apos;" + entry.getKey() + "&apos;" + ':' + "&apos;" + entry.getValue() + "&apos;")
				.collect(Collectors.joining(",")));

		builder.append("}' />");
		builder.append(" order by ");
		builder.append("<foreach item=\"item\" index=\"idx\" collection=\"_sorts\" open=\"\" separator=\",\" close=\"\">");
		builder.append("<if test=\"item.ignoreCase\">" + dialect.getLowercaseFunction() + "(</if>")
				.append("${__columnsMap[item.property]}").append("<if test=\"item.ignoreCase\">)</if>")
				.append(" ${item.direction.name().toLowerCase()}");
		builder.append("</foreach>");
		builder.append("</if>");
		return builder.toString();
	}

	protected void addMappedStatement(String id, String[] sqls, SqlCommandType sqlCommandType) {

		addMappedStatement(id, sqls, sqlCommandType, null, null, null, NoKeyGenerator.INSTANCE, null, null);
	}

	protected void addMappedStatement(String id, String[] sqls, SqlCommandType sqlCommandType, Class<?> parameterType) {

		addMappedStatement(id, sqls, sqlCommandType, parameterType, null, null, NoKeyGenerator.INSTANCE, null, null);
	}

	protected void addMappedStatement(String id, String[] sqls, SqlCommandType sqlCommandType, Class<?> parameterType,
			Class<?> resultType) {

		addMappedStatement(id, sqls, sqlCommandType, parameterType, null, resultType, NoKeyGenerator.INSTANCE, null, null);
	}

	protected void addMappedStatement(String id, String[] sqls, SqlCommandType sqlCommandType, Class<?> parameterType,
			String resultMap) {

		addMappedStatement(id, sqls, sqlCommandType, parameterType, resultMap, null, NoKeyGenerator.INSTANCE, null, null);
	}

	protected void addMappedStatement(String id, String[] sqls, SqlCommandType sqlCommandType, Class<?> parameterType,
			String resultMap, Class<?> resultType, KeyGenerator keyGenerator, String keyProperty, String keyColumn) {

		boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
		boolean flushCache = !isSelect;
		boolean useCache = isSelect;

		addMappedStatement(id, buildSqlSourceFromStrings(sqls, parameterType), StatementType.PREPARED, sqlCommandType, null,
				null, null, parameterType, resultMap, resultType, null, flushCache, useCache, false, keyGenerator, keyProperty,
				keyColumn, null, getLanguageDriver(), null);

		if (log.isDebugEnabled()) {
			System.out.println("/*【" + this.assistant.getCurrentNamespace() + '.' + id + "】 */");
			System.out.println((sqls.length > 1 ? sqls[1] : sqls[0]) + ";\n");
		}
	}

	protected void addMappedStatement(String id, SqlSource sqlSource, StatementType statementType,
			SqlCommandType sqlCommandType, Integer fetchSize, Integer timeout, String parameterMap, Class<?> parameterType,
			String resultMap, Class<?> resultType, ResultSetType resultSetType, boolean flushCache, boolean useCache,
			boolean resultOrdered, KeyGenerator keyGenerator, String keyProperty, String keyColumn, String databaseId,
			LanguageDriver lang, String resultSets) {

		assistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType, fetchSize, timeout, parameterMap,
				parameterType, resultMap, resultType, resultSetType, flushCache, useCache, resultOrdered, keyGenerator,
				keyProperty, keyColumn, databaseId, lang, resultSets);
	}

	/**
	 * build sql source for mybatis from string concat by array.
	 */
	protected SqlSource buildSqlSourceFromStrings(String[] strings, Class<?> parameterTypeClass) {
		final StringBuilder sql = new StringBuilder();
		for (String fragment : strings) {
			sql.append(fragment);
			sql.append(" ");
		}
		LanguageDriver languageDriver = getLanguageDriver();
		return languageDriver.createSqlSource(sqlSessionTemplate.getConfiguration(), sql.toString().trim(),
				parameterTypeClass);
	}

	private LanguageDriver getLanguageDriver() {
		return sqlSessionTemplate.getConfiguration().getLanguageRegistry().getDriver(XMLLanguageDriver.class);
	}

}
