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

package io.easybest.mybatis.auxiliary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.easybest.mybatis.mapping.EntityManager;
import io.easybest.mybatis.mapping.MybatisAssociation;
import io.easybest.mybatis.mapping.MybatisAssociation.JoinColumn;
import io.easybest.mybatis.mapping.MybatisPersistentEntityImpl;
import io.easybest.mybatis.mapping.MybatisPersistentPropertyImpl;
import io.easybest.mybatis.mapping.precompile.Column;
import io.easybest.mybatis.mapping.precompile.Escape;
import io.easybest.mybatis.mapping.precompile.Function;
import io.easybest.mybatis.mapping.precompile.Parameter;
import io.easybest.mybatis.mapping.precompile.SQL;
import io.easybest.mybatis.mapping.precompile.Segment;
import io.easybest.mybatis.mapping.precompile.Table;
import io.easybest.mybatis.mapping.sql.SqlIdentifier;
import io.easybest.mybatis.repository.query.criteria.Criteria;
import io.easybest.mybatis.repository.query.criteria.PredicateResult;
import io.easybest.mybatis.repository.query.criteria.impl.CriteriaImpl;
import io.easybest.mybatis.repository.support.MybatisContext;

import org.springframework.beans.NullValueInNestedPathException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.support.ExampleMatcherAccessor;
import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * .
 *
 * @author Jarvis Song
 */
public class Syntax {

	public static SQLResult bind(MybatisContext<?, ?> context) {

		if (null == context) {
			return SQLResult.EMPTY;
		}

		EntityManager entityManager = context.getEntityManager();
		Class<?> domainType = context.getRequiredDomainType();
		boolean basic = context.isBasic();

		SQLResult.SQLResultBuilder<?, ?> builder = SQLResult.builder();

		Sort sort = context.getSort();
		Example<?> example = context.getExample();

		// <order, connector>
		Tuple<Set<String>, Set<String>> orders = sorts(entityManager, domainType, basic, sort);

		// <condition, connector>
		Tuple<Set<String>, Set<String>> examples = examples(entityManager, context, domainType, basic, example);

		// <condition, connector>
		Tuple<Set<String>, Set<String>> criteriaQueryResults = criteriaQuery(entityManager, context, domainType, basic,
				context.getCriteria());

		if (null != orders && !CollectionUtils.isEmpty(orders.getFirst())) {
			builder.sorting("ORDER BY " + String.join(",", orders.getFirst()));
		}

		if (null != examples && !CollectionUtils.isEmpty(examples.getFirst())) {
			builder.condition(examples.getFirst().stream().map(String::toString).collect(Collectors.joining(" ")));
		}

		if (null != criteriaQueryResults && !CollectionUtils.isEmpty(criteriaQueryResults.getFirst())) {
			builder.condition(
					criteriaQueryResults.getFirst().stream().map(String::toString).collect(Collectors.joining(" ")));
		}

		if (!basic) {

			Set<String> connectors = new LinkedHashSet<>();

			if (null != orders) {
				if (!CollectionUtils.isEmpty(orders.getFirst()) && !CollectionUtils.isEmpty(orders.getSecond())) {
					connectors.addAll(orders.getSecond());
				}

			}
			if (null != examples && !CollectionUtils.isEmpty(examples.getSecond())) {
				connectors.addAll(examples.getSecond());
			}

			if (!CollectionUtils.isEmpty(connectors)) {
				// FIXME sort first?
				StringBuilder sb = new StringBuilder();
				for (String connector : connectors) {
					sb.insert(0, " " + connector);
				}
				builder.connector(sb.toString());
			}
		}

		return builder.build();
	}

	private static Tuple<Set<String>, Set<String>> criteriaQuery(EntityManager entityManager,
			MybatisContext<?, ?> context, Class<?> domainType, boolean basic, Criteria<?, ?> criteria) {

		if (null == criteria) {
			return null;
		}

		Set<String> conditions = new LinkedHashSet<>();
		Set<String> connectors = new LinkedHashSet<>();

		criteriaQuery(conditions, connectors, entityManager, context, domainType, basic,
				(CriteriaImpl<?, ?, ?>) criteria);

		return new Tuple<>(conditions, connectors);
	}

	private static void criteriaQuery(Set<String> conditions, Set<String> connectors, EntityManager entityManager,
			MybatisContext<?, ?> context, Class<?> domainType, boolean basic, CriteriaImpl<?, ?, ?> criteria) {

		PredicateResult pr = criteria.toConditionSQL(entityManager, pv -> {
			context.setBindable(pv.getName(), pv.getValue());
			return Parameter.bindValue(pv.getName());
		}, false);

		conditions.add(pr.getSql());
		if (!CollectionUtils.isEmpty(pr.getConnectors())) {
			connectors.addAll(pr.getConnectors());
		}

	}

