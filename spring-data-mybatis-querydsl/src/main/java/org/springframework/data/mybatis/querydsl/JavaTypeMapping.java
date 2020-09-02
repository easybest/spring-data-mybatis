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
package org.springframework.data.mybatis.querydsl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.primitives.Primitives;
import com.querydsl.core.util.ReflectionUtils;
import com.querydsl.sql.types.BigDecimalType;
import com.querydsl.sql.types.BigIntegerType;
import com.querydsl.sql.types.BlobType;
import com.querydsl.sql.types.BooleanType;
import com.querydsl.sql.types.ByteType;
import com.querydsl.sql.types.BytesType;
import com.querydsl.sql.types.CalendarType;
import com.querydsl.sql.types.CharacterType;
import com.querydsl.sql.types.ClobType;
import com.querydsl.sql.types.CurrencyType;
import com.querydsl.sql.types.DateTimeType;
import com.querydsl.sql.types.DateType;
import com.querydsl.sql.types.DoubleType;
import com.querydsl.sql.types.FloatType;
import com.querydsl.sql.types.IntegerType;
import com.querydsl.sql.types.LocalDateTimeType;
import com.querydsl.sql.types.LocalDateType;
import com.querydsl.sql.types.LocalTimeType;
import com.querydsl.sql.types.LocaleType;
import com.querydsl.sql.types.LongType;
import com.querydsl.sql.types.ObjectType;
import com.querydsl.sql.types.ShortType;
import com.querydsl.sql.types.StringType;
import com.querydsl.sql.types.TimeType;
import com.querydsl.sql.types.TimestampType;
import com.querydsl.sql.types.Type;
import com.querydsl.sql.types.URLType;
import com.querydsl.sql.types.UtilDateType;
import com.querydsl.sql.types.UtilUUIDType;

/**
 * .
 *
 * @author JARVIS SONG
 */
class JavaTypeMapping {

	private static final Type<Object> DEFAULT = new ObjectType();

	private static final Map<Class<?>, Type<?>> defaultTypes = new HashMap<>();

	static {
		registerDefault(new BigIntegerType());
		registerDefault(new BigDecimalType());
		registerDefault(new BlobType());
		registerDefault(new BooleanType());
		registerDefault(new BytesType());
		registerDefault(new ByteType());
		registerDefault(new CharacterType());
		registerDefault(new CalendarType());
		registerDefault(new ClobType());
		registerDefault(new CurrencyType());
		registerDefault(new DateType());
		registerDefault(new DoubleType());
		registerDefault(new FloatType());
		registerDefault(new IntegerType());
		registerDefault(new LocaleType());
		registerDefault(new LongType());
		registerDefault(new ObjectType());
		registerDefault(new ShortType());
		registerDefault(new StringType());
		registerDefault(new TimestampType());
		registerDefault(new TimeType());
		registerDefault(new URLType());
		registerDefault(new UtilDateType());
		registerDefault(new UtilUUIDType(false));

		// Joda time types
		registerDefault(new DateTimeType());
		registerDefault(new LocalDateTimeType());
		registerDefault(new LocalDateType());
		registerDefault(new LocalTimeType());

		// initialize java time api (JSR 310) converters only if java 8 is available
		try {
			Class.forName("java.time.Instant");
			registerDefault((Type<?>) Class.forName("com.querydsl.sql.types.JSR310InstantType").newInstance());
			registerDefault((Type<?>) Class.forName("com.querydsl.sql.types.JSR310LocalDateTimeType").newInstance());
			registerDefault((Type<?>) Class.forName("com.querydsl.sql.types.JSR310LocalDateType").newInstance());
			registerDefault((Type<?>) Class.forName("com.querydsl.sql.types.JSR310LocalTimeType").newInstance());
			registerDefault((Type<?>) Class.forName("com.querydsl.sql.types.JSR310OffsetDateTimeType").newInstance());
			registerDefault((Type<?>) Class.forName("com.querydsl.sql.types.JSR310OffsetTimeType").newInstance());
			registerDefault((Type<?>) Class.forName("com.querydsl.sql.types.JSR310ZonedDateTimeType").newInstance());
		}
		catch (ClassNotFoundException ex) {
			// converters for JSR 310 are not loaded
		}
		catch (InstantiationException ex) {
			throw new RuntimeException(ex);
		}
		catch (IllegalAccessException ex) {
			throw new RuntimeException(ex);
		}
	}

	private static void registerDefault(Type<?> type) {
		defaultTypes.put(type.getReturnedClass(), type);
		Class<?> primitive = Primitives.unwrap(type.getReturnedClass());
		if (primitive != null) {
			defaultTypes.put(primitive, type);
		}
	}

	private final Map<Class<?>, Type<?>> typeByClass = new HashMap<>();

	private final Map<Class<?>, Type<?>> resolvedTypesByClass = new HashMap<>();

	@SuppressWarnings("unchecked")
	public <T> Type<T> getType(Class<T> clazz) {
		Type<?> resolvedType = this.resolvedTypesByClass.get(clazz);
		if (resolvedType == null) {
			resolvedType = this.findType(clazz);
			if (resolvedType != null) {
				this.resolvedTypesByClass.put(clazz, resolvedType);
			}
			else {
				return (Type) DEFAULT;
			}
		}
		return (Type<T>) resolvedType;
	}

	@Nullable
	private Type<?> findType(Class<?> clazz) {
		// Look for a registered type in the class hierarchy
		Class<?> cl = clazz;
		do {
			if (this.typeByClass.containsKey(cl)) {
				return this.typeByClass.get(cl);
			}
			else if (defaultTypes.containsKey(cl)) {
				return defaultTypes.get(cl);
			}
			cl = cl.getSuperclass();
		}
		while (!cl.equals(Object.class));

		// Look for a registered type in any implemented interfaces
		Set<Class<?>> interfaces = ReflectionUtils.getImplementedInterfaces(clazz);
		for (Class<?> itf : interfaces) {
			if (this.typeByClass.containsKey(itf)) {
				return this.typeByClass.get(itf);
			}
			else if (defaultTypes.containsKey(itf)) {
				return defaultTypes.get(itf);
			}
		}
		return null;
	}

	public void register(Type<?> type) {
		this.typeByClass.put(type.getReturnedClass(), type);
		Class<?> primitive = Primitives.unwrap(type.getReturnedClass());
		if (primitive != null) {
			this.typeByClass.put(primitive, type);
		}
		// Clear previous resolved types, so they won't impact future lookups
		this.resolvedTypesByClass.clear();
	}

}
