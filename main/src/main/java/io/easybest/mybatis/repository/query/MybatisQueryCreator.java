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

package io.easybest.mybatis.repository.query;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import io.easybest.mybatis.auxiliary.Syntax;
import io.easybest.mybatis.mapping.EntityManager;
import io.easybest.mybatis.mapping.MybatisAssociation;
import io.easybest.mybatis.mapping.MybatisPersistentEntityImpl;
import io.easybest.mybatis.mapping.MybatisPersistentPropertyImpl;
import io.easybest.mybatis.mapping.precompile.Bind;
import io.easybest.mybatis.mapping.precompile.Choose;
import io.easybest.mybatis.mapping.precompile.Column;
import io.easybest.mybatis.mapping.precompile.CriteriaQuery;
import io.easybest.mybatis.mapping.precompile.Escape;
import io.easybest.mybatis.mapping.precompile.Foreach;
import io.easybest.mybatis.mapping.precompile.Function;
import io.easybest.mybatis.mapping.precompile.MethodInvocation;
import io.easybest.mybatis.mapping.precompile.Parameter;
import io.easybest.mybatis.mapping.precompile.ParameterExpression;
import io.easybest.mybatis.mapping.precompile.Predicate;
import io.easybest.mybatis.mapping.precompile.SQL;
import io.easybest.mybatis.mapping.precompile.Segment;
import io.easybest.mybatis.mapping.sql.SqlIdentifier;
import io.easybest.mybatis.repository.query.ParameterMetadataProvider.ParameterMetadata;
import io.easybest.mybatis.repository.support.MybatisContext;

import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import static org.springframework.data.repository.query.parser.Part.Type.ENDING_WITH;
import static org.springframework.data.repository.query.parser.Part.Type.GREATER_THAN_EQUAL;
import static org.springframework.data.repository.query.parser.Part.Type.LESS_THAN_EQUAL;
import static org.springframework.data.repository.query.parser.Part.Type.NOT_CONTAINING;
import static org.springframework.data.repository.query.parser.Part.Type.NOT_IN;
import static org.springframework.data.repository.query.parser.Part.Type.SIMPLE_PROPERTY;
import static org.springframework.data.repository.query.parser.Part.Type.STARTING_WITH;

/**
 * .
 *
 * @author Jarvis Song
 */
public class MybatisQueryCreator extends AbstractQueryCreator<CriteriaQuery, Predicate> {

	private final EntityManager entityManager;

	private final CriteriaQuery query;

	private final ParameterMetadataProvider provider;

	private final ReturnedType returnedType;

	private final PartTree tree;

	private final MybatisPersistentEntityImpl<?> entity;

	public MybatisQueryCreator(PartTree tree, ReturnedType type, ParameterMetadataProvider provider,
			EntityManager entityManager, MybatisPersistentEntityImpl<?> entity) {

		super(tree);

		this.entityManager = entityManager;
		this.tree = tree;
		this.entity = entity;
		CriteriaQuery criteriaQuery = this.createCriteriaQuery(type);
		this.query = criteriaQuery.distinct(tree.isDistinct()).basic(entity.isBasic());
		this.provider = provider;
		this.returnedType = type;
	}

	protected CriteriaQuery createCriteriaQuery(ReturnedType type) {

		return CriteriaQuery.of();
	}

	public List<ParameterMetadata<?>> getParameterExpressions() {
		return this.provider.getExpressions();
	}

	@Override
	protected Predicate create(Part part, Iterator<Object> iterator) {

		return this.toPredicate(part);
	}

	@Override
	protected Predicate and(Part part, Predicate base, Iterator<Object> iterator) {

		return base.and(this.toPredicate(part));
	}

	@Override
	protected Predicate or(Predicate base, Predicate criteria) {

		return base.or(criteria);
	}

	@Override
	protected CriteriaQuery complete(Predicate criteria, Sort sort) {

		return this.complete(criteria, sort, this.query);
	}

	protected CriteriaQuery complete(@Nullable Predicate predicate, Sort sort, CriteriaQuery query) {

		return query.where(predicate);// .sort(Sorting.of(sort).detectConnectors(this.entityManager,
		// this.entity));
	}

	private Predicate toPredicate(Part part) {
		return new PredicateBuilder(this.entityManager, part).build();
	}

	private class PredicateBuilder {

		private final Part part;

		private final PersistentPropertyPath<MybatisPersistentPropertyImpl> persistentPropertyPath;

		private final EntityManager entityManager;

		public PredicateBuilder(EntityManager entityManager, Part part) {
			this.entityManager = entityManager;

			Assert.notNull(part, "Part must not be null!");

			this.part = part;
			this.persistentPropertyPath = entityManager.getPersistentPropertyPath(part.getProperty());
		}

