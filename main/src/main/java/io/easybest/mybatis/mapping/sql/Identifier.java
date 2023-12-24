/*
 * Copyright 2019-2022 the original author or authors.
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

package io.easybest.mybatis.mapping.sql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@literal Identifier} represents a composite id of an entity that may be composed of
 * one or many parts. Parts or all of the entity might not have a representation as a
 * property in the entity but might only be derived from other entities referencing it.
 *
 * @author Jens Schauder
 * @author Mark Paluch
 * @since 1.1
 */
public final class Identifier {

	private static final Identifier EMPTY = new Identifier(Collections.emptyList());

	private final List<SingleIdentifierValue> parts;

	private Identifier(List<SingleIdentifierValue> parts) {
		this.parts = parts;
	}

	public static Identifier empty() {
		return EMPTY;
	}

	public static Identifier of(SqlIdentifier name, Object value, Class<?> targetType) {

		Assert.notNull(name, "Name must not be empty!");
		Assert.notNull(targetType, "Target type must not be null!");

		return new Identifier(Collections.singletonList(new SingleIdentifierValue(name, value, targetType)));
	}

	public static Identifier from(Map<SqlIdentifier, Object> map) {

		Assert.notNull(map, "Map must not be null!");

		if (map.isEmpty()) {
			return empty();
		}

		List<SingleIdentifierValue> values = new ArrayList<>();

		map.forEach((k, v) -> values
				.add(new SingleIdentifierValue(k, v, v != null ? ClassUtils.getUserClass(v) : Object.class)));

		return new Identifier(Collections.unmodifiableList(values));
	}

	public Identifier withPart(SqlIdentifier name, Object value, Class<?> targetType) {

		Assert.notNull(name, "Name must not be null!");
		Assert.notNull(targetType, "Target type must not be null!");

		boolean overwritten = false;
		List<SingleIdentifierValue> keys = new ArrayList<>(this.parts.size() + 1);

		for (SingleIdentifierValue singleValue : this.parts) {

			if (singleValue.getName().equals(name)) {
				overwritten = true;
				keys.add(new SingleIdentifierValue(singleValue.getName(), value, targetType));
			}
			else {
				keys.add(singleValue);
			}
		}

		if (!overwritten) {
			keys.add(new SingleIdentifierValue(name, value, targetType));
		}

		return new Identifier(Collections.unmodifiableList(keys));
	}

	public Map<SqlIdentifier, Object> toMap() {

		Map<SqlIdentifier, Object> result = new StringKeyedLinkedHashMap<>(this.getParts().size());
		this.forEach((name, value, type) -> result.put(name, value));
		return result;
	}

	public Collection<SingleIdentifierValue> getParts() {
		return this.parts;
	}

	public void forEach(IdentifierConsumer consumer) {

		Assert.notNull(consumer, "IdentifierConsumer must not be null");

		this.getParts().forEach(it -> consumer.accept(it.name, it.value, it.targetType));
	}

	public int size() {
		return this.parts.size();
	}

	@Override
	public boolean equals(Object o) {

		if (this == o) {
			return true;
		}
		if (o == null || this.getClass() != o.getClass()) {
			return false;
		}
		Identifier that = (Identifier) o;
		return Objects.equals(this.parts, that.parts);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.parts);
	}

	@Override
	public String toString() {

		return "Identifier{" + "parts=" + this.parts + '}';
	}

	@FunctionalInterface
	public interface IdentifierConsumer {

		void accept(SqlIdentifier name, Object value, Class<?> targetType);

	}

	static final class SingleIdentifierValue {

		private final SqlIdentifier name;

		private final Object value;

		private final Class<?> targetType;

		private SingleIdentifierValue(SqlIdentifier name, @Nullable Object value, Class<?> targetType) {

			Assert.notNull(name, "Name must not be null.");
			Assert.notNull(targetType, "TargetType must not be null.");

			this.name = name;
			this.value = value;
			this.targetType = targetType;
		}

		public SqlIdentifier getName() {
			return this.name;
		}

		public Object getValue() {
			return this.value;
		}

		public Class<?> getTargetType() {
			return this.targetType;
		}

		@Override
		public boolean equals(Object o) {

			if (this == o) {
				return true;
			}
			if (o == null || this.getClass() != o.getClass()) {
				return false;
			}
			SingleIdentifierValue that = (SingleIdentifierValue) o;
			return this.name.equals(that.name) && this.value.equals(that.value)
					&& this.targetType.equals(that.targetType);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.name, this.value, this.targetType);
		}

		@Override
		public String toString() {

			return "SingleIdentifierValue{" + "name=" + this.name + ", value=" + this.value + ", targetType="
					+ this.targetType + '}';
		}

	}

	private static class StringKeyedLinkedHashMap<V> extends LinkedHashMap<SqlIdentifier, V> {

		private static final long serialVersionUID = 8310126935017610984L;

		public StringKeyedLinkedHashMap(int initialCapacity) {
			super(initialCapacity);
		}

		@Override
		public V get(Object key) {

			if (key instanceof String) {

				for (SqlIdentifier sqlIdentifier : this.keySet()) {
					if (sqlIdentifier.getReference().equals(key)) {
						return super.get(sqlIdentifier);
					}
				}
			}

			return super.get(key);
		}

	}

}
