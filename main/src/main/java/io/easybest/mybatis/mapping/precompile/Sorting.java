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

package io.easybest.mybatis.mapping.precompile;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.util.CollectionUtils;

import io.easybest.mybatis.auxiliary.Syntax;
import io.easybest.mybatis.mapping.EntityManager;
import io.easybest.mybatis.mapping.MybatisPersistentEntityImpl;
import io.easybest.mybatis.mapping.MybatisPersistentPropertyImpl;
import io.easybest.mybatis.mapping.sql.SqlIdentifier;

/**
 * .
 *
 * @author Jarvis Song
 */
@Getter
public class Sorting extends AbstractSegment {

	private Sort sort;

	private Set<String> connectors;

	private Set<String> orders;

	public static Builder builder() {
		return new Builder();
	}

	public static Sorting of(Sort sort) {
		return Sorting.builder().sort(sort).build();
	}

	public Sorting detectConnectors(EntityManager entityManager, MybatisPersistentEntityImpl<?> entity) {

		if (null == this.sort || this.sort.isUnsorted()) {
			return this;
		}

		boolean basic = entity.isBasic();

		for (Sort.Order order : this.sort) {

			PersistentPropertyPath<MybatisPersistentPropertyImpl> ppp = entityManager
					.getPersistentPropertyPath(order.getProperty(), entity.getType());

			MybatisPersistentPropertyImpl leaf = ppp.getRequiredLeafProperty();

			SqlIdentifier columnName = leaf.getColumnName();
			SqlIdentifier tableAlias = SqlIdentifier.unquoted(SQL.ROOT_ALIAS.getValue());

			if (!basic) {
				String tablePath = ppp.toDotPath(source -> source.isAssociation() ? source.getName() : null);
				if (null != tablePath) {
					tableAlias = SqlIdentifier.quoted(tablePath);
				}
			}

			Column column = Column.of(tableAlias.toSql(entityManager.getDialect().getIdentifierProcessing()),
					columnName.getReference(entityManager.getDialect().getIdentifierProcessing()));

			if (null == this.orders) {
				this.orders = new LinkedHashSet<>();
			}

			this.orders.add((order.isIgnoreCase() ? Function.of(entityManager.getDialect().getFunction("lower"), column)
					: column) + " " + order.getDirection().name());

			Set<String> connectors = Syntax.connectors(entityManager, ppp);
			this.addConnectors(connectors);

		}
		return this;
	}

	public void addConnectors(Set<String> connectors) {

		if (null == this.connectors) {
			this.connectors = new LinkedHashSet<>();
		}

		if (null != connectors) {
			this.connectors.addAll(connectors);
		}

	}

	public Set<String> getAllConnectors() {

		Set<String> connectors = new LinkedHashSet<>();
		if (null != this.connectors) {
			connectors.addAll(this.connectors);
		}

		return connectors;

	}

	@Override
	public String toString() {

		if (null == this.sort || this.sort.isUnsorted()) {
			return "";
		}

		StringBuilder builder = new StringBuilder();
		builder.append("ORDER BY ");

		if (!CollectionUtils.isEmpty(this.orders)) {
			builder.append(String.join(",", this.orders));

		}

		return builder.toString();
	}

	public static class Builder {

		private List<? extends Segment> contents;

		private Sort sort;

		private Set<String> connectors;

		private Set<String> orders;

		public Sorting build() {

			Sorting instance = new Sorting();
			instance.contents = this.contents;
			instance.sort = this.sort;
			instance.connectors = this.connectors;
			instance.orders = this.orders;
			return instance;
		}

		public Builder contents(final List<? extends Segment> contents) {
			this.contents = contents;
			return this;
		}

		public Builder sort(final Sort sort) {
			this.sort = sort;
			return this;
		}

		public Builder connectors(final Set<String> connectors) {
			this.connectors = connectors;
			return this;
		}

		public Builder orders(final Set<String> orders) {
			this.orders = orders;
			return this;
		}

	}

}