	public static Set<String> connectors(EntityManager entityManager,
			PersistentPropertyPath<MybatisPersistentPropertyImpl> ppp) {

		List<String> accumulated = new ArrayList<>();
		detectConnector(entityManager, accumulated, ppp.getParentPath());

		return new LinkedHashSet<>(accumulated);
	}

	public static Set<String> connectors(EntityManager entityManager, PropertyPath path) {

		Assert.notNull(path, "path must not be null.");

		PersistentPropertyPath<MybatisPersistentPropertyImpl> ppp = entityManager.getPersistentPropertyPath(path);

		return connectors(entityManager, ppp);
	}

	private static Tuple<Set<String>, Set<String>> sorts(EntityManager entityManager, Class<?> domainType,
			boolean basic, Sort sort) {

		if (null == sort || sort.isUnsorted()) {
			return null;
		}

		Set<String> orders = new LinkedHashSet<>();
		Set<String> connectors = new LinkedHashSet<>();
		for (Sort.Order order : sort) {

			PersistentPropertyPath<MybatisPersistentPropertyImpl> ppp = entityManager
					.getPersistentPropertyPath(order.getProperty(), domainType);
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

			orders.add((order.isIgnoreCase() ? Function.of(entityManager.getDialect().getFunction("lower"), column)
					: column) + " " + order.getDirection().name());

			if (basic || ppp.getLength() < 1) {
				continue;
			}

			List<String> accumulated = new ArrayList<>();
			detectConnector(entityManager, accumulated, ppp.getParentPath());
			connectors.addAll(accumulated);
		}

		return new Tuple<>(orders, connectors);
	}

	private static void detectConnector(EntityManager entityManager, List<String> accumulated,
			PersistentPropertyPath<MybatisPersistentPropertyImpl> current) {

		if (null == current || current.getLength() == 0) {
			return;
		}

		MybatisPersistentPropertyImpl leaf = current.getRequiredLeafProperty();
		PersistentPropertyPath<MybatisPersistentPropertyImpl> parent = current.getParentPath();

		if (leaf.isAssociation()) {

			MybatisAssociation association = leaf.getRequiredAssociation();
			accumulated.add(association.connect(parent.getLength() == 0
					? SqlIdentifier.unquoted(SQL.ROOT_ALIAS.getValue()) : SqlIdentifier.quoted(parent.toDotPath())));
		}

		detectConnector(entityManager, accumulated, parent);

	}

	private static Tuple<Set<String>, Set<String>> examples(EntityManager entityManager, MybatisContext<?, ?> context,
			Class<?> domainType, boolean basic, Example<?> example) {

		if (null == example) {
			return null;
		}

		ExampleMatcher matcher = example.getMatcher();
		ExampleMatcherAccessor matcherAccessor = new ExampleMatcherAccessor(matcher);

		Object probe = example.getProbe();

		Set<String> conditions = new LinkedHashSet<>();
		Set<String> connectors = new LinkedHashSet<>();

		examples(conditions, connectors, entityManager, context, basic, matcher, matcherAccessor, null, domainType,
				new PathNode("root", null, example.getProbe()), probe);

		return new Tuple<>(conditions, connectors);
	}

