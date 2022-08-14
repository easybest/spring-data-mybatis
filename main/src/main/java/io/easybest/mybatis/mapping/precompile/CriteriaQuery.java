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
import java.util.Set;
import java.util.stream.Collectors;

import io.easybest.mybatis.auxiliary.SQLResult;
import io.easybest.mybatis.auxiliary.Syntax;
import lombok.Getter;

import org.springframework.data.mapping.MappingException;

import static io.easybest.mybatis.mapping.precompile.CriteriaQuery.Type.DELETE;
import static io.easybest.mybatis.mapping.precompile.CriteriaQuery.Type.SELECT;
import static io.easybest.mybatis.mapping.precompile.Include.COLUMN_LIST;
import static io.easybest.mybatis.mapping.precompile.Include.COLUMN_LIST_USING_TYPE;
import static io.easybest.mybatis.mapping.precompile.Include.TABLE_NAME;
import static io.easybest.mybatis.mapping.precompile.Include.TABLE_NAME_PURE;
import static io.easybest.mybatis.mapping.precompile.MybatisMapperSnippet.MYBATIS_DEFAULT_PARAMETER_NAME;

/**
 * .
 *
 * @author Jarvis Song
 */
@Getter
public class CriteriaQuery implements Segment {

	private boolean distinct;

	private Segment[] columns;

	private Segment[] from;

	private Predicate[] predicates;

	private Set<String> connectors;

	private Sorting sorting;

	private boolean basic;

	private boolean bind;

	private boolean columnAsType;

	public static CriteriaQuery of() {
		return new CriteriaQuery();
	}

	public CriteriaQuery basic(boolean basic) {

		this.basic = basic;
		return this;
	}

	public CriteriaQuery select(Segment... columns) {

		this.columns = columns;

		return this;
	}

	public CriteriaQuery from(Segment... from) {

		this.from = from;

		return this;
	}

	public CriteriaQuery where(Predicate... predicates) {

		this.predicates = predicates;

		if (predicates.length > 0) {

			if (null == this.connectors) {
				this.connectors = new LinkedHashSet<>();
			}

			for (Predicate predicate : predicates) {
				if (null == predicate) {
					continue;
				}
				this.connectors.addAll(predicate.getAllConnectors());
			}

		}

		return this;
	}

	public CriteriaQuery sort(Sorting sort) {

		this.sorting = sort;

		return this;
	}

	public CriteriaQuery distinct(boolean distinct) {

		this.distinct = distinct;
		return this;
	}

	public CriteriaQuery bind(boolean bind) {

		this.bind = bind;
		return this;
	}

	public CriteriaQuery columnAsType(boolean columnAsType) {

		this.columnAsType = columnAsType;
		return this;
	}

	@Override
	public String toString() {

		return this.toString(SELECT);
	}

	private StringBuilder join() {
		StringBuilder sb = new StringBuilder();
		Set<String> connectors = new LinkedHashSet<>();
		if (null != this.predicates && this.predicates.length > 0) {
			for (Predicate predicate : this.predicates) {
				if (null == predicate) {
					continue;
				}
				connectors.addAll(predicate.getAllConnectors());
			}
		}
		if (null != this.sorting) {
			connectors.addAll(this.sorting.getAllConnectors());
		}
		for (String connector : connectors) {
			sb.insert(0, " " + connector);
		}
		return sb;
	}

	public String toString(Type commandType) {

		StringBuilder builder = new StringBuilder();

		switch (commandType) {
		case DELETE:
			builder.append("DELETE FROM ");
			builder.append(
					null != this.from ? Arrays.stream(this.from).map(Segment::toString).collect(Collectors.joining(" "))
							: TABLE_NAME_PURE);

			if (!this.basic) {
				builder.append(this.join());
			}

			if (null != this.predicates && this.predicates.length > 0) {
				builder.append(" ").append(Where.of(this.predicates));
			}

			break;
		case SELECT:
			if (this.bind) {
				builder.append(Bind.of(SQLResult.PARAM_NAME,
						MethodInvocation.of(Syntax.class, "bind", MYBATIS_DEFAULT_PARAMETER_NAME)));
			}
			builder.append("SELECT ");
			if (this.distinct) {
				builder.append("DISTINCT ");
			}

			builder.append(null != this.columns
					? Arrays.stream(this.columns).map(Segment::toString).collect(Collectors.joining(" "))
					: (this.columnAsType ? COLUMN_LIST_USING_TYPE : COLUMN_LIST));

			builder.append(" FROM ").append(null != this.from
					? Arrays.stream(this.from).map(Segment::toString).collect(Collectors.joining(" ")) : TABLE_NAME);

			if (!this.basic) {
				builder.append(this.join());
			}
			if (this.bind) {
				builder.append(Interpolation.of(SQLResult.PARAM_CONNECTOR_NAME));
			}

			if (null != this.predicates && this.predicates.length > 0) {
				builder.append(" ").append(Where.of(this.predicates));
			}

			if (this.bind) {
				builder.append(Interpolation.of(SQLResult.PARAM_SORTING_NAME));
			}
			else if (null != this.sorting) {
				builder.append(" ").append(this.sorting);
			}

			break;
		case COUNT:
			builder.append("SELECT ");
			if (this.distinct) {
				builder.append("COUNT(DISTINCT ").append(null != this.columns
						? Arrays.stream(this.columns).map(Segment::toString).collect(Collectors.joining(" ")) : "*")
						.append(") ");
			}
			else {
				builder.append("COUNT(*) ");
			}

			builder.append(" FROM ").append(null != this.from
					? Arrays.stream(this.from).map(Segment::toString).collect(Collectors.joining(" ")) : TABLE_NAME);

			if (!this.basic) {
				builder.append(this.join());
			}

			if (null != this.predicates && this.predicates.length > 0) {
				builder.append(" ").append(Where.of(this.predicates));
			}

			break;
		default:
			throw new MappingException("Unsupported SQL command type " + commandType);
		}

		String sql = builder.toString();

		if (commandType == DELETE) {
			sql = sql.replace(SQL.ROOT_ALIAS.getValue() + '.', "");
		}

		return sql;
	}

	public enum Type {

		/**
		 * SELECT.
		 */
		SELECT,
		/**
		 * DELETE.
		 */
		DELETE,
		/**
		 * COUNT.
		 */
		COUNT

	}

}
