package org.springframework.data.mybatis.repository.support;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.jdbc.AbstractSQL;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.mybatis.annotation.GenericGenerator;
import org.springframework.data.mybatis.annotation.GenericGenerators;
import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.mybatis.mapping.MyBatisMappingContext;
import org.springframework.data.mybatis.mapping.MyBatisPersistentEntity;
import org.springframework.data.mybatis.mapping.MyBatisPersistentEntityImpl;
import org.springframework.data.mybatis.mapping.MyBatisPersistentProperty;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import javax.persistence.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.ibatis.mapping.SqlCommandType.*;

/**
 * @author Jarvis Song
 */
@Slf4j
public class MyBatisMapperBuilderAssistant {

	public static final String DEFAULT_SEQUENCE_NAME = "seq_spring_data_mybatis";

	private final Configuration configuration;
	private final MyBatisMappingContext mappingContext;
	private final Dialect dialect;
	private final RepositoryInformation repositoryInformation;
	private final Class<?> domainClass;
	private final String namespace;
	private final MyBatisPersistentEntity<?> persistentEntity;
	private final MapperBuilderAssistant assistant;
	private final LanguageDriver languageDriver;

	private Map<String, SequenceGenerator> sequenceGeneratorCache = new HashMap<>();
	private Map<String, GenericGenerator> genericGeneratorCache = new HashMap<>();

	public MyBatisMapperBuilderAssistant(Configuration configuration, MyBatisMappingContext mappingContext,
			Dialect dialect, RepositoryInformation repositoryInformation) {

		this.configuration = configuration;
		this.mappingContext = mappingContext;
		this.dialect = dialect;
		this.repositoryInformation = repositoryInformation;

		this.domainClass = repositoryInformation.getDomainType();
		this.persistentEntity = mappingContext.getPersistentEntity(this.domainClass);
		this.namespace = repositoryInformation.getRepositoryInterface().getName();
		this.assistant = new MapperBuilderAssistant(configuration, this.namespace.replace('.', '/') + ".java (mapper)");
		this.assistant.setCurrentNamespace(this.namespace);
		this.languageDriver = configuration.getLanguageRegistry().getDriver(XMLLanguageDriver.class);
		prepareSequenceGenerator();
		prepareGenericGenerator();

	}