	private static void examples(Set<String> conditions, Set<String> connectors, EntityManager entityManager,
			MybatisContext<?, ?> context, boolean basic, ExampleMatcher matcher, ExampleMatcherAccessor matcherAccessor,
			String parentPath, Class<?> domainType, PathNode currentNode, Object value) {

		if (null == value) {
			return;
		}

		entityManager.findPersistentPropertyPaths(domainType, p -> true).forEach(ppp -> {

			String path = ppp.toDotPath();
			if (null == path) {
				return;
			}

			String currentPath = null == parentPath ? path : (parentPath + '.' + path);

			if (matcherAccessor.isIgnoredPath(currentPath)) {
				return;
			}

			MybatisPersistentPropertyImpl leaf = ppp.getRequiredLeafProperty();

			if (leaf.isVersionProperty()) {
				return;
			}

			if (leaf.isEmbeddable()) {
				return;
			}

			DirectFieldAccessFallbackBeanWrapper beanWrapper = new DirectFieldAccessFallbackBeanWrapper(value);
			ExampleMatcher.PropertyValueTransformer transformer = matcherAccessor
					.getValueTransformerForPath(currentPath);

			SQL predicate = matcher.isAnyMatching() ? SQL.OR : SQL.AND;
			SqlIdentifier tableAlias = null == parentPath ? SqlIdentifier.unquoted(SQL.ROOT_ALIAS.getValue())
					: SqlIdentifier.quoted(parentPath);
			Column column = Column.of(tableAlias.toSql(entityManager.getDialect().getIdentifierProcessing()),
					leaf.getColumnName().getReference(entityManager.getDialect().getIdentifierProcessing()));

			Optional<Object> optionalValue = Optional.empty();
			try {
				optionalValue = transformer.apply(Optional.ofNullable(beanWrapper.getPropertyValue(path)));
			}
			catch (NullValueInNestedPathException ex) {
				// ignore
				// only will be embeddable columns

				if (matcherAccessor.getNullHandler().equals(ExampleMatcher.NullHandler.INCLUDE)) {
					// FIXME: need to judge that is embedding?
					conditions.add(Stream.of(predicate, column, SQL.of(" IS NULL")).map(Segment::toString)
							.collect(Collectors.joining(" ")));
					return;
				}

				return;
			}

			if (leaf.isAssociation()) {

				MybatisAssociation association = leaf.getRequiredAssociation();

				if (!optionalValue.isPresent()) {

					if (matcherAccessor.getNullHandler().equals(ExampleMatcher.NullHandler.INCLUDE)) {
						// TODO

						if (association.isToOne()) {

							List<JoinColumn> joinColumns = association.getJoinColumns();
							if (!CollectionUtils.isEmpty(joinColumns)) {
								String referenceAlias;
								if (null == parentPath) {
									referenceAlias = SQL.ROOT_ALIAS.getValue();
								}
								else {
									referenceAlias = SqlIdentifier.quoted(parentPath)
											.toSql(entityManager.getDialect().getIdentifierProcessing());
								}

								conditions
										.add(predicate.getValue() + " ("
												+ joinColumns
														.stream().map(
																jc -> Column
																		.of(referenceAlias, jc.getColumnName()
																				.getReference(entityManager.getDialect()
																						.getIdentifierProcessing()))
																		+ " IS NULL")
														.collect(Collectors.joining(" AND "))
												+ ")");
								return;

							}

						}

					}

					return;
				}

				Object attributeValue = optionalValue.get();

				PathNode node = currentNode.add(path, attributeValue);
				if (node.spansCycle()) {
					throw new InvalidDataAccessApiUsageException(
							String.format("Path '%s' from root %s must not span a cyclic property reference!%n%s",
									currentPath, ClassUtils.getShortName(domainType), node));
				}

				if (association.isToOne()) {

					MybatisPersistentEntityImpl<?> targetEntity = association.getTargetEntity();
					String alias = SqlIdentifier.quoted(currentPath)
							.toSql(entityManager.getDialect().getIdentifierProcessing());
					Table table = Table.of(targetEntity.getTableName()
							.getReference(entityManager.getDialect().getIdentifierProcessing()), alias);

					String referenceAlias;
					if (null == parentPath) {
						referenceAlias = SQL.ROOT_ALIAS.getValue();
					}
					else {
						referenceAlias = SqlIdentifier.quoted(parentPath)
								.toSql(entityManager.getDialect().getIdentifierProcessing());
					}

					String sql = "LEFT OUTER JOIN " + table + " ON ";
					List<JoinColumn> joinColumns = association.getJoinColumns();
					if (!CollectionUtils.isEmpty(joinColumns)) {

						sql += joinColumns.stream()
								.map(jc -> alias + "."
										+ jc.getReferencedColumnName()
												.getReference(entityManager.getDialect().getIdentifierProcessing())
										+ " = " + referenceAlias + "."
										+ jc.getColumnName()
												.getReference(entityManager.getDialect().getIdentifierProcessing()))
								.collect(Collectors.joining(" AND "));

					}
					connectors.add(sql);

					examples(conditions, connectors, entityManager, context, basic, matcher, matcherAccessor, path,
							association.getTargetType(), node, attributeValue);
					return;
				}
				else {
					return;
				}

			}

			if (!optionalValue.isPresent()) {

				if (matcherAccessor.getNullHandler().equals(ExampleMatcher.NullHandler.INCLUDE)) {

					conditions.add(Stream.of(predicate, column, SQL.of(" IS NULL")).map(Segment::toString)
							.collect(Collectors.joining(" ")));

					return;
				}

				return;
			}

			Object attributeValue = optionalValue.get();

			boolean ignoreCase = matcherAccessor.isIgnoreCaseForPath(currentPath);
			Segment parameter = Parameter.of(MybatisContext.PARAM_INSTANCE_PREFIX + currentPath);

			List<Segment> segments = new ArrayList<>();
			segments.add(predicate);
			if (leaf.getType().equals(String.class)) {

				ExampleMatcher.StringMatcher stringMatcher = matcherAccessor.getStringMatcherForPath(currentPath);

				if (stringMatcher == ExampleMatcher.StringMatcher.REGEX) {

					segments.add(
							SQL.of(entityManager.getDialect().regexpLike(column.toString(), parameter.toString())));

					conditions.add(segments.stream().map(Segment::toString).collect(Collectors.joining(" ")));
					return;
				}

				segments.add(
						ignoreCase ? Function.of(entityManager.getDialect().getFunction("lower"), column) : column);

				String key = currentPath.replace(".", "_");

				switch (stringMatcher) {
				case DEFAULT:
				case EXACT:
					segments.add(SQL.EQUALS);
					break;
				case CONTAINING:
					segments.add(SQL.LIKE);
					context.setBindable(key,
							"%" + entityManager.getEscapeCharacter().escape((String) attributeValue) + "%");
					parameter = Parameter.of(MybatisContext.PARAM_BINDABLE_PREFIX + key);
					// parameter =
					// (Function.of(entityManager.getDialect().getFunction("concat"),
					// "'%'",
					// parameter.toString(), "'%'"));
					break;
				case STARTING:
					segments.add(SQL.LIKE);
					context.setBindable(key, entityManager.getEscapeCharacter().escape((String) attributeValue) + "%");
					parameter = Parameter.of(MybatisContext.PARAM_BINDABLE_PREFIX + key);
					// parameter =
					// (Function.of(entityManager.getDialect().getFunction("concat"),
					// parameter.toString(),
					// "'%'"));
					break;
				case ENDING:
					segments.add(SQL.LIKE);
					context.setBindable(key, "%" + entityManager.getEscapeCharacter().escape((String) attributeValue));
					parameter = Parameter.of(MybatisContext.PARAM_BINDABLE_PREFIX + key);
					// parameter =
					// (Function.of(entityManager.getDialect().getFunction("concat"),
					// "'%'",
					// parameter.toString()));
					break;
				default:
					throw new IllegalArgumentException("Unsupported StringMatcher " + stringMatcher);
				}

				segments.add(ignoreCase ? Function.of(entityManager.getDialect().getFunction("lower"), parameter)
						: parameter);

				switch (stringMatcher) {
				case CONTAINING:
				case STARTING:
				case ENDING:
					segments.add(Escape.of(entityManager.getDialect()));
				}

			}
			else {
				segments.add(column);
				segments.add(SQL.EQUALS);
				segments.add(parameter);
			}

			conditions.add(segments.stream().map(Segment::toString).collect(Collectors.joining(" ")));

		});

	}

