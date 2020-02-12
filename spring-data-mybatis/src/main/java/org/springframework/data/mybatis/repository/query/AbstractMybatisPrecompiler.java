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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EmbeddedId;

import com.samskivert.mustache.Mustache;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.mybatis.dialect.internal.DatabaseMetaDataDialectResolutionInfoAdapter;
import org.springframework.data.mybatis.dialect.internal.StandardDialectResolver;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.mapping.MybatisPersistentEntity;
import org.springframework.data.mybatis.mapping.MybatisPersistentProperty;
import org.springframework.data.mybatis.mapping.model.Column;
import org.springframework.data.mybatis.repository.support.ResidentParameterName;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.util.StringUtils;

/**
 * Abstract mybatis precompiler.
 *
 * @author JARVIS SONG
 */
@Slf4j
abstract class AbstractMybatisPrecompiler implements MybatisPrecompiler {

	protected final MybatisMappingContext mappingContext;

	protected final Configuration configuration;

	protected final String namespace;

	protected final MybatisPersistentEntity<?> persistentEntity;

	protected final Dialect dialect;

	protected Class<?> repositoryInterface;

	protected EscapeCharacter escape;

	AbstractMybatisPrecompiler(MybatisMappingContext mappingContext, Configuration configuration,
			RepositoryInformation repositoryInformation) {

		this(mappingContext, configuration, repositoryInformation.getRepositoryInterface().getName(),
				repositoryInformation.getDomainType());

		this.repositoryInterface = repositoryInformation.getRepositoryInterface();
	}

	AbstractMybatisPrecompiler(MybatisMappingContext mappingContext, Configuration configuration, String namespace,
			Class<?> domainType) {
		this.mappingContext = mappingContext;
		this.configuration = configuration;
		this.namespace = namespace;
		this.persistentEntity = mappingContext.getRequiredPersistentEntity(domainType);
		this.dialect = StandardDialectResolver.INSTANCE.resolveDialect(
				new DatabaseMetaDataDialectResolutionInfoAdapter(configuration.getEnvironment().getDataSource()));

	}

	protected String render(String name, Object context) {

		String path = "org/springframework/data/mybatis/repository/query/template/" + name + ".xml";
		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(path);
		try (InputStreamReader source = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
			return Mustache.compiler().escapeHTML(false).compile(source).execute(context);
		}
		catch (IOException ex) {
			throw new MappingException("Could not render the statement: " + name, ex);
		}

	}

	@Override
	public void precompile() {
		try {
			String xml = this.doPrecompile();

			if (StringUtils.hasText(xml)) {

				xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"https://mybatis.org/dtd/mybatis-3-mapper.dtd\"> <mapper namespace=\""
						+ this.namespace + "\">" + xml + "</mapper>";
				if (log.isDebugEnabled()) {
					log.debug(xml);
				}
				InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
				XMLMapperBuilder mapperBuilder = new XMLMapperBuilder(inputStream, this.configuration,
						this.namespace.replace('.', '/') + this.getResourceSuffix(),
						this.configuration.getSqlFragments());
				mapperBuilder.parse();

			}
		}
		finally {
			// clean up
		}
	}

	protected abstract String doPrecompile();

	protected String getResourceSuffix() {
		return ".precompile";
	}

	protected String getTableName() {
		return this.persistentEntity.getTable().getFullName();
	}

	protected Map<String, Column> mappingPropertyToColumn() {
		Map<String, Column> map = new LinkedHashMap<>();

		this.persistentEntity.doWithProperties((PropertyHandler<MybatisPersistentProperty>) pp -> {
			if (pp.isAnnotationPresent(EmbeddedId.class) || pp.isEmbeddable()) {
				MybatisPersistentEntity<?> embeddedEntity = this.mappingContext
						.getRequiredPersistentEntity(pp.getActualType());
				embeddedEntity.doWithProperties((PropertyHandler<MybatisPersistentProperty>) epp -> {
					map.put(String.format("%s.%s", pp.getName(), epp.getName()), epp.getColumn());
				});
				return;
			}
			map.put(pp.getName(), pp.getColumn());
		});

		return map;
	}

	protected String variableSegment(String propertyName, Column column) {
		return String.format("#{%s%s%s}", propertyName, //
				(null != column.getJdbcType()) ? (",jdbcType=" + column.getJdbcType().name()) : "", //
				(null != column.getTypeHandler()) ? (",typeHandler=" + column.getTypeHandler().getName()) : "");
	}

	protected String resultMapSegment(boolean isId, String propertyName, Column column) {
		return String.format("<%s property=\"%s\" column=\"%s\" %s%s%s />", isId ? "id" : "result", propertyName, //
				column.getName().render(this.dialect), //
				(null != column.getJavaType()) ? (" javaType=\"" + column.getJavaType().getName() + "\"") : "", //
				(null != column.getJdbcType()) ? (" jdbcType=\"" + column.getJdbcType().name() + "\"") : "", //
				(null != column.getTypeHandler()) ? (" typeHandler=\"" + column.getTypeHandler().getName() + "\"")
						: "");
	}

	protected String testClause(String propertyName, boolean and, boolean not) {
		String[] parts = propertyName.split("\\.");
		String[] conditions = new String[parts.length];
		String prev = null;
		for (int i = 0; i < parts.length; i++) {
			conditions[i] = ((null != prev) ? (prev + ".") : "") + parts[i];
			prev = conditions[i];
		}
		String test = Stream.of(conditions).map(c -> c + (not ? " !" : " =") + "= null")
				.collect(Collectors.joining(and ? " and " : " or "));
		return test;
	}

	protected String testNotNullSegment(String propertyName, String content) {

		String test = this.testClause(propertyName, true, true);
		return String.format("<if test=\"%s\">%s</if>", test, content);
	}