	/**
	 * <code>
	 public abstract long org.springframework.data.repository.CrudRepository.count()
	 public abstract long org.springframework.data.repository.query.QueryByExampleExecutor.count(org.springframework.data.domain.Example)
	 public abstract long org.springframework.data.mybatis.repository.MyBatisRepository.countAll(java.lang.Object)
	 public abstract long org.springframework.data.mybatis.repository.MyBatisRepository.countBasicAll(java.lang.Object)
	 public abstract void org.springframework.data.repository.CrudRepository.delete(java.lang.Object)
	 public abstract void org.springframework.data.repository.CrudRepository.deleteAll(java.lang.Iterable)
	 public abstract void org.springframework.data.repository.CrudRepository.deleteAll()
	 public abstract void org.springframework.data.mybatis.repository.MyBatisRepository.deleteAllInBatch()
	 public abstract void org.springframework.data.mybatis.repository.sample.UserRepository.deleteById(java.lang.Integer)
	 public abstract void org.springframework.data.mybatis.repository.MyBatisRepository.deleteInBatch(java.lang.Iterable)
	 public abstract boolean org.springframework.data.repository.query.QueryByExampleExecutor.exists(org.springframework.data.domain.Example)
	 public abstract boolean org.springframework.data.repository.CrudRepository.existsById(java.lang.Object)
	 public abstract java.util.List org.springframework.data.mybatis.repository.MyBatisRepository.findAll(org.springframework.data.domain.Example,org.springframework.data.domain.Sort)
	 public abstract java.util.List org.springframework.data.mybatis.repository.MyBatisRepository.findAll()
	 public abstract java.util.List org.springframework.data.mybatis.repository.MyBatisRepository.findAll(org.springframework.data.domain.Sort)
	 public abstract java.util.List org.springframework.data.mybatis.repository.MyBatisRepository.findAll(org.springframework.data.domain.Example)
	 public abstract java.util.List org.springframework.data.mybatis.repository.MyBatisRepository.findAll(org.springframework.data.domain.Sort,java.lang.Object)
	 public abstract java.util.List org.springframework.data.mybatis.repository.MyBatisRepository.findAll(java.lang.Object)
	 public abstract org.springframework.data.domain.Page org.springframework.data.mybatis.repository.MyBatisRepository.findAll(org.springframework.data.domain.Pageable,java.lang.Object)
	 public abstract org.springframework.data.domain.Page org.springframework.data.repository.PagingAndSortingRepository.findAll(org.springframework.data.domain.Pageable)
	 public abstract org.springframework.data.domain.Page org.springframework.data.repository.query.QueryByExampleExecutor.findAll(org.springframework.data.domain.Example,org.springframework.data.domain.Pageable)
	 public abstract java.util.List org.springframework.data.mybatis.repository.MyBatisRepository.findAllById(java.lang.Iterable)
	 public abstract org.springframework.data.domain.Page org.springframework.data.mybatis.repository.MyBatisRepository.findBasicAll(org.springframework.data.domain.Pageable,java.lang.Object)
	 public abstract java.util.List org.springframework.data.mybatis.repository.MyBatisRepository.findBasicAll(org.springframework.data.domain.Sort)
	 public abstract java.util.List org.springframework.data.mybatis.repository.MyBatisRepository.findBasicAll(java.lang.Object)
	 public abstract java.util.List org.springframework.data.mybatis.repository.MyBatisRepository.findBasicAll()
	 public abstract java.util.List org.springframework.data.mybatis.repository.MyBatisRepository.findBasicAll(org.springframework.data.domain.Sort,java.lang.Object)
	 public abstract java.util.List org.springframework.data.mybatis.repository.MyBatisRepository.findBasicAllById(java.lang.Iterable)
	 public abstract java.util.Optional org.springframework.data.mybatis.repository.MyBatisRepository.findBasicById(java.lang.Object)
	 public abstract java.util.Optional org.springframework.data.mybatis.repository.MyBatisRepository.findBasicOne(java.lang.Object)
	 public abstract java.util.Optional org.springframework.data.mybatis.repository.sample.UserRepository.findById(java.lang.Integer)
	 public abstract java.util.Optional org.springframework.data.mybatis.repository.MyBatisRepository.findOne(java.lang.Object)
	 public abstract java.util.Optional org.springframework.data.repository.query.QueryByExampleExecutor.findOne(org.springframework.data.domain.Example)
	 public abstract java.lang.Object org.springframework.data.mybatis.repository.MyBatisRepository.getBasicById(java.lang.Object)
	 public abstract java.lang.Object org.springframework.data.mybatis.repository.MyBatisRepository.getBasicOne(java.lang.Object)
	 public abstract org.springframework.data.mybatis.domain.sample.User org.springframework.data.mybatis.repository.sample.UserRepository.getById(java.lang.Integer)
	 public abstract java.lang.Object org.springframework.data.mybatis.repository.MyBatisRepository.getOne(java.lang.Object)
	 public abstract java.lang.Object org.springframework.data.mybatis.repository.MyBatisRepository.insert(java.lang.Object)
	 public abstract java.lang.Object org.springframework.data.repository.CrudRepository.save(java.lang.Object)
	 public abstract java.util.List org.springframework.data.mybatis.repository.MyBatisRepository.saveAll(java.lang.Iterable)
	 public abstract java.lang.Object org.springframework.data.mybatis.repository.MyBatisRepository.saveIgnoreNull(java.lang.Object)
	 public abstract java.lang.Object org.springframework.data.mybatis.repository.MyBatisRepository.update(java.lang.Object)
	 public abstract java.lang.Object org.springframework.data.mybatis.repository.MyBatisRepository.updateIgnoreNull(java.lang.Object)
	
	 * </code>
	 */
	public void parse() {

		assistant.setCurrentNamespace(namespace);

		// generic
		addInsertStatement();// _insert
		addGetByIdStatement(false);// _getBasicById
		addGetByIdStatement(true);// _getById
		addCountStatement();// _count
		addDeleteAllStatement();// _deleteAll
		addDeleteByIdStatement();// _deleteById
		addFindAllStatement();// _findAll
		addFindByPagerStatement(true);// _findByPager
		addFindByPagerStatement(false);// _findBasicByPager
		addCountByConditionStatement(true);// _countByCondition
		addCountByConditionStatement(false);// _countBasicByCondition

		// personalise
		Class<?> repositoryInterface = repositoryInformation.getRepositoryInterface();
		Stream.of(repositoryInterface.getMethods())
				.map(method -> ClassUtils.getMostSpecificMethod(method, repositoryInterface))
				.filter(method -> repositoryInformation.isBaseClassMethod(method) && !method.isBridge() && !method.isDefault()
						&& !Modifier.isStatic(method.getModifiers()))
				.distinct().sorted(Comparator.comparing(Method::getName)).forEachOrdered(method -> {
					parseStatement(method);
				});

	}

