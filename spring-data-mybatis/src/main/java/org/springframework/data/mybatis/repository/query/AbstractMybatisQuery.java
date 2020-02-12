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
package org.springframework.data.mybatis.repository.query;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.apache.ibatis.mapping.SqlCommandType;
import org.mybatis.spring.SqlSessionTemplate;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mybatis.repository.support.ResidentParameterName;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.util.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Abstract base class to implement {@link RepositoryQuery}s.
 *
 * @author JARVIS SONG
 * @since 1.0.0
 */
public abstract class AbstractMybatisQuery implements RepositoryQuery {

	private final SqlSessionTemplate sqlSessionTemplate;

	protected final MybatisQueryMethod method;

	private final Lazy<MybatisQueryExecution> execution;

	private final Lazy<MybatisExecutor> executor;

	public AbstractMybatisQuery(SqlSessionTemplate sqlSessionTemplate, MybatisQueryMethod method) {

		Assert.notNull(sqlSessionTemplate, "SqlSessionTemplate must not be null!");
		Assert.notNull(method, "MybatisQueryMethod must not be null!");

		this.sqlSessionTemplate = sqlSessionTemplate;
		this.method = method;

		this.executor = Lazy.of(this::createExecutor);

		this.execution = Lazy.of(this::createQueryExecution);

	}

	public abstract SqlCommandType getSqlCommandType();

	public String getNamespace() {
		return this.method.getNamespace();
	}

	public String getStatementName() {
		return this.method.getStatementName();
	}

	public String getCountStatementName() {
		return this.method.getCountStatementName();
	}

	public String getStatementId() {
		return this.getNamespace() + '.' + this.getStatementName();
	}

	public String getCountStatementId() {
		return this.getNamespace() + '.' + this.getCountStatementName();
	}

	protected MybatisExecutor createExecutor() {
		MybatisExecutor executor = MybatisExecutor.create(this.sqlSessionTemplate, (statementId, accessor) -> {
			Map<String, Object> params = new HashMap<>();
			Parameters<?, ?> parameters = accessor.getParameters();
			for (Parameter p : parameters.getBindableParameters()) {
				params.put(p.isNamedParameter() ? p.getName().get()
						: ResidentParameterName.POSITION_PREFIX + (p.getIndex() + 1), accessor.getValue(p));
			}

			Sort sort = null;
			if (parameters.hasSortParameter()) {
				sort = accessor.getSort();
			}
			Integer maxResultsFromTree = null;
			if (this instanceof PartTreeMybatisQuery) {
				PartTree tree = ((PartTreeMybatisQuery) this).getTree();
				Sort treeSort = tree.getSort();
				if (null != treeSort && treeSort.isSorted()) {
					if (null != sort) {
						sort.and(treeSort);
					}
					else {
						sort = treeSort;
					}
				}

				if (tree.isLimiting()) {
					maxResultsFromTree = tree.getMaxResults();
				}
			}

			if (parameters.hasPageableParameter()) {
				Pageable pageable = accessor.getPageable();
				if (pageable.isPaged()) {

					if (null != maxResultsFromTree) {
						if (pageable.getPageSize() > maxResultsFromTree && pageable.getOffset() > 0) {
							params.put(ResidentParameterName.OFFSET,
									pageable.getOffset() - (pageable.getPageSize() - maxResultsFromTree));
						}
						else {
							params.put(ResidentParameterName.OFFSET, pageable.getOffset());
						}
						params.put(ResidentParameterName.PAGE_SIZE, maxResultsFromTree);
					}
					else {
						params.put(ResidentParameterName.PAGE_SIZE, pageable.getPageSize());
						params.put(ResidentParameterName.OFFSET, pageable.getOffset());
					}

					if (this.method.isSliceQuery()) {
						params.put(ResidentParameterName.PAGE_SIZE,
								(Integer) params.get(ResidentParameterName.PAGE_SIZE) + 1);
					}

				}
				Sort pageableSort = pageable.getSort();
				if (null != pageableSort && pageableSort.isSorted()) {
					if (null != sort) {
						sort.and(pageableSort);
					}
					else {
						sort = pageableSort;
					}
				}

			}

			if (null != sort && sort.isSorted()) {
				params.put(ResidentParameterName.SORT, sort);
			}
			return params;
		});
		return executor;
	}

