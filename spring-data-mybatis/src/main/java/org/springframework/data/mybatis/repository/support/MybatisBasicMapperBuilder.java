package org.springframework.data.mybatis.repository.support;

import static javax.persistence.GenerationType.AUTO;
import static javax.persistence.GenerationType.IDENTITY;
import static javax.persistence.GenerationType.SEQUENCE;
import static org.apache.ibatis.mapping.SqlCommandType.DELETE;
import static org.apache.ibatis.mapping.SqlCommandType.INSERT;
import static org.apache.ibatis.mapping.SqlCommandType.SELECT;
import static org.apache.ibatis.mapping.SqlCommandType.UPDATE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EmbeddedId;
import javax.persistence.GeneratedValue;
import javax.persistence.SequenceGenerator;
import javax.persistence.SequenceGenerators;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mybatis.annotation.Condition;
import org.springframework.data.mybatis.annotation.Conditions;
import org.springframework.data.mybatis.annotation.CreatedBy;
import org.springframework.data.mybatis.annotation.CreatedDate;
import org.springframework.data.mybatis.annotation.Snowflake;
import org.springframework.data.mybatis.id.SnowflakeKeyGenerator;
import org.springframework.data.mybatis.mapping.MybatisPersistentEntityImpl;
import org.springframework.data.mybatis.mapping.MybatisPersistentProperty;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.parser.Part.IgnoreCaseType;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.util.StringUtils;

import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;

public class MybatisBasicMapperBuilder extends MybatisMapperBuildAssistant {

	public static final String DEFAULT_SEQUENCE_NAME = "seq_spring_data_mybatis";

	public MybatisBasicMapperBuilder(Configuration configuration,
			RepositoryInformation repositoryInformation,
			PersistentEntity<?, ?> persistentEntity) {

		super(configuration, persistentEntity,
				repositoryInformation.getRepositoryInterface().getName());

	}

	@Override
	protected void doBuild() {
		addResultMap();

		addInsertStatement();
		addUpdateStatement(true, true);
		addUpdateStatement(false, true);
		addUpdateStatement(true, false);
		addUpdateStatement(false, false);
		addGetByIdStatement();
		addCountStatement();
		addCountAllStatement();
		addDeleteByIdStatement();
		addDeleteAllStatement();
		addFindStatement(true);
		addFindStatement(false);
	}

	private void addResultMap() {

		List<ResultMapping> resultMappings = new ArrayList<>();

		entity.doWithProperties((PropertyHandler<MybatisPersistentProperty>) p -> {

			if (p.isAnnotationPresent(EmbeddedId.class) || p.isEmbeddable()) {

				((MybatisPersistentEntityImpl) entity)
						.getRequiredPersistentEntity(p.getActualType()).doWithProperties(
								(PropertyHandler<MybatisPersistentProperty>) ep -> {

									resultMappings.add(
											assistant.buildResultMapping(ep.getType(),
													String.format("%s.%s", p.getName(),
															ep.getName()),
													ep.getColumnName(), ep.getType(),
													ep.getJdbcType(), null, null, null,
													null, ep.getSpecifiedTypeHandler(),
													p.isIdProperty()
															? Collections.singletonList(
																	ResultFlag.ID)
															: null));

								});

				return;
			}

			resultMappings.add(assistant.buildResultMapping(p.getType(), p.getName(),
					p.getColumnName(), p.getType(), p.getJdbcType(), null, null, null,
					null, p.getSpecifiedTypeHandler(),
					p.isIdProperty() ? Collections.singletonList(ResultFlag.ID) : null));

		});

		addResultMap(RESULT_MAP, entity.getType(), resultMappings);

	}

