package org.springframework.data.mybatis.repository.support;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.jdbc.AbstractSQL;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.mybatis.dialect.DialectFactory;
import org.springframework.data.mybatis.mapping.MyBatisMappingContext;
import org.springframework.data.mybatis.mapping.MyBatisPersistentEntity;
import org.springframework.data.mybatis.mapping.MyBatisPersistentEntityImpl;
import org.springframework.data.mybatis.mapping.MyBatisPersistentProperty;
import org.springframework.util.StringUtils;

import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Jarvis Song
 */
@Slf4j
public abstract class AbstractMyBatisMapperBuilderAssistant implements MyBatisMapperBuilderAssistant {

	protected final Configuration configuration;
	protected final MyBatisMappingContext mappingContext;

	protected Dialect dialect;
	protected MapperBuilderAssistant assistant;
	protected MyBatisPersistentEntity<?> persistentEntity;
	protected Class<?> domainClass;

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

	public static class SQL extends AbstractSQL<SQL> {

		protected MyBatisPersistentEntity<?> persistentEntity;
		protected Dialect dialect;
		protected MyBatisMappingContext mappingContext;

		public SQL(MyBatisPersistentEntity<?> persistentEntity, Dialect dialect, MyBatisMappingContext mappingContext) {
			this.persistentEntity = persistentEntity;
			this.dialect = dialect;
			this.mappingContext = mappingContext;
		}

		public SQL() {}

		@Override
		public SQL getSelf() {
			return this;
		}

		public String column(MyBatisPersistentProperty property) {
			return dialect.quote(property.getColumnName());
		}

		public String[] COLUMNS(boolean complex) {
			Set<String> columns = new HashSet<>();
			persistentEntity.doWithProperties((SimplePropertyHandler) pp -> {
				MyBatisPersistentProperty property = (MyBatisPersistentProperty) pp;
				columns.add(dialect.quote(persistentEntity.getEntityName()) + '.' + column(property) + " as "
						+ dialect.quote(property.getName()));
			});
			persistentEntity.doWithAssociations((SimpleAssociationHandler) association -> {
				MyBatisPersistentProperty inverseProperty = (MyBatisPersistentProperty) association.getInverse();
				if (inverseProperty.isIdProperty()) {
					if (inverseProperty.isAnnotationPresent(ManyToOne.class)
							|| inverseProperty.isAnnotationPresent(OneToOne.class)) {
						// TODO
					} else {
						MyBatisPersistentEntityImpl<?> assEntity = mappingContext
								.getPersistentEntity(inverseProperty.getActualType());
						persistentEntity.doWithProperties((SimplePropertyHandler) pp -> {
							MyBatisPersistentProperty property = (MyBatisPersistentProperty) pp;
							columns.add(
									dialect.quote(persistentEntity.getEntityName()) + "." + column(property) + " as " + dialect.quote(
											persistentEntity.getEntityName() + "." + assEntity.getEntityName() + "." + property.getName()));
						});
					}
				}
				// TODO REAL ASSOCIATION
				if (complex) {} else {}
			});
			return columns.toArray(new String[columns.size()]);
		}

		public SQL SELECT_WITH_COLUMNS(boolean complex) {

			SELECT(COLUMNS(complex));

			return getSelf();
		}

		public SQL FROM_WITH_LEFT_OUTER_JOIN(boolean complex) {

			FROM(persistentEntity.getTableName() + " " + dialect.quote(persistentEntity.getEntityName()));

			if (complex) {
				// TODO LEFT OUTER JOIN
			}
			return getSelf();
		}

		public SQL ID_CALUSE() {
			MyBatisPersistentProperty idProperty = persistentEntity.getIdProperty();
			// process id cause
			if (persistentEntity.hasCompositeIdProperty()) {
				if (persistentEntity.isAnnotationPresent(IdClass.class)) {
					persistentEntity.doWithIdProperties(pp -> {
						MyBatisPersistentProperty property = (MyBatisPersistentProperty) pp;
						WHERE(dialect.quote(persistentEntity.getEntityName()) + "." + column(idProperty) + "=#{"
								+ mappingContext.getPersistentEntity(persistentEntity.getCompositeIdClass()).getEntityName() + '.'
								+ property.getName() + "}");
					});
				} else {
					MyBatisPersistentEntityImpl<?> idEntity = mappingContext.getPersistentEntity(idProperty.getActualType());
					idEntity.doWithProperties((SimplePropertyHandler) pp -> {
						MyBatisPersistentProperty property = (MyBatisPersistentProperty) pp;
						WHERE(dialect.quote(persistentEntity.getEntityName()) + "." + column(idProperty) + "=#{"
								+ idProperty.getName() + '.' + property.getName() + "}");
					});
				}

			} else {
				WHERE(dialect.quote(persistentEntity.getEntityName()) + "." + column(idProperty) + "=#{" + idProperty.getName()
						+ "}");
			}
			return getSelf();
		}

		public SQL FIND_CONDITION(boolean complex) {
			return getSelf();
		}

		public SQL ORDER_BY(boolean complex, Sort sort) {
			if (null != sort && sort.isSorted()) {
				String[] columns = COLUMNS(complex);
				Map<String, String> map = Stream.of(columns).filter(c -> StringUtils.hasText(c)).collect(Collectors.toMap(c -> {
					String[] ss = c.split(" as ");
					String key = ss[ss.length - 1];
					key = key.replace(String.valueOf(dialect.openQuote()), "").replace(String.valueOf(dialect.closeQuote()), "");
					return key;
				}, c -> c.split(" as ")[0]));

				sort.forEach(order -> {
					String p = map.get(order.getProperty());
					ORDER_BY((StringUtils.isEmpty(p) ? order.getProperty() : p) + " " + order.getDirection().name());
				});
			}
			return getSelf();
		}
	}
}