	private void parseStatement(Method method) {
		if (method.isAnnotationPresent(Select.class) || method.isAnnotationPresent(Insert.class)
				|| method.isAnnotationPresent(Update.class) || method.isAnnotationPresent(Delete.class)) {
			// TODO parse statement by method of repository interface.

		}
	}

	/**
	 * prepare @SequenceGenerator.
	 */
	private void prepareSequenceGenerator() {
		cacheSequenceGenerator(persistentEntity.findAnnotation(SequenceGenerators.class),
				persistentEntity.findAnnotation(SequenceGenerator.class));
		persistentEntity.doWithProperties((PropertyHandler<MyBatisPersistentProperty>) property -> {
			cacheSequenceGenerator(property.findAnnotation(SequenceGenerators.class),
					property.findAnnotation(SequenceGenerator.class));
		});
	}

	private void cacheSequenceGenerator(SequenceGenerators sequenceGenerators, SequenceGenerator sequenceGenerator) {
		if (null != sequenceGenerator) {
			if (sequenceGeneratorCache.containsKey(sequenceGenerator.name())) {
				throw new MappingException(
						"@SequenceGenerator find the same name [" + sequenceGenerator.name() + "] in " + domainClass);
			}
			sequenceGeneratorCache.put(sequenceGenerator.name(), sequenceGenerator);
		}
		if (null != sequenceGenerators && null != sequenceGenerators.value() && sequenceGenerators.value().length > 0) {
			for (SequenceGenerator generator : sequenceGenerators.value()) {
				if (sequenceGeneratorCache.containsKey(generator.name())) {
					throw new MappingException(
							"@SequenceGenerator find the same name [" + generator.name() + "] in " + domainClass);
				}
				sequenceGeneratorCache.put(generator.name(), generator);
			}
		}
	}

	private void prepareGenericGenerator() {
		cacheGenericGenerator(persistentEntity.findAnnotation(GenericGenerators.class),
				persistentEntity.findAnnotation(GenericGenerator.class));
		persistentEntity.doWithProperties((PropertyHandler<MyBatisPersistentProperty>) property -> {
			cacheGenericGenerator(property.findAnnotation(GenericGenerators.class),
					property.findAnnotation(GenericGenerator.class));
		});
	}

	private void cacheGenericGenerator(GenericGenerators genericGenerators, GenericGenerator genericGenerator) {
		if (null != genericGenerator) {
			if (genericGeneratorCache.containsKey(genericGenerator.name())) {
				throw new MappingException(
						"@GenericGenerator find the same name [" + genericGenerator.name() + "] in " + domainClass);
			}
			genericGeneratorCache.put(genericGenerator.name(), genericGenerator);
		}
		if (null != genericGenerators && null != genericGenerators.value() && genericGenerators.value().length > 0) {
			for (GenericGenerator generator : genericGenerators.value()) {
				if (genericGeneratorCache.containsKey(generator.name())) {
					throw new MappingException(
							"@SequenceGenerator find the same name [" + generator.name() + "] in " + domainClass);
				}
				genericGeneratorCache.put(generator.name(), generator);
			}
		}
	}