		public Predicate build() {

			PropertyPath property = this.part.getProperty();
			Part.Type type = this.part.getType();

			MybatisPersistentPropertyImpl leaf = this.persistentPropertyPath.getRequiredLeafProperty();

			SqlIdentifier tableAlias = SqlIdentifier.unquoted(SQL.ROOT_ALIAS.getValue());

			if (!MybatisQueryCreator.this.entity.isBasic()) {
				String tablePath = this.persistentPropertyPath
						.toDotPath(source -> source.isAssociation() ? source.getName() : null);
				if (null != tablePath) {
					tableAlias = SqlIdentifier.quoted(tablePath);
				}
			}

			if (leaf.isAssociation()) {
				// TODO

				MybatisAssociation association = leaf.getRequiredAssociation();

				switch (type) {

				case CONTAINING:
				case NOT_CONTAINING:

					ParameterMetadata<Object> pm = MybatisQueryCreator.this.provider.next(this.part);
					SQL subSelect = SQL.EMPTY;

					MybatisAssociation.JoinTable joinTable = association.getJoinTable();
					if (null != joinTable) {
						subSelect = SQL
								.of("(SELECT "
										+ joinTable.getJoinColumns()[0].getColumnName()
												.getReference(this.entityManager.getDialect().getIdentifierProcessing())
										+ " FROM "
										+ joinTable.getTable()
												.getReference(this.entityManager.getDialect().getIdentifierProcessing())
										+ " WHERE "
										+ joinTable.getInverseJoinColumns()[0].getColumnName()
												.getReference(this.entityManager.getDialect().getIdentifierProcessing())
										+ "="
										+ Parameter.additionalValue(pm.getExpression().getName() + "."
												+ association.getTargetEntity().getRequiredIdProperty().getName())
										+ ")");
					}
					// TODO ONE2MANY

					return Predicate.of(
							Column.of(SQL.ROOT_ALIAS.toString(),
									MybatisQueryCreator.this.entity.getRequiredIdProperty().getColumnName()
											.getReference()),
							type.equals(NOT_CONTAINING) ? SQL.of("NOT IN") : SQL.of("IN"), subSelect

					);

				}

				return null;
			}

			Column column = Column.of(tableAlias.toSql(this.entityManager.getDialect().getIdentifierProcessing()),
					leaf.getColumnName().getReference(this.entityManager.getDialect().getIdentifierProcessing()));

			switch (type) {

			case BETWEEN:
				ParameterMetadata<Object> first = MybatisQueryCreator.this.provider.next(this.part);
				ParameterMetadata<Object> second = MybatisQueryCreator.this.provider.next(this.part);
				return Predicate
						.of(column, SQL.BETWEEN, Parameter.additionalValue(first.getExpression().getName()), SQL.AND,
								Parameter.of(second.getExpression().getName()))
						.detectConnectors(this.entityManager, this.persistentPropertyPath);

			case AFTER:
			case GREATER_THAN:
			case GREATER_THAN_EQUAL:
				return Predicate
						.of(column, type.equals(GREATER_THAN_EQUAL) ? SQL.GREATER_THAN_EQUAL_TR : SQL.GREATER_THAN_TR,
								Parameter.additionalValue(
										MybatisQueryCreator.this.provider.next(this.part).getExpression().getName()))
						.detectConnectors(this.entityManager, this.persistentPropertyPath);

			case BEFORE:
			case LESS_THAN:
			case LESS_THAN_EQUAL:
				return Predicate
						.of(column, type.equals(LESS_THAN_EQUAL) ? SQL.LESS_THAN_EQUAL_TR : SQL.LESS_THAN_TR,
								Parameter.additionalValue(
										MybatisQueryCreator.this.provider.next(this.part).getExpression().getName()))
						.detectConnectors(this.entityManager, this.persistentPropertyPath);

			case IS_NULL:
				return Predicate.of(column, SQL.of("IS NULL")).detectConnectors(this.entityManager,
						this.persistentPropertyPath);

			case IS_NOT_NULL:
				return Predicate.of(column, SQL.of("IS NOT NULL")).detectConnectors(this.entityManager,
						this.persistentPropertyPath);

			case IN:
			case NOT_IN:
				ParameterMetadata<Object> propertyExpression = MybatisQueryCreator.this.provider.next(this.part);

				String paramName = MybatisContext.PARAM_ADDITIONAL_VALUES_PREFIX
						+ propertyExpression.getExpression().getName();
				return Predicate.of(this.lowerIfIgnoreCase(propertyExpression, column),
						type.equals(NOT_IN) ? SQL.of("NOT IN") : SQL.of("IN"),

						Choose.of(MethodInvocation.of(Syntax.class, "isEmpty", paramName).toString(), SQL.of("(NULL)"),
								Foreach.builder().collection(paramName)
										.contents(Collections.singletonList(
												this.lowerIfIgnoreCase(propertyExpression, Parameter.of("item"))))
										.build())

				).detectConnectors(this.entityManager, this.persistentPropertyPath);

			case STARTING_WITH:
			case ENDING_WITH:
			case CONTAINING:
			case NOT_CONTAINING:

				if (property.getLeafProperty().isCollection()) {
					// TODO
					return null;
				}

				propertyExpression = MybatisQueryCreator.this.provider.next(this.part);

				return Predicate.of(
						Bind.of("__bindable_" + propertyExpression.getExpression().getName(),
								(type.equals(STARTING_WITH) ? "" : "'%'+") + "entityManager.escapeCharacter.escape("
										+ MybatisContext.PARAM_ADDITIONAL_VALUES_PREFIX
										+ propertyExpression.getExpression().getName() + ")"
										+ (type.equals(ENDING_WITH) ? "" : "+'%'")),

						this.lowerIfIgnoreCase(propertyExpression, column),
						type.equals(NOT_CONTAINING) ? SQL.NOT_LIKE : SQL.LIKE,
						Parameter.of("__bindable_" + propertyExpression.getExpression().getName()),
						Escape.of(this.entityManager.getDialect())
				// Function.of(this.entityManager.getDialect().getFunction("concat"),
				// type.equals(STARTING_WITH) ? SQL.EMPTY : SQL.PAH,
				// this.lowerIfIgnoreCase(propertyExpression,
				// Parameter
				// .additionalValue(propertyExpression.getExpression().getName())),
				// type.equals(ENDING_WITH) ? SQL.EMPTY : SQL.PAH)
				).detectConnectors(this.entityManager, this.persistentPropertyPath);

			case LIKE:
			case NOT_LIKE:

				propertyExpression = MybatisQueryCreator.this.provider.next(this.part);

				return Predicate
						.of(this.lowerIfIgnoreCase(propertyExpression, column),
								type.equals(Part.Type.NOT_LIKE) ? SQL.NOT_LIKE : SQL.LIKE,
								this.lowerIfIgnoreCase(propertyExpression,
										Parameter.additionalValue(propertyExpression.getExpression().getName())),
								Escape.of(this.entityManager.getDialect()))
						.detectConnectors(this.entityManager, this.persistentPropertyPath);

			case TRUE:
				return Predicate
						.of(column, SQL.EQUALS,
								this.entityManager.getDialect().supportsBoolean() ? SQL.TRUE : SQL.of("1"))
						.detectConnectors(this.entityManager, this.persistentPropertyPath);

			case FALSE:
				return Predicate
						.of(column, SQL.EQUALS,
								this.entityManager.getDialect().supportsBoolean() ? SQL.FALSE : SQL.of("0"))
						.detectConnectors(this.entityManager, this.persistentPropertyPath);

			case SIMPLE_PROPERTY:
			case NEGATING_SIMPLE_PROPERTY:
				ParameterMetadata<Object> expression = MybatisQueryCreator.this.provider.next(this.part);
				if (expression.isIsNullParameter()) {
					return Predicate.of(column, SQL.of("IS NULL"));
				}
				Parameter parameter = Parameter.additionalValue(expression.getExpression().getName());
				return Predicate
						.of(this.lowerIfIgnoreCase(expression, column),
								type.equals(SIMPLE_PROPERTY) ? SQL.EQUALS : SQL.NOT_EQUALS_TR,
								this.lowerIfIgnoreCase(expression, parameter))
						.detectConnectors(this.entityManager, this.persistentPropertyPath);

			case REGEX:
				return Predicate
						.of(SQL.of(
								this.entityManager.getDialect().regexpLike(column.toString(),
										Parameter.additionalValue(MybatisQueryCreator.this.provider.next(this.part)
												.getExpression().getName()).toString())))
						.detectConnectors(this.entityManager, this.persistentPropertyPath);

			case IS_EMPTY:
			case IS_NOT_EMPTY:
				if (!property.getLeafProperty().isCollection()) {
					throw new IllegalArgumentException(
							"IsEmpty / IsNotEmpty can only be used on collection properties!");
				}
				// TODO

			default:
				throw new IllegalArgumentException("Unsupported keyword " + type);
			}

		}

		private Segment lowerIfIgnoreCase(ParameterMetadata<?> metadata, Segment segment) {

			ParameterExpression<?> expression = metadata.getExpression();

			switch (this.part.shouldIgnoreCase()) {

			case ALWAYS:

				Assert.state(this.canLowerCase(expression),
						"Unable to ignore case of " + expression + " types, the property '"
								+ this.part.getProperty().getSegment() + "' must reference a String");

				return Function.of(this.entityManager.getDialect().getFunction("lower"), segment);
			case WHEN_POSSIBLE:

				if (this.canLowerCase(expression)) {
					return Function.of(this.entityManager.getDialect().getFunction("lower"), segment);
				}

			case NEVER:
			default:
				return segment;
			}

		}

		private boolean canLowerCase(ParameterExpression<?> expression) {
			return String.class.equals(expression.getJavaType());
		}

	}

}