	public static boolean isEmpty(Object obj) {

		if (null == obj) {
			return true;
		}
		if (obj.getClass().isArray()) {
			return ((Object[]) obj).length == 0;
		}
		if (obj instanceof Collection) {
			return ((Collection<?>) obj).isEmpty();
		}
		return false;
	}

	private static class PathNode {

		String name;

		@Nullable
		PathNode parent;

		List<PathNode> siblings = new ArrayList<>();

		@Nullable
		Object value;

		PathNode(String edge, @Nullable PathNode parent, @Nullable Object value) {

			this.name = edge;
			this.parent = parent;
			this.value = value;
		}

		PathNode add(String attribute, @Nullable Object value) {

			PathNode node = new PathNode(attribute, this, value);
			this.siblings.add(node);
			return node;
		}

		boolean spansCycle() {

			if (this.value == null) {
				return false;
			}

			String identityHex = ObjectUtils.getIdentityHexString(this.value);
			PathNode current = this.parent;

			while (current != null) {

				if (current.value != null && ObjectUtils.getIdentityHexString(current.value).equals(identityHex)) {
					return true;
				}
				current = current.parent;
			}

			return false;
		}

		@Override
		public String toString() {

			StringBuilder sb = new StringBuilder();
			if (this.parent != null) {
				sb.append(this.parent);
				sb.append(" -");
				sb.append(this.name);
				sb.append("-> ");
			}

			sb.append("[{ ");
			sb.append(ObjectUtils.nullSafeToString(this.value));
			sb.append(" }]");
			return sb.toString();
		}

	}

}