	public void addStatement(String mappedStatementName, String[] sql, SqlCommandType sqlCommandType,
			Class<?> parameterTypeClass, String resultMapId, Class<?> returnType, KeyGenerator keyGenerator,
			String keyProperty, String keyColumn) {

		boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
		boolean flushCache = !isSelect;
		boolean useCache = isSelect;

		if (log.isDebugEnabled()) {
			log.debug(mappedStatementName + " :\n" + Arrays.toString(sql));
		}

		assistant.addMappedStatement(mappedStatementName, buildSqlSourceFromStrings(sql, parameterTypeClass),
				StatementType.PREPARED, sqlCommandType, null, null,
				// ParameterMapID
				null, parameterTypeClass, resultMapId, returnType, ResultSetType.FORWARD_ONLY, flushCache, useCache, false,
				keyGenerator, keyProperty, keyColumn,
				// DatabaseID
				null, languageDriver,
				// ResultSets
				null);
	}

	/**
	 * build sql source for mybatis from string concat by array.
	 * 
	 * @param strings
	 * @param parameterTypeClass
	 * @return
	 */
	private SqlSource buildSqlSourceFromStrings(String[] strings, Class<?> parameterTypeClass) {
		final StringBuilder sql = new StringBuilder();
		for (String fragment : strings) {
			sql.append(fragment);
			sql.append(" ");
		}

		return languageDriver.createSqlSource(configuration, sql.toString().trim(), parameterTypeClass);
	}

	private KeyGenerator handleSelectKeyFromIdProperty(MyBatisPersistentProperty idProperty, String baseStatementId,
			Class<?> parameterTypeClass, boolean executeBefore, String generator) {
		String id = baseStatementId + SelectKeyGenerator.SELECT_KEY_SUFFIX;
		Class<?> resultTypeClass = idProperty.getActualType();
		StatementType statementType = StatementType.PREPARED;
		String keyProperty = idProperty.getName();
		String keyColumn = column(idProperty);

		// defaults
		boolean useCache = false;
		KeyGenerator keyGenerator = NoKeyGenerator.INSTANCE;
		Integer fetchSize = null;
		Integer timeout = null;
		boolean flushCache = false;
		String parameterMap = null;
		String resultMap = null;
		ResultSetType resultSetTypeEnum = null;

		String[] sqls;
		if (executeBefore) {
			String sequenceName = DEFAULT_SEQUENCE_NAME;
			if (StringUtils.hasText(generator)) {
				SequenceGenerator sequenceGenerator = sequenceGeneratorCache.get(generator);
				if (null != sequenceGenerator && StringUtils.hasText(sequenceGenerator.sequenceName())) {
					sequenceName = sequenceGenerator.sequenceName();
				}
			}
			sqls = new String[] { dialect.getSequenceNextValString(sequenceName) };
		} else {
			sqls = new String[] { dialect.getIdentityColumnSupport().getIdentitySelectString(persistentEntity.getTableName(),
					idProperty.getColumnName(), idProperty.getJdbcType().TYPE_CODE) };
		}

		SqlSource sqlSource = buildSqlSourceFromStrings(sqls, parameterTypeClass);
		SqlCommandType sqlCommandType = SqlCommandType.SELECT;

		assistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType, fetchSize, timeout, parameterMap,
				parameterTypeClass, resultMap, resultTypeClass, resultSetTypeEnum, flushCache, useCache, false, keyGenerator,
				keyProperty, keyColumn, null, languageDriver, null);

		id = assistant.applyCurrentNamespace(id, false);

