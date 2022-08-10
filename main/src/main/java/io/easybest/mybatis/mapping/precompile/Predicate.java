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

package io.easybest.mybatis.mapping.precompile;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.criteria.Predicate.BooleanOperator;

import io.easybest.mybatis.auxiliary.Syntax;
import io.easybest.mybatis.mapping.EntityManager;
import io.easybest.mybatis.mapping.MybatisPersistentPropertyImpl;
import lombok.Getter;

import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.mapping.PropertyPath;

import static javax.persistence.criteria.Predicate.BooleanOperator.AND;
import static javax.persistence.criteria.Predicate.BooleanOperator.OR;

/**
 * .
 *
 * @author Jarvis Song
 */
@Getter
public class Predicate extends AbstractSegment {

	private BooleanOperator operator;

	private Predicate restriction;

	private Set<String> connectors;

	public static Predicate of(Segment... segments) {
		return Predicate.builder().contents(Arrays.asList(segments)).build();
	}

	public Predicate or(Predicate restriction) {

		Predicate p = of(this);
		p.restriction = restriction;
		p.operator = OR;

		return p;
	}

	public Predicate and(Predicate restriction) {

		Predicate p = of(this);
		p.restriction = restriction;
		p.operator = AND;

		return p;
	}

	public Predicate detectConnectors(EntityManager entityManager, PropertyPath path) {

		Set<String> connectors = Syntax.connectors(entityManager, path);
		return this.addConnectors(connectors);
	}

	public Predicate detectConnectors(EntityManager entityManager,
			PersistentPropertyPath<MybatisPersistentPropertyImpl> persistentPropertyPath) {

		Set<String> connectors = Syntax.connectors(entityManager, persistentPropertyPath);
		return this.addConnectors(connectors);
	}

	public Predicate addConnectors(Set<String> connectors) {

		if (null == this.connectors) {
			this.connectors = new LinkedHashSet<>();
		}

		if (null != connectors) {
			this.connectors.addAll(connectors);
		}

		return this;
	}

	public Set<String> getAllConnectors() {

		Set<String> connectors = new LinkedHashSet<>();
		if (null != this.connectors) {
			connectors.addAll(this.connectors);
		}
		if (null != this.restriction) {
			connectors.addAll(this.restriction.getAllConnectors());
		}

		return connectors;

	}

	@Override
	public String toString() {

		StringBuilder builder = new StringBuilder();
		builder.append("(");
		builder.append(this.contents.stream().map(Segment::toString).collect(Collectors.joining(" ")));
		builder.append(")");
		if (null != this.restriction) {

			switch (this.operator) {
			case OR:
				builder.append(" OR ");
				break;
			case AND:
				builder.append(" AND ");
				break;
			}

			builder.append(this.restriction);

		}

		return builder.toString();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		protected List<? extends Segment> contents;

		private BooleanOperator operator;

		private Predicate restriction;

		private Set<String> connectors;

		public Predicate build() {

			Predicate instance = new Predicate();
			instance.contents = this.contents;
			instance.operator = this.operator;
			instance.restriction = this.restriction;
			instance.connectors = this.connectors;

			return instance;
		}

		public Builder contents(final List<? extends Segment> contents) {
			this.contents = contents;
			return this;
		}

		public Builder operator(final BooleanOperator operator) {
			this.operator = operator;
			return this;
		}

		public Builder restriction(final Predicate restriction) {
			this.restriction = restriction;
			return this;
		}

		public Builder connectors(final Set<String> connectors) {
			this.connectors = connectors;
			return this;
		}

	}

}
