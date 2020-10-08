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
package org.springframework.data.mybatis.mapping.model;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.chrono.JapaneseDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.Temporal;

import org.apache.ibatis.type.EnumOrdinalTypeHandler;
import org.apache.ibatis.type.EnumTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeAliasRegistry;

import org.springframework.data.mybatis.annotation.TypeHandler;
import org.springframework.data.mybatis.mapping.MybatisPersistentProperty;
import org.springframework.data.mybatis.mapping.handler.DateUnixTimestampTypeHandler;
import org.springframework.data.mybatis.mapping.handler.UnixTimestampDateTypeHandler;
import org.springframework.util.StringUtils;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class Column implements Component, Cloneable {

	private static final long serialVersionUID = 6881902687485454726L;

	protected static Map<Class<?>, String> TYPE_ALIAS = new HashMap<>();

	private static final Map<Class<?>, JdbcType> JAVA_TYPE_MAPPING;
	static {
		TypeAliasRegistry typeAliasRegistry = new TypeAliasRegistry();
		Map<String, Class<?>> typeAliases = typeAliasRegistry.getTypeAliases();
		typeAliases.entrySet().stream().forEach(entry -> TYPE_ALIAS.put(entry.getValue(), entry.getKey()));

		Map<Class<?>, JdbcType> map = new HashMap<>();
		map.put(Boolean.class, JdbcType.BOOLEAN);
		map.put(boolean.class, JdbcType.BOOLEAN);
		map.put(Byte.class, JdbcType.BIT);
		map.put(byte.class, JdbcType.BIT);
		map.put(Short.class, JdbcType.SMALLINT);
		map.put(short.class, JdbcType.SMALLINT);
		map.put(Integer.class, JdbcType.INTEGER);
		map.put(int.class, JdbcType.INTEGER);
		map.put(Long.class, JdbcType.BIGINT);
		map.put(long.class, JdbcType.BIGINT);
		map.put(Float.class, JdbcType.FLOAT);
		map.put(float.class, JdbcType.FLOAT);
		map.put(Double.class, JdbcType.DOUBLE);
		map.put(double.class, JdbcType.DOUBLE);
		map.put(Character.class, JdbcType.VARCHAR);
		map.put(char.class, JdbcType.VARCHAR);
		map.put(Reader.class, JdbcType.CLOB);
		map.put(String.class, JdbcType.VARCHAR);
		map.put(BigInteger.class, JdbcType.BIGINT);
		map.put(BigDecimal.class, JdbcType.DECIMAL);
		map.put(InputStream.class, JdbcType.BLOB);
		map.put(Byte[].class, JdbcType.BLOB);
		map.put(byte[].class, JdbcType.BLOB);
		map.put(Object.class, JdbcType.OTHER);
		map.put(Date.class, JdbcType.TIMESTAMP);
		map.put(java.sql.Date.class, JdbcType.DATE);
		map.put(java.sql.Time.class, JdbcType.TIME);
		map.put(java.sql.Timestamp.class, JdbcType.TIMESTAMP);
		map.put(Instant.class, JdbcType.TIMESTAMP);
		map.put(LocalDateTime.class, JdbcType.TIMESTAMP_WITH_TIMEZONE);
		map.put(LocalDate.class, JdbcType.DATE);
		map.put(LocalTime.class, JdbcType.TIME);
		map.put(OffsetDateTime.class, JdbcType.TIMESTAMP);
		map.put(OffsetTime.class, JdbcType.TIME);
		map.put(ZonedDateTime.class, JdbcType.TIMESTAMP);
		map.put(Month.class, JdbcType.INTEGER);
		map.put(Year.class, JdbcType.INTEGER);
		map.put(YearMonth.class, JdbcType.VARCHAR);
		map.put(JapaneseDate.class, JdbcType.TIMESTAMP);
		JAVA_TYPE_MAPPING = Collections.unmodifiableMap(map);
	}

	protected final Model model;

	private final MybatisPersistentProperty property;

	protected final String columnAlias;

	protected Identifier name;

	protected String propertyName;

	protected JdbcType jdbcType;

	protected Class<?> javaType;

	protected Class<?> typeHandler;

	public Column(Model model, MybatisPersistentProperty property) {
		this.model = model;
		this.property = property;

		javax.persistence.Column columnAnn = property.findAnnotation(javax.persistence.Column.class);
		if (null != columnAnn && StringUtils.hasText(columnAnn.name())) {
			this.name = Identifier.toIdentifier(columnAnn.name());
		}
		else {
			this.name = Identifier
					.toIdentifier(model.getMappingContext().getFieldNamingStrategy().getFieldName(property));
		}
		this.javaType = property.getType();
		this.jdbcType = this.processJdbcType(property);
		this.typeHandler = this.processTypeHandler(property);

		String[] aliasArray = model.getAlias().split("\\.");
		int pos = 1;
		if (aliasArray.length == pos) {
			this.columnAlias = this.name.getText();
			this.propertyName = property.getName();
		}
		else {
			String[] aa = new String[aliasArray.length - pos];
			System.arraycopy(aliasArray, pos, aa, 0, aa.length);
			String pre = Arrays.stream(aa).collect(Collectors.joining("."));
			this.propertyName = pre + '.' + property.getName();

			if (model instanceof Embedding) {
				if (aa.length == 1) {
					this.columnAlias = this.name.getText();
				}
				else {
					String[] bb = new String[aa.length - 1];
					System.arraycopy(aa, 0, bb, 0, bb.length);
					this.columnAlias = Arrays.stream(bb).collect(Collectors.joining(".")) + '.' + this.name.getText();
				}
			}
			else {
				this.columnAlias = pre + '.' + this.name.getText();
			}

		}
	}

	private JdbcType processJdbcType(MybatisPersistentProperty property) {

		org.springframework.data.mybatis.annotation.JdbcType jdbcTypeAnn = property
				.findAnnotation(org.springframework.data.mybatis.annotation.JdbcType.class);
		if (null != jdbcTypeAnn) {
			return JdbcType.valueOf(jdbcTypeAnn.value());
		}

		if (property.isAnnotationPresent(Temporal.class)) {
			Temporal temporalAnn = property.getRequiredAnnotation(Temporal.class);
			switch (temporalAnn.value()) {
			case DATE:
				return JdbcType.DATE;
			case TIME:
				return JdbcType.TIME;
			case TIMESTAMP:
				return JdbcType.TIMESTAMP;
			}
		}

		if (property.getType().isEnum()) {
			Enumerated enumeratedAnn = property.findAnnotation(Enumerated.class);
			if (null != enumeratedAnn) {
				switch (enumeratedAnn.value()) {
				case ORDINAL:
					return JdbcType.INTEGER;
				case STRING:
					return JdbcType.VARCHAR;
				}
			}
			return JdbcType.VARCHAR;
		}

		if (property.isAnnotationPresent(Lob.class)) {
			if (CharSequence.class.isAssignableFrom(property.getType())) {
				return JdbcType.CLOB;
			}
			return JdbcType.BLOB;
		}

		JdbcType jdbcType = JAVA_TYPE_MAPPING.get(property.getType());

		return (null != jdbcType) ? jdbcType : JdbcType.UNDEFINED;
	}

	private Class<?> processTypeHandler(MybatisPersistentProperty property) {
		TypeHandler typeHandler = property.findAnnotation(TypeHandler.class);
		if (null != typeHandler) {
			return typeHandler.value();
		}

		if (property.isEnumerated()) {
			Enumerated enumeratedAnn = property.getRequiredAnnotation(Enumerated.class);
			switch (enumeratedAnn.value()) {
			case ORDINAL:
				return EnumOrdinalTypeHandler.class;
			case STRING:
				return EnumTypeHandler.class;
			}
		}

		if (property.getType() == Long.class && this.getJdbcType() == JdbcType.TIMESTAMP) {
			return UnixTimestampDateTypeHandler.class;
		}
		if (property.getType() == Date.class && this.getJdbcType() == JdbcType.BIGINT) {
			return DateUnixTimestampTypeHandler.class;
		}
		return null;
	}

	@Override
	public Model getModel() {
		return this.model;
	}

	public String getColumnAlias() {
		return this.columnAlias;
	}

	public Identifier getName() {
		return this.name;
	}

	public void setName(Identifier name) {
		this.name = name;
	}

	public String getPropertyName() {
		return this.propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public JdbcType getJdbcType() {
		return this.jdbcType;
	}

	public void setJdbcType(JdbcType jdbcType) {
		this.jdbcType = jdbcType;
	}

	public Class<?> getJavaType() {
		return this.javaType;
	}

	public void setJavaType(Class<?> javaType) {
		this.javaType = javaType;
	}

	public Class<?> getTypeHandler() {
		return this.typeHandler;
	}

	public void setTypeHandler(Class<?> typeHandler) {
		this.typeHandler = typeHandler;
	}

	@Override
	protected Column clone() throws CloneNotSupportedException {
		return (Column) super.clone();
	}

	public String getJavaTypeString() {
		if (null == this.javaType) {
			return null;
		}

		String type = TYPE_ALIAS.get(this.javaType);
		if (null != type) {
			return type;
		}
		return this.javaType.getName();
	}

	public boolean isString() {
		JdbcType jdbcType = this.getJdbcType();
		return jdbcType == JdbcType.VARCHAR || jdbcType == JdbcType.CHAR || jdbcType == JdbcType.CLOB
				|| jdbcType == JdbcType.NVARCHAR || jdbcType == JdbcType.NCHAR || jdbcType == JdbcType.LONGNVARCHAR
				|| jdbcType == JdbcType.LONGVARCHAR;
	}

	public MybatisPersistentProperty getProperty() {
		return this.property;
	}

	@Override
	public String toString() {
		return "Column{" + "name=" + this.name + ", propertyName='" + this.propertyName + '\'' + '}';
	}

}
