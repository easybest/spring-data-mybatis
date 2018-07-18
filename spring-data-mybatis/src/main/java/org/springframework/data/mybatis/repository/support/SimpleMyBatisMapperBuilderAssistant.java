package org.springframework.data.mybatis.repository.support;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.type.TypeHandler;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mybatis.annotation.GenericGenerator;
import org.springframework.data.mybatis.annotation.GenericGenerators;
import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.mybatis.mapping.CompositeIdColumn;
import org.springframework.data.mybatis.mapping.GeneratedValueIdColumn;
import org.springframework.data.mybatis.mapping.IdColumn;
import org.springframework.data.mybatis.mapping.MyBatisMappingContext;
import org.springframework.data.mybatis.mapping.MyBatisPersistentProperty;
import org.springframework.data.mybatis.mapping.Table;
import org.springframework.data.mybatis.mapping.ToOneJoinColumnAssociation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import javax.persistence.SequenceGenerator;
import javax.persistence.SequenceGenerators;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javax.persistence.GenerationType.*;
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

	public SimpleMyBatisMapperBuilderAssistant(SqlSessionTemplate sqlSessionTemplate,
			MyBatisMappingContext mappingContext, Dialect dialect, RepositoryInformation repositoryInformation) {

		super(sqlSessionTemplate, mappingContext, dialect, repositoryInformation.getRepositoryInterface().getName(),
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

		Table table = persistentEntity.getTable();
		if (null != table.getIdColumn() && table.getIdColumn() instanceof GeneratedValueIdColumn) {
			keyProperty = table.getIdColumn().getProperty().getName();
			keyColumn = table.getIdColumn().getActualName(dialect);
			keyGenerator = handleSelectKeyFromIdColumn((GeneratedValueIdColumn) table.getIdColumn(), "_insert");
		}

		StringBuilder builder = new StringBuilder();
		StringBuilder values = new StringBuilder();
		builder.append("insert into ").append(table.getFullName(dialect)).append("(");
		if (null != table.getIdColumn()) {
			if (table.getIdColumn() instanceof CompositeIdColumn) {
				builder.append(((CompositeIdColumn) table.getIdColumn()).getIdColumns().stream()
						.map(idColumn -> idColumn.getActualName(dialect) + ',').collect(Collectors.joining()));

				values.append(((CompositeIdColumn) table.getIdColumn()).getIdColumns().stream().map(idColumn -> {
					StringBuilder composite = new StringBuilder();
					composite.append("#{").append(idColumn.getAlias()).append(",jdbcType=").append(idColumn.getJdbcType());
					Class<? extends TypeHandler> specifiedTypeHandler = idColumn.getSpecifiedTypeHandler();
					if (null != specifiedTypeHandler) {
						composite.append(",typeHandler=").append(specifiedTypeHandler.getName());
					}
					composite.append("},");
					return composite.toString();
				}).collect(Collectors.joining()));

			} else {
				IdColumn idColumn = table.getIdColumn();
				builder.append(idColumn.getActualName(dialect)).append(',');
				values.append("#{").append(idColumn.getAlias()).append(",jdbcType=").append(idColumn.getJdbcType());
				Class<? extends TypeHandler> specifiedTypeHandler = idColumn.getSpecifiedTypeHandler();
				if (null != specifiedTypeHandler) {
					values.append(",typeHandler=").append(specifiedTypeHandler.getName());
				}
				values.append("},");
			}
		}

		builder.append(Stream.concat(table.getColumns().stream().map(column -> column.getActualName(dialect)),
				table.getAssociations().stream().map(association -> {
					if (association instanceof ToOneJoinColumnAssociation) {
						return Arrays.stream(((ToOneJoinColumnAssociation) association).getJoinColumns())
								.map(joinColumn -> joinColumn.getActualName(dialect)).collect(Collectors.joining(","));
					}
					return "";
				}).filter(StringUtils::hasText)).collect(Collectors.joining(",")));

		values.append(Stream.concat(table.getColumns().stream().map(column -> {
			StringBuilder columns = new StringBuilder();
			columns.append("#{").append(column.getAlias()).append(",jdbcType=").append(column.getJdbcType());
			Class<? extends TypeHandler> specifiedTypeHandler = column.getSpecifiedTypeHandler();
			if (null != specifiedTypeHandler) {
				columns.append(",typeHandler=").append(specifiedTypeHandler.getName());
			}
			columns.append("}");
			return columns.toString();
		}), table.getAssociations().stream().map(association -> {
			if (association instanceof ToOneJoinColumnAssociation) {
				return Arrays.stream(((ToOneJoinColumnAssociation) association).getJoinColumns())
						.map(joinColumn -> "#{" + joinColumn.getAlias() + ",jdbcType=" + joinColumn.getJdbcType() + "}")
						.collect(Collectors.joining(","));
			}
			return "";
		}).filter(StringUtils::hasText)).collect(Collectors.joining(",")));

		builder.append(") values(").append(values).append(')');

		// TODO ASS

		addMappedStatement("_insert", new String[] { builder.toString() }, INSERT, domainClass, null, null, keyGenerator,
				keyProperty, keyColumn);
	}

	private void addUpdateStatement(final boolean ignoreNull) {
		Table table = persistentEntity.getTable();

		String idCaluse = buildIdCaluse(false);
		if (StringUtils.isEmpty(idCaluse)) {
			return; // FIXME throw new exception?
		}

		StringBuilder builder = new StringBuilder();
		builder.append("update ").append(table.getFullName(dialect)).append(' ');

		builder.append("<set>");

		builder.append(table.getColumns().stream().map(column -> {
			StringBuilder columns = new StringBuilder();
			if (column.getProperty().isVersionProperty()) {
				columns.append(column.getActualName(dialect)).append('=').append(column.getActualName(dialect)).append("+1");
				return columns.toString();
			}
			if (ignoreNull) {
				columns.append("<if test=\"").append(column.getAlias()).append(" != null\">");
			}
			columns.append(column.getActualName(dialect)).append("=#{").append(column.getAlias()).append(",jdbcType=")
					.append(column.getJdbcType());
			Class<? extends TypeHandler> specifiedTypeHandler = column.getSpecifiedTypeHandler();
			if (null != specifiedTypeHandler) {
				columns.append(",typeHandler=").append(specifiedTypeHandler.getName());
			}
			columns.append("},");
			if (ignoreNull) {
				columns.append("</if>");
			}
			return columns.toString();
		}).collect(Collectors.joining()));

		builder.append(table.getAssociations().stream().map(association -> {
			if (association instanceof ToOneJoinColumnAssociation) {
				return Arrays.stream(((ToOneJoinColumnAssociation) association).getJoinColumns()).map(joinColumn -> {
					StringBuilder columns = new StringBuilder();

					columns.append("<choose>");
					columns.append("<when test=\"").append(((ToOneJoinColumnAssociation) association).getJoinProperty().getName())
							.append(" == null\">");
					if (!ignoreNull) {
						columns.append(joinColumn.getActualName(dialect)).append("=null,");
					}
					columns.append("</when>");
					columns.append("<otherwise>");

					if (ignoreNull) {
						columns.append("<if test=\"").append(joinColumn.getAlias()).append(" != null\">");
					}
					columns.append(joinColumn.getActualName(dialect)).append("=")
							.append("#{" + joinColumn.getAlias() + ",jdbcType=" + joinColumn.getJdbcType() + "},");
					if (ignoreNull) {
						columns.append("</if>");
					}

					columns.append("</otherwise>");
					columns.append("</choose>");

					return columns.toString();
				}).filter(StringUtils::hasText).collect(Collectors.joining());
			}
			return "";
		}).filter(StringUtils::hasText).collect(Collectors.joining()));

		builder.append("</set>");

		builder.append(" where ");
		builder.append(idCaluse);
		String[] sqls = new String[] { "<script>", builder.toString(), "</script>" };
		addMappedStatement(ignoreNull ? "_updateIgnoreNull" : "_update", sqls, UPDATE, domainClass);
	}

	private void addGetByIdStatement(boolean complex) {
		Table table = persistentEntity.getTable();
		if (null == table.getIdColumn()) {
			return;
		}
		StringBuilder builder = new StringBuilder();
		builder.append("select ");
		builder.append(buildStandardSelectColumns(complex));
		builder.append(" from ").append(buildStandardFrom(complex)).append(" where ");
		builder.append(buildIdCaluse(complex));

		addMappedStatement(complex ? "_getById" : "_getBasicById", new String[] { builder.toString() }, SELECT,
				table.getIdColumn().getProperty().getType(), domainClass);
	}

	private void addCountStatement() {
		StringBuilder builder = new StringBuilder();
		builder.append("select count(*) from ").append(buildStandardFrom(false));
		addMappedStatement("_count", new String[] { builder.toString() }, SELECT, domainClass, long.class);
	}

	private void addDeleteAllStatement() {
		addMappedStatement("_deleteAll", new String[] { "delete from " + persistentEntity.getTable().getFullName(dialect) },
				DELETE);
	}

	private void addDeleteByIdStatement() {
		Table table = persistentEntity.getTable();
		if (null == table.getIdColumn()) {
			return;
		}
		StringBuilder builder = new StringBuilder();
		builder.append("delete from ").append(table.getFullName(dialect)).append(" where ").append(buildIdCaluse(false));
		addMappedStatement("_deleteById", new String[] { builder.toString() }, DELETE,
				table.getIdColumn().getProperty().getType());
	}

	private void addFindAllStatement() {

		addMappedStatement("_findAll", new String[] { "<script>", buildFindSQL(true), "</script>" }, DELETE, Map.class,
				domainClass);
	}

	private String buildIdCaluse(boolean complex) {
		Table table = persistentEntity.getTable();
		if (null == table.getIdColumn()) {
			return "";
		}
		if (table.getIdColumn() instanceof CompositeIdColumn) {

			return ((CompositeIdColumn) table.getIdColumn()).getIdColumns().stream().map(idColumn -> {
				StringBuilder builder = new StringBuilder();
				if (complex) {
					builder.append(idColumn.getQuotedPrefix(dialect)).append('.');
				}
				builder.append(idColumn.getActualName(dialect)).append("=#{").append(idColumn.getAlias()).append(",jdbcType=")
						.append(idColumn.getJdbcType());
				Class<? extends TypeHandler> specifiedTypeHandler = idColumn.getSpecifiedTypeHandler();
				if (null != specifiedTypeHandler) {
					builder.append(",typeHandler=").append(specifiedTypeHandler.getName());
				}
				builder.append('}');
				return builder.toString();
			}).collect(Collectors.joining(" and "));

		} else {
			IdColumn idColumn = table.getIdColumn();
			StringBuilder builder = new StringBuilder();
			if (complex) {
				builder.append(idColumn.getQuotedPrefix(dialect)).append('.');
			}
			builder.append(idColumn.getActualName(dialect)).append("=#{").append(idColumn.getAlias()).append(",jdbcType=")
					.append(idColumn.getJdbcType());
			Class<? extends TypeHandler> specifiedTypeHandler = idColumn.getSpecifiedTypeHandler();
			if (null != specifiedTypeHandler) {
				builder.append(",typeHandler=").append(specifiedTypeHandler.getName());
			}
			builder.append('}');
			return builder.toString();
		}

	}

	private String buildFindSQL(boolean complex) {
		StringBuilder builder = new StringBuilder();
		builder.append("select ").append(buildStandardSelectColumns(complex)).append(" from ")
				.append(buildStandardFrom(complex)).append(buildConditionCaluse(complex)).append(buildStandardOrderBy(complex));
		return builder.toString();
	}

	private String buildConditionCaluse(boolean complex) {
		Table table = persistentEntity.getTable();
		StringBuilder builder = new StringBuilder();

		builder.append("<if test=\"_condition != null\">");
		builder.append("<trim prefix=\" where \" prefixOverrides=\"and |or \">");
		// builder.append(DYNAMIC_CONDITION(complex));
		builder.append("</trim>");
		builder.append("</if>");

		if (null != table.getIdColumn()) {
			builder.append("<if test=\"_ids != null\">");
			builder.append("<trim prefix=\" where \" prefixOverrides=\"and |or \">");
			if (table.getIdColumn() instanceof CompositeIdColumn) {
				builder.append(
						"<foreach item=\"item\" index=\"index\" collection=\"_ids\" open=\"(\" separator=\"or\" close=\")\">");
				builder.append("<trim prefixOverrides=\"and \">");

				builder.append(((CompositeIdColumn) table.getIdColumn()).getIdColumns().stream().map(idColumn -> {
					StringBuilder composite = new StringBuilder();
					if (complex) {
						composite.append(idColumn.getQuotedPrefix(dialect)).append('.');
					}
					composite.append(idColumn.getActualName(dialect)).append(" = ").append("#{item.").append(idColumn.getAlias())
							.append(",jdbcType=").append(idColumn.getJdbcType()).append("}");
					return composite.toString();
				}).collect(Collectors.joining(" and ")));

				builder.append("</trim>");
				builder.append("</foreach>");

			} else {
				if (complex) {
					builder.append(table.getIdColumn().getQuotedPrefix(dialect)).append('.');
				}
				builder.append(table.getIdColumn().getActualName(dialect)).append(" in ");
				builder.append(
						"<foreach item=\"item\" index=\"index\" collection=\"_ids\" open=\"(\" separator=\",\" close=\")\">#{item}</foreach>");
			}
			builder.append("</trim>");
			builder.append("</if>");
		}
		return builder.toString();
	}

	private void addFindByPagerStatement(boolean complex) {

		addMappedStatement(complex ? "_findByPager" : "_findBasicByPager",
				new String[] { "<script>", dialect.getLimitHandler().processSql(buildFindSQL(complex), null), "</script>" },
				SELECT, Map.class, domainClass);
	}

	private void addCountByConditionStatement(boolean complex) {
		StringBuilder builder = new StringBuilder();
		builder.append("select count(*) from ").append(buildStandardFrom(complex)).append(buildConditionCaluse(complex));
		addMappedStatement(complex ? "_countByCondition" : "_countBasicByCondition",
				new String[] { "<script>", builder.toString(), "</script>" }, SELECT, Map.class, long.class);
	}

	private KeyGenerator handleSelectKeyFromIdColumn(GeneratedValueIdColumn idColumn, String baseStatementId) {
		String id = baseStatementId + SelectKeyGenerator.SELECT_KEY_SUFFIX;
		String[] sqls;
		boolean executeBefore;
		if ((idColumn.getStrategy() == AUTO && "identity".equals(dialect.getNativeIdentifierGeneratorStrategy()))
				|| idColumn.getStrategy() == IDENTITY) {
			// before identity
			sqls = new String[] {
					dialect.getIdentityColumnSupport().getIdentitySelectString(idColumn.getTable().getFullName(dialect),
							idColumn.getActualName(dialect), idColumn.getJdbcType().TYPE_CODE) };
			executeBefore = false;
		} else if ((idColumn.getStrategy() == AUTO && "sequence".equals(dialect.getNativeIdentifierGeneratorStrategy()))
				|| idColumn.getStrategy() == SEQUENCE) {
			String sequenceName = DEFAULT_SEQUENCE_NAME;
			if (null != idColumn.getSequenceGenerator()
					&& StringUtils.hasText(idColumn.getSequenceGenerator().sequenceName())) {
				sequenceName = idColumn.getSequenceGenerator().sequenceName();
			}
			sqls = new String[] { dialect.getSequenceNextValString(sequenceName) };
			executeBefore = true;
		} else {
			throw new UnsupportedOperationException("unsupported generated value id strategy: " + idColumn.getStrategy());
		}

		addMappedStatement(id, sqls, SELECT, domainClass, null, idColumn.getProperty().getActualType(),
				NoKeyGenerator.INSTANCE, idColumn.getProperty().getName(), idColumn.getName());

		id = assistant.applyCurrentNamespace(id, false);

		MappedStatement keyStatement = sqlSessionTemplate.getConfiguration().getMappedStatement(id, false);
		SelectKeyGenerator answer = new SelectKeyGenerator(keyStatement, executeBefore);
		sqlSessionTemplate.getConfiguration().addKeyGenerator(id, answer);
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

}