	private void addInsertStatement() {

		StringBuilder builder = new StringBuilder();

		KeyGenerator keyGenerator = NoKeyGenerator.INSTANCE;
		String keyProperty = null, keyColumn = null;

		builder.append("insert into ").append(entity.getTableName()).append(" (");

		MybatisPersistentProperty idProperty = entity.getIdProperty();

		if (null != idProperty) {

			keyProperty = idProperty.getName();
			keyColumn = idProperty.getColumnName();

			if (idProperty.isAnnotationPresent(Snowflake.class)) {
				keyGenerator = new SnowflakeKeyGenerator(keyProperty,
						((MybatisPersistentEntityImpl) this.entity).getMappingContext()
								.getSnowflake());

				configuration.addKeyGenerator(assistant.applyCurrentNamespace(
						"__insert" + SnowflakeKeyGenerator.SELECT_KEY_SUFFIX, false),
						keyGenerator);
			}
			else if (idProperty.isAnnotationPresent(GeneratedValue.class)) {
				GeneratedValue gv = idProperty
						.getRequiredAnnotation(GeneratedValue.class);
				String gid = "__insert" + SelectKeyGenerator.SELECT_KEY_SUFFIX;
				String[] sqls;
				boolean executeBefore;
				if (gv.strategy() == IDENTITY || (gv.strategy() == AUTO && "identity"
						.equals(dialect.getNativeIdentifierGeneratorStrategy()))) {
					// identity
					sqls = new String[] {
							dialect.getIdentityColumnSupport().getIdentitySelectString(
									entity.getTableName(), idProperty.getColumnName(),
									idProperty.getJdbcType().TYPE_CODE) };
					executeBefore = false;
				}
				else if (gv.strategy() == SEQUENCE || (gv.strategy() == AUTO && "sequence"
						.equals(dialect.getNativeIdentifierGeneratorStrategy()))) {
					// sequence
					String sequenceName = DEFAULT_SEQUENCE_NAME;

					if (StringUtils.hasText(gv.generator())) {
						// search sequence generator
						Map<String, String> sequenceGenerators = new HashMap<>();
						if (entity.isAnnotationPresent(SequenceGenerators.class)) {
							sequenceGenerators.putAll(Stream
									.of(entity.getRequiredAnnotation(
											SequenceGenerators.class).value())
									.filter(sg -> StringUtils.hasText(sg.sequenceName()))
									.collect(Collectors.toMap(sg -> sg.name(),
											sg -> sg.sequenceName())));
						}
						if (entity.isAnnotationPresent(SequenceGenerator.class)) {
							SequenceGenerator sg = entity
									.getRequiredAnnotation(SequenceGenerator.class);
							if (StringUtils.hasText(sg.sequenceName())) {
								sequenceGenerators.put(sg.name(), sg.sequenceName());
							}
						}

						if (idProperty.isAnnotationPresent(SequenceGenerators.class)) {
							sequenceGenerators.putAll(Stream
									.of(idProperty.getRequiredAnnotation(
											SequenceGenerators.class).value())
									.filter(sg -> StringUtils.hasText(sg.sequenceName()))
									.collect(Collectors.toMap(sg -> sg.name(),
											sg -> sg.sequenceName())));
						}
						if (idProperty.isAnnotationPresent(SequenceGenerator.class)) {
							SequenceGenerator sg = idProperty
									.getRequiredAnnotation(SequenceGenerator.class);
							if (StringUtils.hasText(sg.sequenceName())) {
								sequenceGenerators.put(sg.name(), sg.sequenceName());
							}
						}

						String sn = sequenceGenerators.get(gv.generator());
						if (StringUtils.hasText(sn)) {
							sequenceName = sn;
						}
					}

					sqls = new String[] {
							dialect.getSequenceNextValString(sequenceName) };
					executeBefore = true;
				}
				else {
					throw new UnsupportedOperationException(
							"unsupported generated value id strategy: " + gv.strategy());
				}
				addMappedStatement(gid, sqls, SELECT, entity.getType(), null,
						idProperty.getActualType(), NoKeyGenerator.INSTANCE,
						idProperty.getName(), idProperty.getColumnName());

				gid = assistant.applyCurrentNamespace(gid, false);

				MappedStatement keyStatement = configuration.getMappedStatement(gid,
						false);
				keyGenerator = new SelectKeyGenerator(keyStatement, executeBefore);
				configuration.addKeyGenerator(gid, keyGenerator);
			}
		}

		List<MybatisPersistentProperty> columns = findNormalColumns();

		builder.append(columns.stream().map(p -> {
			if (p.isAnnotationPresent(EmbeddedId.class) || p.isEmbeddable()) {
				return findNormalColumns(((MybatisPersistentEntityImpl) entity)
						.getRequiredPersistentEntity(p.getActualType())).stream()
								.map(ep -> ep.getColumnName())
								.collect(Collectors.joining(","));
			}
			return p.getColumnName();
		}).collect(Collectors.joining(",")));

		builder.append(") values (");

		builder.append(columns.stream().map(p -> {

			if (p.isAnnotationPresent(EmbeddedId.class) || p.isEmbeddable()) {
				return findNormalColumns(((MybatisPersistentEntityImpl) entity)
						.getRequiredPersistentEntity(p.getActualType()))
								.stream()
								.map(ep -> null != ep.getSpecifiedTypeHandler()
										? String.format(
												"#{%s.%s,jdbcType=%s,typeHandler=%s}",
												p.getName(), ep.getName(),
												ep.getJdbcType().name(),
												ep.getSpecifiedTypeHandler().getName())
										: String.format("#{%s.%s,jdbcType=%s}",
												p.getName(), ep.getName(),
												ep.getJdbcType().name()))
								.collect(Collectors.joining(","));
			}

			return null != p.getSpecifiedTypeHandler()
					? String.format("#{%s,jdbcType=%s,typeHandler=%s}", p.getName(),
							p.getJdbcType().name(), p.getSpecifiedTypeHandler().getName())
					: String.format("#{%s,jdbcType=%s}", p.getName(),
							p.getJdbcType().name());
		}

		).collect(Collectors.joining(",")));

		builder.append(")");

		addMappedStatement("__insert", new String[] { builder.toString() }, INSERT,
				entity.getType(), null, null, keyGenerator, keyProperty, keyColumn);

	}

