/*
 * Copyright 2019-2023 the original author or authors.
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

package io.easybest.mybatis.mapping;

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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import lombok.experimental.UtilityClass;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeAliasRegistry;

/**
 * .
 *
 * @author Jarvis Song
 */
@UtilityClass
public class TypeRegistry {

	private static final Map<Class<?>, String> TYPE_ALIAS;

	private static final Map<Class<?>, JdbcType> CLASS_JDBC_TYPE_MAP;
	static {

		Map<Class<?>, String> typeAlias = new HashMap<>();
		new TypeAliasRegistry().getTypeAliases().forEach((key, value) -> typeAlias.put(value, key));
		TYPE_ALIAS = Collections.unmodifiableMap(typeAlias);

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
		map.put(UUID.class, JdbcType.VARCHAR);
		CLASS_JDBC_TYPE_MAP = Collections.unmodifiableMap(map);
	}

	public static Optional<JdbcType> convert(Class<?> javaType) {
		if (null == javaType) {
			return Optional.empty();
		}
		return Optional.ofNullable(CLASS_JDBC_TYPE_MAP.get(javaType));
	}

	public static Optional<String> alias(Class<?> javaType) {
		if (null == javaType) {
			return Optional.empty();
		}
		return Optional.ofNullable(TYPE_ALIAS.get(javaType));
	}

}
