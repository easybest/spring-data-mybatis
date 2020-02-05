/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mybatis.repository.query;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EmbeddedId;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.SequenceGenerator;
import javax.persistence.SequenceGenerators;

import org.apache.ibatis.session.Configuration;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.mapping.MybatisPersistentEntity;
import org.springframework.data.mybatis.mapping.MybatisPersistentProperty;
import org.springframework.data.mybatis.mapping.model.Column;
import org.springframework.data.mybatis.repository.support.ResidentStatementName;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.util.StringUtils;

/**
 * Simple mybatis precompiler.
 *
 * @author JARVIS SONG
 */
class SimpleMybatisPrecompiler extends AbstractMybatisPrecompiler {

	static final String DEFAULT_SEQUENCE_NAME = "seq_spring_data_mybatis";

	SimpleMybatisPrecompiler(MybatisMappingContext mappingContext, Configuration configuration,
			RepositoryInformation repositoryInformation) {
		super(mappingContext, configuration, repositoryInformation);
	}

	@Override
	protected String doPrecompile() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.addResultMap());
		builder.append(this.addInsertStatement(true));
		builder.append(this.addInsertStatement(false));
		builder.append(this.addGetByIdStatement());
		builder.append(this.addFindStatement(true));
		builder.append(this.addFindStatement(false));
		return builder.toString();
	}

	private String addResultMap() {
		if (this.configuration.hasResultMap(this.namespace + '.' + ResidentStatementName.RESULT_MAP)) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		StringBuilder associationBuilder = new StringBuilder();
		this.persistentEntity.doWithProperties((PropertyHandler<MybatisPersistentProperty>) pp -> {
			if (pp.isAnnotationPresent(EmbeddedId.class) || pp.isEmbeddable()) {
				associationBuilder.append(String.format("<association property=\"%s\">", pp.getName()));
				MybatisPersistentEntity<?> embeddedEntity = this.mappingContext
						.getRequiredPersistentEntity(pp.getActualType());
				embeddedEntity.doWithProperties((PropertyHandler<MybatisPersistentProperty>) epp -> associationBuilder
						.append(this.resultMapSegment(epp.isIdProperty(), epp.getName(), epp.getColumn())));
				associationBuilder.append("</association>");
				return;
			}
			builder.append(this.resultMapSegment(pp.isIdProperty(), pp.getName(), pp.getColumn()));
		});

		builder.append(associationBuilder);
		return String.format("<resultMap id=\"%s\" type=\"%s\">%s</resultMap>", ResidentStatementName.RESULT_MAP,
				this.persistentEntity.getType().getName(), builder.toString());

	}

	private String addInsertStatement(boolean selective) {
		if (this.configuration.hasStatement(this.namespace + '.'
				+ (selective ? ResidentStatementName.INSERT_SELECTIVE : ResidentStatementName.INSERT))) {
			return "";
		}
		StringBuilder builder = new StringBuilder();

		MybatisPersistentProperty idProperty = this.persistentEntity.getIdProperty();
		String keyProperty = "";
		String keyColumn = "";
		boolean useGeneratedKeys = false;
		if (!this.persistentEntity.hasCompositeId() && null != idProperty) {
			keyProperty = idProperty.getName();
			keyColumn = idProperty.getColumn().getName().getText();
			if (idProperty.isAnnotationPresent(GeneratedValue.class)) {
				useGeneratedKeys = true;
				builder.append(this.buildKeyGenerator(idProperty));
			}
		}
		builder.append(selective ? this.buildInsertSelectiveSQL() : this.buildInsertSQL());
		return String.format(
				"<insert id=\"%s\" parameterType=\"%s\" keyProperty=\"%s\" keyColumn=\"%s\" useGeneratedKeys=\"%b\">%s</insert>",
				(selective ? ResidentStatementName.INSERT_SELECTIVE : ResidentStatementName.INSERT),
				this.persistentEntity.getType().getName(), keyProperty, keyColumn, useGeneratedKeys,
				builder.toString());

	}

	private String buildInsertSQL() {
		Map<String, Column> propertyToColumn = this.mappingPropertyToColumn();
		String columns = propertyToColumn.keySet().stream()
				.map(k -> propertyToColumn.get(k).getName().render(this.dialect)).collect(Collectors.joining(","));
		String values = propertyToColumn.keySet().stream().map(k -> this.variableSegment(k, propertyToColumn.get(k)))
				.collect(Collectors.joining(","));
		String sql = String.format("insert into %s (%s) values (%s)", this.getTableName(), columns, values);
		return sql;
	}

	private String buildInsertSelectiveSQL() {
		Map<String, Column> propertyToColumn = this.mappingPropertyToColumn();
		String columns = propertyToColumn.keySet().stream()
				.map(k -> this.testNotNullSegment(k, propertyToColumn.get(k).getName().render(this.dialect) + ","))
				.collect(Collectors.joining());
		String values = propertyToColumn.keySet().stream()
				.map(k -> this.testNotNullSegment(k, this.variableSegment(k, propertyToColumn.get(k)) + ","))
				.collect(Collectors.joining());
		String sql = String.format("insert into %s " + //
				"<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\">%s</trim> " + //
				"<trim prefix=\"values(\" suffix=\")\" suffixOverrides=\",\">%s</trim>", //
				this.getTableName(), columns, values);
		return sql;
	}

	private String buildKeyGenerator(MybatisPersistentProperty idProperty) {
		boolean executeBefore;
		String sql;
		GeneratedValue gv = idProperty.getRequiredAnnotation(GeneratedValue.class);
		if (gv.strategy() == GenerationType.IDENTITY || (gv.strategy() == GenerationType.AUTO
				&& "identity".equals(this.dialect.getNativeIdentifierGeneratorStrategy()))) {
			// identity
			sql = this.dialect.getIdentityColumnSupport().getIdentitySelectString(this.getTableName(),
					idProperty.getColumn().getName().getCanonicalName(),
					idProperty.getColumn().getJdbcType().TYPE_CODE);
			executeBefore = false;
		}
		else if (gv.strategy() == GenerationType.SEQUENCE || (gv.strategy() == GenerationType.AUTO
				&& "sequence".equals(this.dialect.getNativeIdentifierGeneratorStrategy()))) {
			String sequenceName = DEFAULT_SEQUENCE_NAME;
			if (StringUtils.hasText(gv.generator())) {
				// search sequence generator
				Map<String, String> sequenceGenerators = new HashMap<>();
				if (this.persistentEntity.isAnnotationPresent(SequenceGenerators.class)) {
					sequenceGenerators.putAll(
							Stream.of(this.persistentEntity.getRequiredAnnotation(SequenceGenerators.class).value())
									.filter(sg -> StringUtils.hasText(sg.sequenceName()))
									.collect(Collectors.toMap(sg -> sg.name(), sg -> sg.sequenceName())));
				}
				if (this.persistentEntity.isAnnotationPresent(SequenceGenerator.class)) {
					SequenceGenerator sg = this.persistentEntity.getRequiredAnnotation(SequenceGenerator.class);
					if (StringUtils.hasText(sg.sequenceName())) {
						sequenceGenerators.put(sg.name(), sg.sequenceName());
					}
				}
				if (idProperty.isAnnotationPresent(SequenceGenerators.class)) {
					sequenceGenerators
							.putAll(Stream.of(idProperty.getRequiredAnnotation(SequenceGenerators.class).value())
									.filter((sg) -> StringUtils.hasText(sg.sequenceName()))
									.collect(Collectors.toMap((sg) -> sg.name(), (sg) -> sg.sequenceName())));
				}
				if (idProperty.isAnnotationPresent(SequenceGenerator.class)) {
					SequenceGenerator sg = idProperty.getRequiredAnnotation(SequenceGenerator.class);
					if (StringUtils.hasText(sg.sequenceName())) {
						sequenceGenerators.put(sg.name(), sg.sequenceName());
					}
				}
				String sn = sequenceGenerators.get(gv.generator());
				if (StringUtils.hasText(sn)) {
					sequenceName = sn;
				}
			}
			sql = this.dialect.getSequenceNextValString(sequenceName);
			executeBefore = true;
		}
		else {
			throw new UnsupportedOperationException("unsupported generated value id strategy: " + gv.strategy());
		}
		return String.format(
				"<selectKey keyProperty=\"%s\" keyColumn=\"%s\" order=\"%s\" resultType=\"%s\">%s</selectKey>",
				idProperty.getName(), idProperty.getColumn().getName().getText(), executeBefore ? "BEFORE" : "AFTER",
				idProperty.getType().getName(), sql);
	}

	private String addGetByIdStatement() {
		if (this.configuration.hasStatement(this.namespace + '.' + ResidentStatementName.GET_BY_ID, false)) {
			return "";
		}
		if (null == this.persistentEntity.getIdClass()) {
			return "";
		}
		String sql = String.format("select * from %s where %s", this.getTableName(), this.buildByIdQueryCondition());
		return String.format("<select id=\"%s\" parameterType=\"%s\" resultMap=\"%s\">%s</select>",
				ResidentStatementName.GET_BY_ID, this.persistentEntity.getIdClass().getName(),
				ResidentStatementName.RESULT_MAP, sql);
	}

	private String buildByIdQueryCondition() {

		MybatisPersistentProperty idProperty = this.persistentEntity.getRequiredIdProperty();
		if (idProperty.isAnnotationPresent(EmbeddedId.class)) {
			MybatisPersistentEntity<?> embeddedEntity = this.mappingContext
					.getRequiredPersistentEntity(idProperty.getActualType());
			return this
					.findProperties(embeddedEntity).stream().map(epp -> String.format("%s = %s.%s",
							epp.getColumn().getName().render(this.dialect), idProperty.getName(), epp.getName()))
					.collect(Collectors.joining(" and "));
		}

		return this
				.findProperties().stream().filter(PersistentProperty::isIdProperty).map(p -> String.format("%s = %s",
						p.getColumn().getName().render(this.dialect), this.variableSegment(p.getName(), p.getColumn())))
				.collect(Collectors.joining(" and "));
	}

	private String addFindStatement(boolean pageable) {
		if (this.configuration.hasStatement(
				this.namespace + '.' + ((pageable) ? ResidentStatementName.FIND_BY_PAGER : ResidentStatementName.FIND),
				false)) {
			return "";
		}
		String conditions = String.format("<where>%s%s</where> %s", this.buildByIdsQueryCondition(),
				this.buildByConditionQueryCondition(), this.buildStandardOrderBySegment());
		String sql = String.format("select * from %s %s", this.getTableName(), conditions);
		return String.format("<select id=\"%s\" resultMap=\"%s\">%s</select>",
				pageable ? ResidentStatementName.FIND_BY_PAGER : ResidentStatementName.FIND,
				ResidentStatementName.RESULT_MAP,
				pageable ? this.dialect.getLimitHandler().processSql(sql, null) : sql);
	}

	private String buildByConditionQueryCondition() {
		return String.format("<if test=\"__condition != null\"><trim prefixOverrides=\"and |or \"></trim></if>");
	}

	private String buildStandardOrderBySegment() {
		String mapping = this.mappingPropertyToColumn().entrySet().stream()
				.map(entry -> String.format("&apos;%s&apos;:&apos;%s&apos;", entry.getKey(),
						entry.getValue().getName().render(this.dialect)))
				.collect(Collectors.joining(","));
		String bind = String.format("<bind name=\"__columnsMap\" value='#{%s}'/>", mapping);
		String sql = String.format("order by "
				+ " <foreach collection=\"__sort\" item=\"item\" index=\"idx\" open=\"\" close=\"\" separator=\",\">"
				+ "<if test=\"item.ignoreCase\">%s(</if>" + "${__columnsMap[item.property]}"
				+ "<if test=\"item.ignoreCase\">)</if> " + "${item.direction.name().toLowerCase()}" + "</foreach>",
				this.dialect.getLowercaseFunction());
		return String.format("<if test=\"__sort != null\">%s %s</if>", bind, sql);
	}

	private String buildByIdsQueryCondition() {
		if (null == this.persistentEntity.getIdClass()) {
			return "";
		}

		String sql;
		MybatisPersistentProperty idProperty = this.persistentEntity.getRequiredIdProperty();
		if (idProperty.isAnnotationPresent(EmbeddedId.class)) {
			String conditions = this
					.findProperties(this.mappingContext.getRequiredPersistentEntity(idProperty.getActualType()))
					.stream().map(epp -> String.format("%s = #{item.%s}",
							epp.getColumn().getName().render(this.dialect), epp.getName()))
					.collect(Collectors.joining(" and "));
			sql = String.format(
					"<foreach collection=\"__ids\" item=\"item\" index=\"index\" open=\"(\" separator=\") or (\" close=\")\">%s</foreach>",
					conditions);
		}
		else {
			sql = String.format(
					"%s in <foreach collection=\"__ids\" item=\"item\" index=\"index\" open=\"(\" separator=\",\" close=\")\">#{item}</foreach>",
					idProperty.getColumn().getName().render(this.dialect));
		}
		return String.format("<if test=\"__ids != null\"><trim prefixOverrides=\"and |or \"> and %s</trim></if>", sql);
	}

}
