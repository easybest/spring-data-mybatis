package org.springframework.data.mybatis.repository.support;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.sql.DataSource;
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
import org.apache.ibatis.session.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.mybatis.dialect.H2Dialect;
import org.springframework.data.mybatis.dialect.MySQLDialect;
import org.springframework.data.mybatis.mapping.MybatisPersistentEntity;
import org.springframework.data.mybatis.mapping.MybatisPersistentEntityImpl;
import org.springframework.data.mybatis.mapping.MybatisPersistentProperty;
import org.springframework.data.mybatis.mapping.MybatisPersistentPropertyImpl;

@Slf4j
public abstract class MybatisMapperBuildAssistant implements MybatisMapperBuilder {

	protected static Dialect dialect = null;

	protected FieldNamingStrategy fieldNamingStrategy = PropertyNameFieldNamingStrategy.INSTANCE;

	protected final Configuration configuration;

	protected MybatisPersistentEntity<?> entity;

	protected MapperBuilderAssistant assistant;

	public MybatisMapperBuildAssistant(Configuration configuration,
			PersistentEntity<?, ?> persistentEntity, String namespace) {

		this.configuration = configuration;

		dialect = detectDialect();

		this.entity = (MybatisPersistentEntity<?>) persistentEntity;

		this.assistant = new MapperBuilderAssistant(configuration,
				namespace.replace('.', '/') + ".java (mapper)");
		this.assistant.setCurrentNamespace(namespace);
	}

	@Override
	public void build() {

		try {
			doBuild();
		}
		finally {
			this.assistant = null;
			this.entity = null;
		}

	}

	protected abstract void doBuild();

	protected List<MybatisPersistentProperty> findNormalColumns() {
		List<MybatisPersistentProperty> columns = new ArrayList<>();
		entity.doWithProperties(
				(PropertyHandler<MybatisPersistentProperty>) columns::add);
		return columns;
	}

	protected Dialect detectDialect() {
		if (null != dialect) {
			return dialect;
		}
		log.info("detect dialect ...");
		DataSource ds = configuration.getEnvironment().getDataSource();
		Connection conn = null;
		try {
			conn = ds.getConnection();
			DatabaseMetaData metaData = conn.getMetaData();
			final String databaseName = metaData.getDatabaseProductName();
			if (databaseName.toLowerCase().startsWith("mysql")) {
				dialect = new MySQLDialect();
			}
			else if (databaseName.toLowerCase().startsWith("h2")) {
				dialect = new H2Dialect();
			}
		}
		catch (SQLException e) {
			throw new MappingException("could not detect database dialect.", e);
		}
		finally {
			if (null != conn) {
				try {
					conn.close();
				}
				catch (SQLException e) {
					log.error(e.getMessage(), e);
				}
			}

		}

		if (null == dialect) {
			throw new MappingException("could not detect database dialect.");
		}
		return dialect;
	}

	protected String buildStandardOrderBy(Sort sort) {
		if (null == sort || sort.isUnsorted()) {
			return "";
		}

		final Map<String, String> map = findNormalColumns().stream()
				.collect(Collectors.toMap(p -> p.getName(), p -> p.getColumnName()));
		return " order by " + sort.stream()
				.map(order -> map.getOrDefault(order.getProperty(), order.getProperty())
						+ ' ' + order.getDirection().name().toLowerCase())
				.collect(Collectors.joining(","));

	}

	protected String buildStandardOrderBy() {
		StringBuilder builder = new StringBuilder();

		builder.append("<if test=\"__sort != null\">");
		builder.append("<bind name=\"__columnsMap\" value='#{");

		builder.append(findNormalColumns()
				.stream().map(p -> String.format("&apos;%s&apos;:&apos;%s&apos;",
						p.getName(), p.getColumnName()))
				.collect(Collectors.joining(",")));

		builder.append("}' />");
		builder.append(" order by ");
		builder.append(
				"<foreach item=\"item\" index=\"idx\" collection=\"__sort\" open=\"\" separator=\",\" close=\"\">");
		builder.append("<if test=\"item.ignoreCase\">" + dialect.getLowercaseFunction()
				+ "(</if>").append("${__columnsMap[item.property]}")
				.append("<if test=\"item.ignoreCase\">)</if>")
				.append(" ${item.direction.name().toLowerCase()}");
		builder.append("</foreach>");
		builder.append("</if>");

		return builder.toString();
	}