	protected List<MybatisPersistentProperty> findProperties(MybatisPersistentEntity<?> entity) {
		List<MybatisPersistentProperty> properties = new ArrayList<>();
		entity.doWithProperties((PropertyHandler<MybatisPersistentProperty>) properties::add);
		return properties;
	}

	protected List<MybatisPersistentProperty> findProperties() {
		return this.findProperties(this.persistentEntity);
	}

	protected SqlCommandType extractSqlCommandType(String queryString) {
		if (StringUtils.isEmpty(queryString)) {
			return SqlCommandType.UNKNOWN;
		}

		queryString = queryString.trim().toLowerCase();
		if (queryString.startsWith("insert")) {
			return SqlCommandType.INSERT;
		}
		if (queryString.startsWith("update")) {
			return SqlCommandType.UPDATE;
		}
		if (queryString.startsWith("delete")) {
			return SqlCommandType.DELETE;
		}
		if (queryString.startsWith("select")) {
			return SqlCommandType.SELECT;
		}

		return SqlCommandType.UNKNOWN;
	}

	protected boolean canUpperCase(MybatisPersistentProperty property) {
		return String.class.equals(property.getType());
	}

	protected String buildQueryByConditionOperator(Part.Type type) {
		switch (type) {
		case BETWEEN:
			return " between ";
		case SIMPLE_PROPERTY:
			return "=";
		case NEGATING_SIMPLE_PROPERTY:
			return "<![CDATA[<>]]>";
		case LESS_THAN:
		case BEFORE:
			return "<![CDATA[<]]>";
		case LESS_THAN_EQUAL:
			return "<![CDATA[<=]]>";
		case GREATER_THAN:
		case AFTER:
			return "<![CDATA[>]]>";
		case GREATER_THAN_EQUAL:
			return ">=";
		case NOT_LIKE:
		case NOT_CONTAINING:
			return " not like ";
		case LIKE:
		case STARTING_WITH:
		case ENDING_WITH:
		case CONTAINING:
			return " like ";
		case IN:
			return " in ";
		case NOT_IN:
			return " not in ";
		}

		return "";
	}

	protected String buildQueryByConditionLeftSegment(String column, Part.IgnoreCaseType ignoreCaseType,
			MybatisPersistentProperty property) {
		switch (ignoreCaseType) {

		case ALWAYS:
			return this.dialect.getLowercaseFunction() + "(" + column + ")";
		case WHEN_POSSIBLE:
			if ((null != property) && this.canUpperCase(property)) {
				return this.dialect.getLowercaseFunction() + "(" + column + ")";
			}
			return column;
		case NEVER:
		default:
			return column;
		}
	}

	protected String buildQueryByConditionRightSegment(Part.Type type, Part.IgnoreCaseType ignoreCaseType,
			String[] properties) {
		switch (type) {
		case BETWEEN:
			return String.format("#{%s} and #{%s}", properties[0], properties[1]);
		case IS_NOT_NULL:
			return " is not null";
		case IS_NULL:
			return " is null";
		case STARTING_WITH:
			return this.buildLikeRightSegment(properties[0], false, true, ignoreCaseType);
		case ENDING_WITH:
			return this.buildLikeRightSegment(properties[0], true, false, ignoreCaseType);
		case NOT_CONTAINING:
		case CONTAINING:
			return this.buildLikeRightSegment(properties[0], true, true, ignoreCaseType);
		case NOT_IN:
		case IN:
			return String.format(
					"<foreach item=\"__item\" index=\"__index\" collection=\"%s\" open=\"(\" separator=\",\" close=\")\">#{__item}</foreach>",
					properties[0]);
		case TRUE:
			return " = true";
		case FALSE:
			return " = false";
		default:
			if (ignoreCaseType == Part.IgnoreCaseType.ALWAYS || ignoreCaseType == Part.IgnoreCaseType.WHEN_POSSIBLE) {
				return String.format("%s(#{%s})", this.dialect.getLowercaseFunction(), properties[0]);
			}
			return String.format("#{%s}", properties[0]);

		}
	}

	protected String buildLikeRightSegment(String property, boolean left, boolean right,
			Part.IgnoreCaseType ignoreCaseType) {
		return String.format("<bind name=\"__bind_%s\" value=\"%s%s%s\" />%s", property, (left ? "'%' + " : ""),
				property, (right ? " + '%'" : ""),
				(((ignoreCaseType == Part.IgnoreCaseType.ALWAYS)
						|| (ignoreCaseType == Part.IgnoreCaseType.WHEN_POSSIBLE))
								? String.format("%s(#{__bind_%s})", this.dialect.getLowercaseFunction(), property)
								: String.format("#{__bind_%s}", property)));
	}

	protected String buildStandardOrderBySegment() {
		String mapping = this.mappingPropertyToColumn().entrySet().stream()
				.map(entry -> String.format("&apos;%s&apos;:&apos;%s&apos;", entry.getKey(),
						entry.getValue().getName().render(this.dialect)))
				.collect(Collectors.joining(","));
		String bind = String.format("<bind name=\"__columnsMap\" value='#{%s}'/>", mapping);
		String sql = String.format("order by "
				+ " <foreach collection=\"%s\" item=\"item\" index=\"idx\" open=\"\" close=\"\" separator=\",\">"
				+ "<if test=\"item.ignoreCase\">%s(</if>" + "${__columnsMap[item.property]}"
				+ "<if test=\"item.ignoreCase\">)</if> " + "${item.direction.name().toLowerCase()}" + "</foreach>",
				ResidentParameterName.SORT, this.dialect.getLowercaseFunction());
		return String.format("<if test=\"%s != null\">%s %s</if>", ResidentParameterName.SORT, bind, sql);
	}

}