	private void addUpdateStatement(boolean ignoreNull, boolean byId) {

		if (!entity.hasIdProperty()) {
			return;
		}

		StringBuilder builder = new StringBuilder();
		builder.append("update ").append(entity.getTableName()).append(" <set> ");

		builder.append(

				findNormalColumns().stream().filter(p -> !(p
						.isAnnotationPresent(CreatedDate.class)
						|| p.isAnnotationPresent(
								org.springframework.data.annotation.CreatedDate.class)
						|| p.isAnnotationPresent(CreatedBy.class)
						|| p.isAnnotationPresent(
								org.springframework.data.annotation.CreatedBy.class)))
						.map(p -> {

							if (p.isAnnotationPresent(EmbeddedId.class)
									|| p.isEmbeddable()) {
								return findNormalColumns(
										((MybatisPersistentEntityImpl) entity)
												.getRequiredPersistentEntity(
														p.getActualType())).stream()
																.map(ep -> {

																	StringBuilder sb = new StringBuilder();
																	if (ignoreNull) {
																		sb.append(
																				"<if test=\"");
																		if (byId) {
																			sb.append(
																					"__entity != null and ");
																			sb.append(
																					"__entity."
																							+ p.getName()
																							+ " != null and ");
																			sb.append(
																					"__entity."
																							+ p.getName()
																							+ '.'
																							+ ep.getName()
																							+ " != null");
																		}
																		else {
																			sb.append(p
																					.getName()
																					+ " != null and ");
																			sb.append(p
																					.getName()
																					+ '.'
																					+ ep.getName()
																					+ " != null");
																		}
																		sb.append("\">");
																	}

																	sb.append(ep
																			.getColumnName())
																			.append("=");
																	sb.append((null != ep
																			.getSpecifiedTypeHandler()
																					? String.format(
																							"#{%s.%s,jdbcType=%s,typeHandler=%s}",
																							byId ? ("__entity."
																									+ p.getName())
																									: p.getName(),
																							ep.getName(),
																							ep.getJdbcType()
																									.name(),
																							ep.getSpecifiedTypeHandler()
																									.getName())
																					: String.format(
																							"#{%s.%s,jdbcType=%s}",
																							byId ? ("__entity."
																									+ p.getName())
																									: p.getName(),
																							ep.getName(),
																							ep.getJdbcType()
																									.name())));
																	sb.append(",");
																	if (ignoreNull) {
																		sb.append(
																				"</if>");
																	}
																	return sb.toString();

																}).collect(Collectors
																		.joining(" "));
							}

							if (p.isVersionProperty()) {
								return p.getColumnName() + "=" + p.getColumnName()
										+ "+1,";
							}

							StringBuilder sb = new StringBuilder();
							if (ignoreNull) {

								if (ignoreNull) {
									sb.append("<if test=\"");
									if (byId) {
										sb.append("__entity != null and ");
										sb.append("__entity." + p.getName() + " != null");

									}
									else {
										sb.append(p.getName() + " != null");
									}
									sb.append("\">");
								}

							}

							sb.append(p.getColumnName()).append("=");
							sb.append((null != p.getSpecifiedTypeHandler()
									? String.format("#{%s,jdbcType=%s,typeHandler=%s}",
											byId ? ("__entity." + p.getName())
													: p.getName(),
											p.getJdbcType().name(),
											p.getSpecifiedTypeHandler().getName())
									: String.format("#{%s,jdbcType=%s}",
											byId ? ("__entity." + p.getName())
													: p.getName(),
											p.getJdbcType().name())));
							sb.append(",");
							if (ignoreNull) {
								sb.append("</if>");
							}
							return sb.toString();

						}).collect(Collectors.joining()));

		builder.append("</set> where ").append(buildIdCaluse(true, byId));

		String[] sqls = new String[] { "<script>", builder.toString(), "</script>" };
		addMappedStatement(
				ignoreNull
						? (byId ? "__update_by_id_ignore_null" : "__update_ignore_null")
						: (byId ? "__update_by_id" : "__update"),
				sqls, UPDATE, entity.getType());
	}

