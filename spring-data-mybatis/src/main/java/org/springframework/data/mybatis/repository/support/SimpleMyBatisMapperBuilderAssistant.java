package org.springframework.data.mybatis.repository.support;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.mybatis.annotation.GenericGenerator;
import org.springframework.data.mybatis.annotation.GenericGenerators;
import org.springframework.data.mybatis.mapping.MyBatisMappingContext;
import org.springframework.data.mybatis.mapping.MyBatisPersistentEntity;
import org.springframework.data.mybatis.mapping.MyBatisPersistentProperty;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import javax.persistence.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.apache.ibatis.mapping.SqlCommandType.*;

/**
 * MyBatis Mapper Builder Assistant for SimpleMyBatisRepository.
 * 
 * @author Jarvis Song
 */
class SimpleMyBatisMapperBuilderAssistant extends AbstractMyBatisMapperBuilderAssistant {

	public static final String DEFAULT_SEQUENCE_NAME = "seq_spring_data_mybatis";

	private final RepositoryInformation repositoryInformation;

	private Map<String, SequenceGenerator> sequenceGeneratorCache = new HashMap<>();
	private Map<String, GenericGenerator> genericGeneratorCache = new HashMap<>();

	public SimpleMyBatisMapperBuilderAssistant(Configuration configuration, MyBatisMappingContext mappingContext,
			RepositoryInformation repositoryInformation) {

		super(configuration, mappingContext, repositoryInformation.getRepositoryInterface().getName(),
				repositoryInformation.getDomainType());
		this.repositoryInformation = repositoryInformation;

		prepareSequenceGenerator();
		prepareGenericGenerator();

	}

	@Override
	protected void doPrepare() {

		// personalise
		Class<?> repositoryInterface = repositoryInformation.getRepositoryInterface();
		Stream.of(repositoryInterface.getMethods())
				.map(method -> ClassUtils.getMostSpecificMethod(method, repositoryInterface))
				.filter(method -> repositoryInformation.isBaseClassMethod(method) && !method.isBridge() && !method.isDefault()
						&& !Modifier.isStatic(method.getModifiers()))
				.distinct().sorted(Comparator.comparing(Method::getName)).forEachOrdered(method -> {
					parseStatement(method);
				});

		// generic
		addInsertStatement();// _insert
		addUpdateStatement(true);// _updateIgnoreNull
		addUpdateStatement(false);// _update
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

		// last clear
		sequenceGeneratorCache = null;
		genericGeneratorCache = null;
	}

