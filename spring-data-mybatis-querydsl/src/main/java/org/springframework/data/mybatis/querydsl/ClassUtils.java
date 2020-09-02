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

import java.util.Map;
import java.util.Optional;

import org.springframework.lang.NonNull;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public final class ClassUtils {

	private ClassUtils() {
	}

	private static final Map<String, Class> PRIMITIVE_TYPE_MAP = CollectionUtils.mapOf("int", Integer.TYPE, "boolean",
			Boolean.TYPE, "long", Long.TYPE, "byte", Byte.TYPE, "double", Double.TYPE, "float", Float.TYPE, "char",
			Character.TYPE, "short", Short.TYPE, "void", void.class);

	private static final Map<String, Class> PRIMITIVE_ARRAY_MAP = CollectionUtils.mapOf("int", int[].class, "boolean",
			boolean[].class, "long", long[].class, "byte", byte[].class, "double", double[].class, "float",
			float[].class, "char", char[].class, "short", short[].class);

	public static Optional<Class> getPrimitiveType(String primitiveType) {
		return Optional.ofNullable(PRIMITIVE_TYPE_MAP.get(primitiveType));
	}

	public static @NonNull Optional<Class> arrayTypeForPrimitive(String primitiveType) {
		if (primitiveType != null) {
			return Optional.ofNullable(PRIMITIVE_ARRAY_MAP.get(primitiveType));
		}
		return Optional.empty();
	}

}
