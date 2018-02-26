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
import org.apache.ibatis.session.Configuration;
import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.mybatis.dialect.DialectFactory;
import org.springframework.data.mybatis.mapping.MyBatisMappingContext;
import org.springframework.data.mybatis.mapping.MyBatisPersistentEntity;

import java.util.Arrays;

/**
 * @author Jarvis Song
 */
@Slf4j
public abstract class AbstractMyBatisMapperBuilderAssistant implements MyBatisMapperBuilderAssistant {

	protected final Configuration configuration;
	protected final MyBatisMappingContext mappingContext;

	protected Dialect dialect;
	protected MyBatisPersistentEntity<?> persistentEntity;
	protected Class<?> domainClass;

	protected MapperBuilderAssistant assistant;

	protected AbstractMyBatisMapperBuilderAssistant(Configuration configuration, MyBatisMappingContext mappingContext,
			String namespace, Class<?> domainClass) {
		this.configuration = configuration;
		this.mappingContext = mappingContext;

		this.dialect = DialectFactory.getDialect(configuration);
		this.domainClass = domainClass;
		this.persistentEntity = mappingContext.getPersistentEntity(domainClass);

		this.assistant = new MapperBuilderAssistant(configuration, namespace.replace('.', '/') + ".java (mapper)");
		this.assistant.setCurrentNamespace(namespace);
	}

	/**
	 * Prepare statement for MyBatis.
	 */
	@Override
	public void prepare() {

		doPrepare();

		this.assistant = null;
		this.dialect = null;
		this.persistentEntity = null;
		this.domainClass = null;
	}

	/**
	 * Prepare mapper.
	 */
	protected abstract void doPrepare();

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

		if (log.isDebugEnabled()) {
			log.debug("+++++++ addMappedStatement(" + id + ")【" + this.assistant.getCurrentNamespace() + "】 :\n"
					+ Arrays.toString(sqls));
		}

		addMappedStatement(id, buildSqlSourceFromStrings(sqls, parameterType), StatementType.PREPARED, sqlCommandType, null,
				null, null, parameterType, resultMap, resultType, null, flushCache, useCache, false, keyGenerator, keyProperty,
				keyColumn, null, getLanguageDriver(), null);
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
	 *
	 * @param strings
	 * @param parameterTypeClass
	 * @return
	 */
	protected SqlSource buildSqlSourceFromStrings(String[] strings, Class<?> parameterTypeClass) {
		final StringBuilder sql = new StringBuilder();
		for (String fragment : strings) {
			sql.append(fragment);
			sql.append(" ");
		}
		LanguageDriver languageDriver = getLanguageDriver();
		return languageDriver.createSqlSource(configuration, sql.toString().trim(), parameterTypeClass);
	}

	private LanguageDriver getLanguageDriver() {
		return configuration.getLanguageRegistry().getDriver(XMLLanguageDriver.class);
	}

	/**
	 * static inner class extends AbstractSQL.
	 */
	class SQL extends org.springframework.data.mybatis.repository.query.SQL {

		public SQL() {
			super(AbstractMyBatisMapperBuilderAssistant.this.dialect, mappingContext, persistentEntity);
		}

	}
}