	private void parseStatement(Method method) {
		if (method.isAnnotationPresent(Select.class) || method.isAnnotationPresent(Insert.class)
				|| method.isAnnotationPresent(Update.class) || method.isAnnotationPresent(Delete.class)) {
			// TODO parse statement by method of repository interface.

		}
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
			keyColumn = new SQL().COLUMN(idProperty);
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
							keyGenerator = handleSelectKeyFromIdProperty(idProperty, "_insert", domainClass, false,
									generatedValue.generator());
						} else if ("sequence".equals(dialect.getNativeIdentifierGeneratorStrategy())) {
							keyGenerator = handleSelectKeyFromIdProperty(idProperty, "_insert", domainClass, true,
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
					keyGenerator = handleSelectKeyFromIdProperty(idProperty, "_insert", domainClass, false,
							generatedValue.generator());

				} else if (generatedValue.strategy() == GenerationType.SEQUENCE) {
					if (!dialect.supportsSequences()) {
						throw new MappingException(
								domainClass + " Id @GeneratedValue set stratety=SEQUENCE, but this database's dialect["
										+ dialect.getClass() + "] not support sequence.");
					}
					keyGenerator = handleSelectKeyFromIdProperty(idProperty, "_insert", domainClass, true,
							generatedValue.generator());
				} else if (generatedValue.strategy() == GenerationType.TABLE) {
					// TODO support table id
				}
			}
		}

		SQL sql = new SQL() {
			{
				INSERT_INTO(TABLE_NAME());
				persistentEntity.doWithProperties((SimplePropertyHandler) pp -> {
					MyBatisPersistentProperty property = (MyBatisPersistentProperty) pp;
					VALUES(COLUMN(property), VAR(property.getName(), property.getJdbcType()));
				});
				persistentEntity.doWithAssociations((SimpleAssociationHandler) ass -> {

					MyBatisPersistentProperty inverseProperty = (MyBatisPersistentProperty) ass.getInverse();
					MyBatisPersistentEntity<?> targetEntity = inverseProperty.getOwnerEntity().getMappingContext()
							.getPersistentEntity(inverseProperty.getActualType());

					// @Id + @Embeddable || @EmbeddedId
					if ((inverseProperty.isAnnotationPresent(Id.class) && targetEntity.isAnnotationPresent(Embeddable.class))
							|| inverseProperty.isAnnotationPresent(EmbeddedId.class)) {
						targetEntity.doWithProperties((SimplePropertyHandler) idp1 -> {
							MyBatisPersistentProperty idp = (MyBatisPersistentProperty) idp1;

							VALUES(COLUMN(idp), VAR(targetEntity.getName() + "." + idp.getName(), idp.getJdbcType()));
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

		addMappedStatement("_insert", sql.toStrings(), INSERT, domainClass, null, null, keyGenerator, keyProperty,
				keyColumn);
	}

	private void addUpdateStatement(boolean ignoreNull) {

		SQL ql = new SQL() {
			{
				UPDATE(TABLE_NAME());
			}
		};

		StringBuilder builder = new StringBuilder();
		builder.append(ql.toString());

		if (ignoreNull) {
			builder.append("<set>");
		} else {
			builder.append(" set ");
		}
		persistentEntity.doWithProperties((SimplePropertyHandler) pp -> {
			MyBatisPersistentProperty property = (MyBatisPersistentProperty) pp;
			if (property.isIdProperty()) {
				return;
			}
			if (property.isVersionProperty()) {
				builder.append(ql.COLUMN(property) + "=" + ql.COLUMN(property) + "+1,");
				return;
			}
			if (!ignoreNull) {
				builder.append(ql.COLUMN(property) + "=" + ql.VAR(property) + ",");
			} else {
				builder.append("<if test=\"" + property.getName() + " != null\">" + ql.COLUMN(property) + "=" + ql.VAR(property)
						+ "," + "</if>");
			}
		});

		if (builder.charAt(builder.length() - 1) == ',') {
			builder.deleteCharAt(builder.length() - 1);
		}

		if (ignoreNull) {
			builder.append("</set>");
		}
		SQL where = new SQL() {
			{
				UPDATE(TABLE_NAME());
				ID_CALUSE(false);
			}
		};
		builder.append(where.toString().replace(ql.toString(), ""));

		String[] sqls;
		if (ignoreNull) {
			sqls = new String[] { "<script>", builder.toString(), "</script>" };
		} else {
			sqls = new String[] { builder.toString() };
		}
		addMappedStatement(ignoreNull ? "_updateIgnoreNull" : "_update", sqls, UPDATE, domainClass);
	}

	private void addGetByIdStatement(boolean complex) {

		SQL sql = new SQL() {
			{
				SELECT_WITH_COLUMNS(complex);
				FROM_WITH_LEFT_OUTER_JOIN(complex);
				if (complex) {
					// TODO LEFT OUTER JOIN
				}
				ID_CALUSE();// process id cause
			}
		};
		addMappedStatement(complex ? "_getById" : "_getBasicById", sql.toStrings(), SELECT, persistentEntity.getIdClass(),
				domainClass);
	}

	private void addCountStatement() {
		SQL sql = new SQL() {
			{
				SELECT("count(*)");
				FROM_WITH_LEFT_OUTER_JOIN(false);
			}
		};
		addMappedStatement("_count", new String[] { sql.toString() }, SELECT, domainClass, long.class);
	}

	private void addDeleteAllStatement() {
		addMappedStatement("_deleteAll", new String[] { "truncate table " + new SQL().TABLE_NAME() }, DELETE);
	}

	private void addDeleteByIdStatement() {

		SQL sql = new SQL() {
			{
				DELETE_FROM(TABLE_NAME() + " " + ALIAS());
				ID_CALUSE();// process id cause
			}
		};
		addMappedStatement("_deleteById", sql.toStrings(), DELETE, persistentEntity.getIdClass());
	}

	private void addFindAllStatement() {

		SQL sql = new SQL() {
			{
				SELECT_WITH_COLUMNS(true);
				FROM_WITH_LEFT_OUTER_JOIN(true);

			}
		};
		addMappedStatement("_findAll",
				new String[] { "<script>", sql.toString(), sql.FIND_CONDITION_SQL(true), sql.SORT_SQL(true), "</script>" },
				DELETE, Map.class, domainClass);

	}

	private void addFindByPagerStatement(boolean complex) {
		SQL sql = new SQL() {
			{
				SELECT_WITH_COLUMNS(complex);
				FROM_WITH_LEFT_OUTER_JOIN(complex);
			}
		};

		addMappedStatement(complex ? "_findByPager" : "_findBasicByPager",
				new String[] { "<script>",
						dialect.getLimitHandler().processSql(sql.toString() + sql.FIND_CONDITION_SQL(complex), null), "</script>" },
				SELECT, Map.class, domainClass);
	}

	private void addCountByConditionStatement(boolean complex) {
		SQL sql = new SQL() {
			{
				SELECT("count(*)");
				FROM_WITH_LEFT_OUTER_JOIN(complex);
			}
		};
		addMappedStatement(complex ? "_countByCondition" : "_countBasicByCondition",
				new String[] { "<script>", sql.toString(), sql.FIND_CONDITION_SQL(complex), "</script>" }, SELECT, Map.class,
				long.class);
	}

	private KeyGenerator handleSelectKeyFromIdProperty(MyBatisPersistentProperty idProperty, String baseStatementId,
			Class<?> parameterTypeClass, boolean executeBefore, String generator) {
		SQL sql = new SQL();
		String id = baseStatementId + SelectKeyGenerator.SELECT_KEY_SUFFIX;
		Class<?> resultTypeClass = idProperty.getActualType();
		String keyProperty = idProperty.getName();
		String keyColumn = sql.COLUMN(idProperty);

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
			sqls = new String[] { dialect.getIdentityColumnSupport().getIdentitySelectString(sql.TABLE_NAME(),
					idProperty.getColumnName(), idProperty.getJdbcType().TYPE_CODE) };
		}

		addMappedStatement(id, sqls, SELECT, parameterTypeClass, null, resultTypeClass, NoKeyGenerator.INSTANCE,
				keyProperty, keyColumn);

		id = assistant.applyCurrentNamespace(id, false);

		MappedStatement keyStatement = configuration.getMappedStatement(id, false);
		SelectKeyGenerator answer = new SelectKeyGenerator(keyStatement, executeBefore);
		configuration.addKeyGenerator(id, answer);
		return answer;
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

	/**
	 * SQL Build.
	 */
	class SQL extends AbstractMyBatisMapperBuilderAssistant.SQL {

		public String DYNAMIC_CONDITION(boolean complex) {
			final StringBuilder builder = new StringBuilder();

			return builder.toString();
		}

		public String FIND_CONDITION_SQL(boolean complex) {
			final StringBuilder builder = new StringBuilder();
			builder.append("<if test=\"_condition != null\">");
			builder.append("<trim prefix=\" where \" prefixOverrides=\"and |or \">");
			builder.append(DYNAMIC_CONDITION(complex));
			builder.append("</trim>");
			builder.append("</if>");
			// process findByIds
			if (entity.hasIdProperty()) {
				builder.append("<if test=\"_ids != null\">");
				builder.append("<trim prefix=\" where \" prefixOverrides=\"and |or \">");

				if (entity.hasCompositeIdProperty()) {
					// TODO () OR () MODE
					MyBatisPersistentEntity<?> compositeIdPersistentEntity = entity.getCompositeIdPersistentEntity();
					builder.append(
							"<foreach item=\"item\" index=\"index\" collection=\"_ids\" open=\"(\" separator=\"or\" close=\")\">");
					builder.append("<trim prefixOverrides=\"and \">");
					if (null != compositeIdPersistentEntity) {

						if (entity.isAnnotationPresent(IdClass.class)) {
							entity.doWithIdProperties(pp -> {
								MyBatisPersistentProperty idProperty = entity.getIdProperty();
								builder.append(" and ").append(dialect.quote(entity.getEntityName())).append('.')
										.append(COLUMN(idProperty)).append("=").append(VAR(idProperty.getName()));
							});
						} else {
							compositeIdPersistentEntity.doWithProperties((SimplePropertyHandler) pp -> {
								MyBatisPersistentProperty idProperty = entity.getIdProperty();
								builder.append(" and ").append(ALIAS()).append('.').append(COLUMN(idProperty)).append("=")
										.append(VAR("item." + idProperty.getName()));
							});
						}
					}
					builder.append("</trim>");
					builder.append("</foreach>");
				} else {

					MyBatisPersistentProperty idProperty = entity.getIdProperty();
					builder.append(ALIAS()).append('.').append(COLUMN(idProperty)).append(" in ");
					builder.append(
							"<foreach item=\"item\" index=\"index\" collection=\"_ids\" open=\"(\" separator=\",\" close=\")\">#{item}</foreach>");
				}
				builder.append("</trim>");
				builder.append("</if>");
			}
			return builder.toString();
		}
	}

}