	private void addGetByIdStatement() {
		StringBuilder builder = new StringBuilder();
		builder.append("select * ");
		// builder.append(findNormalColumns().stream()
		// .map(p -> String.format("%s as %s", p.getColumnName(), p.getName()))
		// .collect(Collectors.joining(",")));
		builder.append(" from ").append(entity.getTableName()).append(" where ")
				.append(buildIdCaluse(false, false));
		addMappedStatement("__get_by_id", new String[] { builder.toString() }, SELECT,
				entity.getIdProperty().getType(), RESULT_MAP);
	}

	private void addCountStatement() {
		StringBuilder builder = new StringBuilder();
		builder.append("select count(*) from ").append(entity.getTableName()).append(" ");

		// find in
		if (entity.hasIdProperty()) {
			builder.append("<if test=\"__ids != null\">");
			builder.append("<trim prefix=\" where \" prefixOverrides=\"and |or \">");
			builder.append(entity.getIdProperty().getColumnName()).append(" in ");
			builder.append(
					"<foreach item=\"item\" index=\"index\" collection=\"__ids\" open=\"(\" separator=\",\" close=\")\">#{item}</foreach>");
			builder.append("</trim>");
			builder.append("</if>");
		}

		// find condition
		builder.append("<if test=\"__condition != null\">");
		builder.append("<trim prefix=\" where \" prefixOverrides=\"and |or \">");
		builder.append(buildCondition());
		builder.append("</trim>");
		builder.append("</if>");

		addMappedStatement("__count",
				new String[] { "<script>", builder.toString(), "</script>" }, SELECT,
				entity.getType(), long.class);
	}

	private void addCountAllStatement() {
		StringBuilder builder = new StringBuilder();
		builder.append("select count(*) from ").append(entity.getTableName());
		addMappedStatement("__count_all", new String[] { builder.toString() }, SELECT,
				entity.getType(), long.class);
	}

	private void addDeleteByIdStatement() {
		if (!entity.hasIdProperty()) {
			return;
		}
		StringBuilder builder = new StringBuilder();

		builder.append("delete from ").append(entity.getTableName()).append(" where ")
				.append(buildIdCaluse(false, false));
		addMappedStatement("__delete_by_id", new String[] { builder.toString() }, DELETE,
				entity.getIdProperty().getType());

	}

	private void addDeleteAllStatement() {
		addMappedStatement("__delete_all",
				new String[] { "delete from " + entity.getTableName() }, DELETE);
	}

