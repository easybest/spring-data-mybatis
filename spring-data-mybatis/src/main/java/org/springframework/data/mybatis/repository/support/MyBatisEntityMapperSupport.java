package org.springframework.data.mybatis.repository.support;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.mybatis.mapping.MyBatisMappingContext;
import org.springframework.data.mybatis.mapping.MyBatisPersistentEntity;
import org.springframework.data.mybatis.mapping.MyBatisPersistentProperty;
import org.springframework.data.mybatis.repository.dialect.Dialect;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * @author Jarvis Song
 */
@Slf4j
public class MyBatisEntityMapperSupport {

	private static final String MAPPER_BEGIN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">";
	private static final String MAPPER_END = "</mapper>";

	private final Configuration configuration;
	private final MyBatisMappingContext mappingContext;
	private final Dialect dialect;
	private final Class<?> domainClass;

	private final MyBatisPersistentEntity<?> persistentEntity;

	public MyBatisEntityMapperSupport(Configuration configuration, MyBatisMappingContext mappingContext, Dialect dialect,
			Class<?> domainType) {

		this.configuration = configuration;
		this.mappingContext = mappingContext;
		this.dialect = dialect;

		this.persistentEntity = mappingContext.getPersistentEntity(domainType);
		this.domainClass = domainType;
	}

	public void done() {

		if (null == persistentEntity) {
			log.warn("Could not find persistent entity for domain: " + domainClass + " from mapping context.");
			return;
		}

		parse(render());

	}

	private String render() {
		StringBuilder builder = new StringBuilder();
		builder.append(MAPPER_BEGIN);
		builder.append("<mapper namespace=\"" + domainClass.getName() + "\">");

		// insert
		if (!isStatementExist("_insert")) {
			builder.append(buildInsertSQL());
		}

		builder.append(MAPPER_END);
		return builder.toString();
	}

	private String tableName() {
		return dialect.wrap(persistentEntity.getTableName());
	}

	private String column(MyBatisPersistentProperty property) {
		return dialect.wrap(property.getColumnName());
	}

	private String buildInsertSQL() {

		StringBuilder builder = new StringBuilder();
		builder.append("<insert id=\"_insert\" parameterType=\"" + domainClass.getName() + "\" lang=\"XML\"");

		builder.append(">");
		builder.append("<![CDATA[");

		builder.append("insert into ").append(tableName()).append("(");

		persistentEntity.doWithProperties((SimplePropertyHandler) pp -> {
			MyBatisPersistentProperty property = (MyBatisPersistentProperty) pp;
			if (property.isIdProperty()) {
				// if is id property
				// TODO composite id

				return;
			}

			builder.append(column(property)).append(",");
		});

		if (builder.charAt(builder.length() - 1) == ',') {
			builder.deleteCharAt(builder.length() - 1);
		}

		builder.append(") values (");

		persistentEntity.doWithProperties((SimplePropertyHandler) pp -> {
			MyBatisPersistentProperty property = (MyBatisPersistentProperty) pp;
			if (property.isIdProperty()) {
				// if is id property
				// TODO composite id

				return;
			}

			builder.append("#{").append(property.getName()).append(",jdbcType=").append(property.getJdbcType());
			builder.append("},");

		});
		if (builder.charAt(builder.length() - 1) == ',') {
			builder.deleteCharAt(builder.length() - 1);
		}
		builder.append(")]]>");
		builder.append("</insert>");
		return builder.toString();
	}

	private void parse(String xml) {
		if (null == xml) {
			return;
		}
		String namespace = domainClass.getName();

		if (log.isDebugEnabled()) {
			log.debug(
					"\n******************* Auto Generate MyBatis Mapping XML (" + namespace + ") *******************\n" + xml);
		}
		InputStream inputStream = null;
		try {
			inputStream = new ByteArrayInputStream(xml.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// ignore
		}
		String resource = namespace + "_auto_generate.xml";
		try {
			XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(inputStream, configuration, resource,
					configuration.getSqlFragments(), namespace);
			xmlMapperBuilder.parse();
		} catch (Exception e) {
			throw new MappingException("create auto mapping error for " + namespace, e);
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	private String namespace() {
		return domainClass.getName();
	}

	private String statement(String statement) {
		return namespace() + SqlSessionRepositorySupport.DOT + statement;
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