	private MybatisQueryExecution createQueryExecution() {
		if (this.method.isStreamQuery()) {
			return new MybatisQueryExecution.StreamExecution();
		}
		if (this.method.isProcedureQuery()) {
			return new MybatisQueryExecution.ProcedureExecution();
		}
		if (this.method.isCollectionQuery()) {
			return new MybatisQueryExecution.CollectionExecution();
		}
		if (this.method.isSliceQuery()) {
			return new MybatisQueryExecution.SlicedExecution();
		}
		if (this.method.isPageQuery()) {
			return new MybatisQueryExecution.PagedExecution();
		}
		if (this.method.isModifyingQuery()) {
			return null;
		}
		return new MybatisQueryExecution.SingleEntityExecution();
	}

	public MybatisExecutor getExecutor() {
		return this.executor.get();
	}

	@Override
	public MybatisQueryMethod getQueryMethod() {
		return this.method;
	}

	@Override
	public Object execute(Object[] parameters) {
		return this.doExecute(this.getExecution(), parameters);
	}

	@Nullable
	private Object doExecute(MybatisQueryExecution execution, Object[] values) {

		MybatisParametersParameterAccessor accessor = new MybatisParametersParameterAccessor(
				this.method.getParameters(), values);
		Object result = execution.execute(this, accessor);

		ResultProcessor withDynamicProjection = this.method.getResultProcessor().withDynamicProjection(accessor);
		return withDynamicProjection.processResult(result, new TupleConverter(withDynamicProjection.getReturnedType()));
	}

	protected MybatisQueryExecution getExecution() {

		MybatisQueryExecution execution = this.execution.getNullable();

		if (null != execution) {
			return execution;
		}

		if (this.method.isModifyingQuery()) {
			return new MybatisQueryExecution.ModifyingExecution(this.method);
		}
		else {
			return new MybatisQueryExecution.SingleEntityExecution();
		}
	}

	public SqlSessionTemplate getSqlSessionTemplate() {
		return this.sqlSessionTemplate;
	}

	static class TupleConverter implements Converter<Object, Object> {

		private final ReturnedType type;

		TupleConverter(ReturnedType type) {

			Assert.notNull(type, "Returned type must not be null!");

			this.type = type;
		}

		@Override
		public Object convert(Object source) {

			if (!(source instanceof Tuple)) {
				return source;
			}

			Tuple tuple = (Tuple) source;
			List<TupleElement<?>> elements = tuple.getElements();

			if (elements.size() == 1) {

				Object value = tuple.get(elements.get(0));

				if (this.type.isInstance(value) || value == null) {
					return value;
				}
			}

			return new TupleBackedMap(tuple);
		}

		private static class TupleBackedMap implements Map<String, Object> {

			private static final String UNMODIFIABLE_MESSAGE = "A TupleBackedMap cannot be modified.";

			private final Tuple tuple;

			TupleBackedMap(Tuple tuple) {
				this.tuple = tuple;
			}

			@Override
			public int size() {
				return this.tuple.getElements().size();
			}

			@Override
			public boolean isEmpty() {
				return this.tuple.getElements().isEmpty();
			}

			@Override
			public boolean containsKey(Object key) {

				try {
					this.tuple.get((String) key);
					return true;
				}
				catch (IllegalArgumentException ex) {
					return false;
				}
			}

			@Override
			public boolean containsValue(Object value) {
				return Arrays.asList(this.tuple.toArray()).contains(value);
			}

			@Override
			@Nullable
			public Object get(Object key) {

				if (!(key instanceof String)) {
					return null;
				}

				try {
					return this.tuple.get((String) key);
				}
				catch (IllegalArgumentException ex) {
					return null;
				}
			}

			@Override
			public Object put(String key, Object value) {
				throw new UnsupportedOperationException(UNMODIFIABLE_MESSAGE);
			}

			@Override
			public Object remove(Object key) {
				throw new UnsupportedOperationException(UNMODIFIABLE_MESSAGE);
			}

			@Override
			public void putAll(Map<? extends String, ?> m) {
				throw new UnsupportedOperationException(UNMODIFIABLE_MESSAGE);
			}

			@Override
			public void clear() {
				throw new UnsupportedOperationException(UNMODIFIABLE_MESSAGE);
			}

			@Override
			public Set<String> keySet() {

				return this.tuple.getElements().stream() //
						.map(TupleElement::getAlias) //
						.collect(Collectors.toSet());
			}

			@Override
			public Collection<Object> values() {
				return Arrays.asList(this.tuple.toArray());
			}

			@Override
			public Set<Entry<String, Object>> entrySet() {

				return this.tuple.getElements().stream() //
						.map(e -> new HashMap.SimpleEntry<String, Object>(e.getAlias(), this.tuple.get(e))) //
						.collect(Collectors.toSet());
			}

		}

	}

}