	protected void addMappedStatement(String id, String[] sqls,
			SqlCommandType sqlCommandType) {

		addMappedStatement(id, sqls, sqlCommandType, null, null, null,
				NoKeyGenerator.INSTANCE, null, null);
	}

	protected void addMappedStatement(String id, String[] sqls,
			SqlCommandType sqlCommandType, Class<?> parameterType) {

		addMappedStatement(id, sqls, sqlCommandType, parameterType, null, null,
				NoKeyGenerator.INSTANCE, null, null);
	}

	protected void addMappedStatement(String id, String[] sqls,
			SqlCommandType sqlCommandType, Class<?> parameterType, Class<?> resultType) {

		addMappedStatement(id, sqls, sqlCommandType, parameterType, null, resultType,
				NoKeyGenerator.INSTANCE, null, null);
	}

	protected void addMappedStatement(String id, String[] sqls,
			SqlCommandType sqlCommandType, Class<?> parameterType, String resultMap) {

		addMappedStatement(id, sqls, sqlCommandType, parameterType, resultMap, null,
				NoKeyGenerator.INSTANCE, null, null);
	}

	protected void addMappedStatement(String id, String[] sqls,
			SqlCommandType sqlCommandType, Class<?> parameterType, String resultMap,
			Class<?> resultType, KeyGenerator keyGenerator, String keyProperty,
			String keyColumn) {

		boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
		boolean flushCache = !isSelect;
		boolean useCache = isSelect;

		addMappedStatement(id, buildSqlSourceFromStrings(sqls, parameterType),
				StatementType.PREPARED, sqlCommandType, null, null, null, parameterType,
				resultMap, resultType, null, flushCache, useCache, false, keyGenerator,
				keyProperty, keyColumn, null, getLanguageDriver(), null);

		if (log.isDebugEnabled()) {
			System.out.println(
					"/*【" + this.assistant.getCurrentNamespace() + '.' + id + "】 */");
			System.out.println((sqls.length > 1 ? sqls[1] : sqls[0]) + ";\n");
		}
	}

	protected void addMappedStatement(String id, SqlSource sqlSource,
			StatementType statementType, SqlCommandType sqlCommandType, Integer fetchSize,
			Integer timeout, String parameterMap, Class<?> parameterType,
			String resultMap, Class<?> resultType, ResultSetType resultSetType,
			boolean flushCache, boolean useCache, boolean resultOrdered,
			KeyGenerator keyGenerator, String keyProperty, String keyColumn,
			String databaseId, LanguageDriver lang, String resultSets) {

		assistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType,
				fetchSize, timeout, parameterMap, parameterType, resultMap, resultType,
				resultSetType, flushCache, useCache, resultOrdered, keyGenerator,
				keyProperty, keyColumn, databaseId, lang, resultSets);
	}

	/**
	 * build sql source for mybatis from string concat by array.
	 */
	protected SqlSource buildSqlSourceFromStrings(String[] strings,
			Class<?> parameterTypeClass) {
		final StringBuilder sql = new StringBuilder();
		for (String fragment : strings) {
			sql.append(fragment);
			sql.append(" ");
		}
		LanguageDriver languageDriver = getLanguageDriver();
		return languageDriver.createSqlSource(configuration, sql.toString().trim(),
				parameterTypeClass);
	}

	private LanguageDriver getLanguageDriver() {
		return configuration.getLanguageRegistry().getDriver(XMLLanguageDriver.class);
	}

	public void setFieldNamingStrategy(FieldNamingStrategy fieldNamingStrategy) {
		this.fieldNamingStrategy = fieldNamingStrategy;
	}

	protected MybatisPersistentProperty createPersistentProperty(Property property,
			MybatisPersistentEntityImpl<?> owner, SimpleTypeHolder simpleTypeHolder) {
		return new MybatisPersistentPropertyImpl(property, owner, simpleTypeHolder);
	}

}