	private void addFindStatement(boolean pageable) {

		StringBuilder builder = new StringBuilder();

		builder.append("select * ");
		// builder.append(findNormalColumns().stream()
		// .map(p -> String.format("%s as %s", p.getColumnName(), p.getName()))
		// .collect(Collectors.joining(",")));
		builder.append(" from ").append(entity.getTableName());

		// find in
		if (entity.hasIdProperty()) {
			builder.append("<if test=\"__ids != null\">");
			builder.append("<trim prefix=\" where \" prefixOverrides=\"and |or \">");
			builder.append(entity.getIdProperty().getColumnName()).append(" in ");
			builder.append(
					"<foreach item=\"item\" index=\"index\" collection=\"__ids\" open=\"(\" separator=\",\" close=\")\">#{item}</foreach>");
			builder.append("</trim>");
			builder.append("</if>");
		}

		// find condition
		builder.append("<if test=\"__condition != null\">");
		builder.append("<trim prefix=\" where \" prefixOverrides=\"and |or \">");
		builder.append(buildCondition());
		builder.append("</trim>");
		builder.append("</if>");
		// order by
		builder.append(buildStandardOrderBy());

		addMappedStatement(pageable ? "__find_by_pager" : "__find",
				new String[] { "<script>",
						pageable ? dialect.getLimitHandler().processSql(
								builder.toString(), null) : builder.toString(),
						"</script>" },
				DELETE, Map.class, RESULT_MAP);
	}

	private String buildCondition() {

		final StringBuilder builder = new StringBuilder();

		entity.doWithProperties((PropertyHandler<MybatisPersistentProperty>) p -> {

			Set<Condition> set = new HashSet<>();

			Conditions conditions = p.findAnnotation(Conditions.class);
			if (null != conditions && conditions.value().length > 0) {
				set.addAll(Stream.of(conditions.value()).collect(Collectors.toSet()));
			}
			Condition condition = p.findAnnotation(Condition.class);
			if (null != condition) {
				set.add(condition);
			}

			builder.append(set.stream().map(c -> {

				String[] properties = c.properties();
				if (null == properties || properties.length == 0) {
					properties = new String[] { p.getName() };
				}
				Type type = Type.valueOf(c.type().name());
				if (type.getNumberOfArguments() > 0
						&& type.getNumberOfArguments() != properties.length) {
					throw new MappingException("@Condition with type " + type + " needs "
							+ type.getNumberOfArguments() + " arguments, but only find "
							+ properties.length + " properties in this @Condition.");
				}

				StringBuilder sb = new StringBuilder();
				sb.append("<if test=\"");
				sb.append(Stream.of(properties).map(
						property -> String.format("__condition.%s != null", property))
						.collect(Collectors.joining(" and ")));

				sb.append("\">");

				sb.append(" and ")
						.append(queryConditionLeft(
								StringUtils.hasText(c.column()) ? c.column()
										: p.getColumnName(),
								IgnoreCaseType.valueOf(c.ignoreCaseType().name())))
						.append(calculateOperation(type));

				sb.append(queryConditionRight(type,
						IgnoreCaseType.valueOf(c.ignoreCaseType().name()),
						Stream.of(properties).map(p1 -> "__condition." + p1)
								.toArray(String[]::new)));

				sb.append("</if>");
				return sb;
			}).collect(Collectors.joining(" ")));

		});

		return builder.toString();
	}

	private String buildIdCaluse(boolean clearly, boolean byId) {

		if (!entity.hasIdProperty()) {
			return null;
		}

		MybatisPersistentProperty p = entity.getRequiredIdProperty();
		if (p.isAnnotationPresent(EmbeddedId.class)) {
			return findNormalColumns(((MybatisPersistentEntityImpl) this.entity)
					.getRequiredPersistentEntity(p.getActualType()))
							.stream()
							.map(ep -> ep.getColumnName() + " = "
									+ (null != ep.getSpecifiedTypeHandler()
											? String.format(
													"#{%s,jdbcType=%s,typeHandler=%s}",
													clearly ? ((byId ? "__id"
															: p.getName()) + '.'
															+ ep.getName())
															: ep.getName(),
													ep.getJdbcType().name(),
													ep.getSpecifiedTypeHandler()
															.getName())
											: String.format("#{%s,jdbcType=%s}", clearly
													? ((byId ? "__id" : p.getName()) + '.'
															+ ep.getName())
													: ep.getName(),
													ep.getJdbcType().name())))
							.collect(Collectors.joining(" and "));
		}

		return p.getColumnName() + " = "
				+ (null != p.getSpecifiedTypeHandler()
						? String.format("#{%s,jdbcType=%s,typeHandler=%s}",
								byId ? "__id" : p.getName(), p.getJdbcType().name(),
								p.getSpecifiedTypeHandler().getName())
						: String.format("#{%s,jdbcType=%s}", byId ? "__id" : p.getName(),
								p.getJdbcType().name()));
	}

}