		MappedStatement keyStatement = configuration.getMappedStatement(id, false);
		SelectKeyGenerator answer = new SelectKeyGenerator(keyStatement, executeBefore);
		configuration.addKeyGenerator(id, answer);
		return answer;
	}

	/**
	 * 1 将组件类注解为@Embeddable,并将组件的属性注解为@Id <br/>
	 * 2 将组件的属性注解为@EmbeddedId (方便) <br/>
	 * 3 将类注解为@IdClass,并将该实体中所有属于主键的属性都注解为@Id<br/>
	 *
	 * @return
	 */
	private void addInsertStatement() {

		KeyGenerator keyGenerator = NoKeyGenerator.INSTANCE;
		String keyProperty = null, keyColumn = null;

		if (persistentEntity.hasIdProperty() && !persistentEntity.hasCompositeIdProperty()) {
			MyBatisPersistentProperty idProperty = persistentEntity.getIdProperty();
			keyProperty = idProperty.getName();
			keyColumn = column(idProperty);
			GeneratedValue generatedValue = idProperty.findAnnotation(GeneratedValue.class);
			if (null != generatedValue) {
				// using @GeneratedValue
				if (generatedValue.strategy() == GenerationType.AUTO) {
					if (StringUtils.hasText(generatedValue.generator())) {
						GenericGenerator genericGenerator = genericGeneratorCache.get(generatedValue.generator());
						if (null == genericGenerator) {
							throw new MappingException("Id @GeneratedValue has assigned generator=" + generatedValue.generator()
									+ ", but can not find any @GenericGenerator with name=" + generatedValue.generator());
						}
						if (StringUtils.isEmpty(genericGenerator.strategy())) {
							throw new MappingException("Id @GeneratedValue has assigned generator=" + generatedValue.generator()
									+ ", but can not find strategy in @GenericGenerator with name=" + generatedValue.generator());
						}

						// TODO USE @GenericGenerator's strategy

					} else {
						if ("identity".equals(dialect.getNativeIdentifierGeneratorStrategy())) {
							keyGenerator = handleSelectKeyFromIdProperty(idProperty, "_update", domainClass, false,
									generatedValue.generator());
						} else if ("sequence".equals(dialect.getNativeIdentifierGeneratorStrategy())) {
							keyGenerator = handleSelectKeyFromIdProperty(idProperty, "_update", domainClass, true,
									generatedValue.generator());
						} else {
							throw new MappingException("Id @GeneratedValue with strategy=AUTO, but dialect[" + dialect.getClass()
									+ "] not support identity or sequence.");
						}
					}
				} else if (generatedValue.strategy() == GenerationType.IDENTITY) {
					if (!dialect.getIdentityColumnSupport().supportsIdentityColumns()) {
						throw new MappingException(
								domainClass + " Id @GeneratedValue set stratety=IDENTITY, but this database's dialect["
										+ dialect.getClass() + "] not support identity.");
					}
					keyGenerator = handleSelectKeyFromIdProperty(idProperty, "_update", domainClass, false,
							generatedValue.generator());

				} else if (generatedValue.strategy() == GenerationType.SEQUENCE) {
					if (!dialect.supportsSequences()) {
						throw new MappingException(
								domainClass + " Id @GeneratedValue set stratety=SEQUENCE, but this database's dialect["
										+ dialect.getClass() + "] not support sequence.");
					}
					keyGenerator = handleSelectKeyFromIdProperty(idProperty, "_update", domainClass, true,
							generatedValue.generator());
				} else if (generatedValue.strategy() == GenerationType.TABLE) {
					// TODO support table id
				}
			}
		}

		SQL sql = new SQL(persistentEntity, dialect, mappingContext) {
			{
				INSERT_INTO(tableName());
				persistentEntity.doWithProperties((SimplePropertyHandler) pp -> {
					MyBatisPersistentProperty property = (MyBatisPersistentProperty) pp;
					VALUES(column(property), "#{" + property.getName() + ",jdbcType=" + property.getJdbcType() + "}");
				});
				persistentEntity.doWithAssociations((SimpleAssociationHandler) ass -> {

					MyBatisPersistentProperty inverseProperty = (MyBatisPersistentProperty) ass.getInverse();
					MyBatisPersistentEntity<?> targetEntity = inverseProperty.getOwnerEntity().getMappingContext()
							.getPersistentEntity(inverseProperty.getActualType());
					/**
					 * 1 将组件类注解为@Embeddable,并将组件的属性注解为@Id <br/>
					 * 2 将组件的属性注解为@EmbeddedId (方便) <br/>
					 * 3 将类注解为@IdClass,并将该实体中所有属于主键的属性都注解为@Id<br/>
					 */

					// @Id + @Embeddable || @EmbeddedId
					if ((inverseProperty.isAnnotationPresent(Id.class) && targetEntity.isAnnotationPresent(Embeddable.class))
							|| inverseProperty.isAnnotationPresent(EmbeddedId.class)) {
						targetEntity.doWithProperties((SimplePropertyHandler) idp1 -> {
							MyBatisPersistentProperty idp = (MyBatisPersistentProperty) idp1;
							VALUES(column(idp),
									"#{" + targetEntity.getName() + "." + idp.getName() + ",jdbcType=" + idp.getJdbcType() + "}");
						});
					}

					// with @Id + @ManyToOne || @Id + @OneToOne
					if (inverseProperty.isAnnotationPresent(Id.class) && (inverseProperty.isAnnotationPresent(ManyToOne.class)
							|| inverseProperty.isAnnotationPresent(OneToOne.class))) {
						// TODO
					}

				});

			}
		};
		addStatement("_insert", new String[] { sql.toString() }, INSERT, domainClass, null, null, keyGenerator, keyProperty,
				keyColumn);

	}

	private void addGetByIdStatement(boolean complex) {
		MyBatisPersistentProperty idProperty = persistentEntity.getIdProperty();

		SQL sql = new SQL(persistentEntity, dialect, mappingContext) {
			{
				SELECT_WITH_COLUMNS(complex);
				FROM_WITH_LEFT_OUTER_JOIN(complex);
				if (complex) {
					// TODO LEFT OUTER JOIN
				}
				ID_CALUSE();// process id cause
			}
		};
		addStatement(complex ? "_getById" : "_getBasicById", new String[] { sql.toString() }, SELECT,
				persistentEntity.getIdClass(), null, domainClass, NoKeyGenerator.INSTANCE, null, null);
	}

	private void addCountStatement() {
		SQL sql = new SQL(persistentEntity, dialect, mappingContext) {
			{
				SELECT("count(*)");
				FROM_WITH_LEFT_OUTER_JOIN(false);
			}
		};
		addStatement("_count", new String[] { sql.toString() }, SELECT, domainClass, null, long.class,
				NoKeyGenerator.INSTANCE, null, null);
	}

	private void addDeleteAllStatement() {
		addStatement("_deleteAll", new String[] { "truncate table " + dialect.quote(persistentEntity.getTableName()) },
				DELETE, null, null, null, NoKeyGenerator.INSTANCE, null, null);
	}

	private void addDeleteByIdStatement() {

		SQL sql = new SQL(persistentEntity, dialect, mappingContext) {
			{
				DELETE_FROM(persistentEntity.getTableName() + " " + dialect.quote(persistentEntity.getEntityName()));
				ID_CALUSE();// process id cause
			}
		};
		addStatement("_deleteById", new String[] { sql.toString() }, DELETE, persistentEntity.getIdClass(), null, null,
				NoKeyGenerator.INSTANCE, null, null);
	}

	private void addFindAllStatement() {

		SQL sql = new SQL(persistentEntity, dialect, mappingContext) {
			{
				SELECT_WITH_COLUMNS(true);
				FROM_WITH_LEFT_OUTER_JOIN(true);

			}
		};
		addStatement("_findAll",
				new String[] { "<script>", sql.toString(), FIND_CONDITION_SQL(true), SORT_SQL(true), "</script>" }, DELETE,
				Map.class, null, domainClass, NoKeyGenerator.INSTANCE, null, null);

	}

	private void addFindByPagerStatement(boolean complex) {
		String sql = new SQL(persistentEntity, dialect, mappingContext) {
			{
				SELECT_WITH_COLUMNS(complex);
				FROM_WITH_LEFT_OUTER_JOIN(complex);
			}
		}.toString();
		sql += FIND_CONDITION_SQL(complex);

		sql = dialect.getLimitHandler().processSql(sql, null);

		addStatement(complex ? "_findByPager" : "_findBasicByPager", new String[] { "<script>", sql, "</script>" }, SELECT,
				Map.class, null, domainClass, NoKeyGenerator.INSTANCE, null, null);
	}

	private void addCountByConditionStatement(boolean complex) {
		String sql = new SQL(persistentEntity, dialect, mappingContext) {
			{
				SELECT("count(*)");
				FROM_WITH_LEFT_OUTER_JOIN(complex);
			}
		}.toString();
		sql += FIND_CONDITION_SQL(complex);
		addStatement(complex ? "_countByCondition" : "_countBasicByCondition",
				new String[] { "<script>", sql, "</script>" }, SELECT, Map.class, null, long.class, NoKeyGenerator.INSTANCE,
				null, null);
	}

	private String FIND_CONDITION_SQL(boolean complex) {
		final StringBuilder builder = new StringBuilder();
		builder.append("<if test=\"_condition != null\">");
		builder.append("<trim prefix=\" where \" prefixOverrides=\"and |or \">");
		builder.append(buildCondition());
		builder.append("</trim>");
		builder.append("</if>");
		// process findByIds
		if (persistentEntity.hasIdProperty()) {
			builder.append("<if test=\"_ids != null\">");
			builder.append("<trim prefix=\" where \" prefixOverrides=\"and |or \">");

			if (persistentEntity.hasCompositeIdProperty()) {
				// TODO () OR () MODE
				MyBatisPersistentEntity<?> compositeIdPersistentEntity = persistentEntity.getCompositeIdPersistentEntity();
				builder.append(
						"<foreach item=\"item\" index=\"index\" collection=\"_ids\" open=\"(\" separator=\"or\" close=\")\">");
				builder.append("<trim prefixOverrides=\"and \">");
				if (null != compositeIdPersistentEntity) {

					if (persistentEntity.isAnnotationPresent(IdClass.class)) {
						persistentEntity.doWithIdProperties(pp -> {
							MyBatisPersistentProperty idProperty = persistentEntity.getIdProperty();
							builder.append(" and ").append(dialect.quote(persistentEntity.getEntityName())).append('.')
									.append(column(idProperty)).append("=").append("#{item." + idProperty.getName() + "}");
						});
					} else {
						compositeIdPersistentEntity.doWithProperties((SimplePropertyHandler) pp -> {
							MyBatisPersistentProperty idProperty = persistentEntity.getIdProperty();
							builder.append(" and ").append(dialect.quote(persistentEntity.getEntityName())).append('.')
									.append(column(idProperty)).append("=").append("#{item." + idProperty.getName() + "}");

						});
					}

				}
				builder.append("</trim>");
				builder.append("</foreach>");
			} else {

				MyBatisPersistentProperty idProperty = persistentEntity.getIdProperty();
				builder.append(dialect.quote(persistentEntity.getEntityName())).append('.').append(column(idProperty))
						.append(" in ");
				builder.append(
						"<foreach item=\"item\" index=\"index\" collection=\"_ids\" open=\"(\" separator=\",\" close=\")\">#{item}</foreach>");

			}
			builder.append("</trim>");
			builder.append("</if>");
		}
		return builder.toString();
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

	public static class SQL extends AbstractSQL<SQL> {

		protected MyBatisPersistentEntity<?> persistentEntity;
		protected Dialect dialect;
		protected MyBatisMappingContext mappingContext;

		public SQL(MyBatisPersistentEntity<?> persistentEntity, Dialect dialect, MyBatisMappingContext mappingContext) {
			this.persistentEntity = persistentEntity;
			this.dialect = dialect;
			this.mappingContext = mappingContext;
		}

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

	public String tableName() {
		return dialect.quote(persistentEntity.getTableName());
	}

	public String column(MyBatisPersistentProperty property) {
		return dialect.quote(property.getColumnName());
	}

	private String buildCondition() {
		final StringBuilder builder = new StringBuilder();

		return builder.toString();
	}

	private String statement(String statement) {
		return namespace + SqlSessionRepositorySupport.DOT + statement;
	}

	/**
	 * is ResultMap exists.
	 *
	 * @param name no need namespace.
	 */
	public boolean isResultMapExist(String name) {
		if (null == configuration) {
			return false;
		}
		return configuration.hasResultMap(statement(name));
	}

	/**
	 * Is Fragment exists.
	 */
	public boolean isFragmentExist(String fragment) {
		if (null == configuration) {
			return false;
		}
		return configuration.getSqlFragments().containsKey(statement(fragment));
	}

	/**
	 * is statement exists.
	 */
	public boolean isStatementExist(String id) {
		if (null == configuration) {
			return false;
		}
		return configuration.hasStatement(statement(id));
	}
}
