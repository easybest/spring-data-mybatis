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
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EmbeddedId;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;

import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.mybatis.dialect.internal.DatabaseMetaDataDialectResolutionInfoAdapter;
import org.springframework.data.mybatis.dialect.internal.StandardDialectResolver;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.mapping.MybatisPersistentEntity;
import org.springframework.data.mybatis.mapping.MybatisPersistentProperty;
import org.springframework.data.mybatis.mapping.model.Column;
import org.springframework.data.repository.core.RepositoryInformation;
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

	AbstractMybatisPrecompiler(MybatisMappingContext mappingContext, Configuration configuration,
			RepositoryInformation repositoryInformation) {
		this.mappingContext = mappingContext;
		this.configuration = configuration;
		this.namespace = repositoryInformation.getRepositoryInterface().getName();
		this.persistentEntity = mappingContext.getRequiredPersistentEntity(repositoryInformation.getDomainType());

		this.dialect = StandardDialectResolver.INSTANCE.resolveDialect(
				new DatabaseMetaDataDialectResolutionInfoAdapter(configuration.getEnvironment().getDataSource()));
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

	protected String testNotNullSegment(String propertyName, String content) {
		String[] parts = propertyName.split("\\.");
		String[] conditions = new String[parts.length];
		String prev = null;
		for (int i = 0; i < parts.length; i++) {
			conditions[i] = ((null != prev) ? (prev + ".") : "") + parts[i];
			prev = conditions[i];
		}
		String test = Stream.of(conditions).map(c -> c + " != null").collect(Collectors.joining(" and "));
		return String.format("<if test=\"%s\">%s</if>", test, content);
	}

}
