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

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.criteria.ParameterExpression;

import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * .
 *
 * @author JARVIS SONG
 */
interface QueryParameterSetter {

	void setParameter(BindableQuery query, MybatisParametersParameterAccessor accessor, ErrorHandling errorHandling);

	QueryParameterSetter NOOP = (query, values, errorHandling) -> {
	};

	class NamedOrIndexedQueryParameterSetter implements QueryParameterSetter {

		private final Function<MybatisParametersParameterAccessor, Object> valueExtractor;

		private final Parameter<?> parameter;

		private final @Nullable TemporalType temporalType;

		NamedOrIndexedQueryParameterSetter(Function<MybatisParametersParameterAccessor, Object> valueExtractor,
				Parameter<?> parameter, @Nullable TemporalType temporalType) {

			Assert.notNull(valueExtractor, "ValueExtractor must not be null!");

			this.valueExtractor = valueExtractor;
			this.parameter = parameter;
			this.temporalType = temporalType;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void setParameter(BindableQuery query, MybatisParametersParameterAccessor accessor,
				ErrorHandling errorHandling) {

			Object value = this.valueExtractor.apply(accessor);

			if (this.temporalType != null) {

				// One would think we can simply use parameter to identify the parameter
				// we want to set.
				// But that does not work with list valued parameters. At least Hibernate
				// tries to bind them by name.
				// TODO: move to using setParameter(Parameter, value) when
				// https://hibernate.atlassian.net/browse/HHH-11870 is
				// fixed.

				if (this.parameter instanceof ParameterExpression) {
					errorHandling.execute(() -> query.setParameter((Parameter<Date>) this.parameter, (Date) value,
							this.temporalType));
				}
				else if (query.hasNamedParameters() && this.parameter.getName() != null) {
					errorHandling.execute(
							() -> query.setParameter(this.parameter.getName(), (Date) value, this.temporalType));
				}
				else {

					Integer position = this.parameter.getPosition();

					if ((position != null) //
							&& ((query.getParameters().size() >= this.parameter.getPosition()) //
									|| query.registerExcessParameters() //
									|| (errorHandling == ErrorHandling.LENIENT))) {

						errorHandling.execute(() -> query.setParameter(this.parameter.getPosition(), (Date) value,
								this.temporalType));
					}
				}

			}
			else {

				if (this.parameter instanceof ParameterExpression) {
					errorHandling.execute(() -> query.setParameter((Parameter<Object>) this.parameter, value));
				}
				else if (query.hasNamedParameters() && this.parameter.getName() != null) {
					errorHandling.execute(() -> query.setParameter(this.parameter.getName(), value));

				}
				else {

					Integer position = this.parameter.getPosition();

					if (position != null //
							&& (query.getParameters().size() >= position //
									|| errorHandling == ErrorHandling.LENIENT //
									|| query.registerExcessParameters())) {
						errorHandling.execute(() -> query.setParameter(position, value));
					}
				}
			}
		}

	}

	@Slf4j
	enum ErrorHandling {

		STRICT {

			@Override
			public void execute(Runnable block) {
				block.run();
			}
		},

		LENIENT {

			@Override
			public void execute(Runnable block) {

				try {
					block.run();
				}
				catch (RuntimeException rex) {
					log.info("Silently ignoring", rex);
				}
			}
		};

		abstract void execute(Runnable block);

	}

	class QueryMetadataCache {

		private Map<String, QueryMetadata> cache = Collections.emptyMap();

		public QueryMetadata getMetadata(String cacheKey, Query query) {

			QueryMetadata queryMetadata = this.cache.get(cacheKey);

			if (null == queryMetadata) {

				queryMetadata = new QueryMetadata(query);

				Map<String, QueryMetadata> cache;

				if (this.cache.isEmpty()) {
					cache = Collections.singletonMap(cacheKey, queryMetadata);
				}
				else {
					cache = new HashMap<>(this.cache);
					cache.put(cacheKey, queryMetadata);
				}

				synchronized (this) {
					this.cache = cache;
				}
			}

			return queryMetadata;
		}

	}

	@Slf4j
	class QueryMetadata {

		private final boolean namedParameters;

		private final Set<Parameter<?>> parameters;

		private final boolean registerExcessParameters;

		QueryMetadata(Query query) {

			this.namedParameters = QueryUtils.hasNamedParameter(query);
			this.parameters = query.getParameters();

			// Since EclipseLink doesn't reliably report whether a query has parameters
			// we simply try to set the parameters and ignore possible failures.
			// this is relevant for native queries with SpEL expressions, where the method
			// parameters don't have to match the
			// parameters in the query.
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=521915

			this.registerExcessParameters = query.getParameters().size() == 0
					&& unwrapClass(query).getName().startsWith("org.eclipse");
		}

		QueryMetadata(QueryMetadata metadata) {

			this.namedParameters = metadata.namedParameters;
			this.parameters = metadata.parameters;
			this.registerExcessParameters = metadata.registerExcessParameters;
		}

		public BindableQuery withQuery(Query query) {
			return new BindableQuery(this, query);
		}

		public Set<Parameter<?>> getParameters() {
			return this.parameters;
		}

		public boolean hasNamedParameters() {
			return this.namedParameters;
		}

		public boolean registerExcessParameters() {
			return this.registerExcessParameters;
		}

		private static Class<?> unwrapClass(Query query) {

			Class<? extends Query> queryType = query.getClass();

			try {

				return Proxy.isProxyClass(queryType) //
						? query.unwrap(null).getClass() //
						: queryType;

			}
			catch (RuntimeException ex) {

				log.warn("Failed to unwrap actual class for Query proxy.", ex);

				return queryType;
			}
		}

	}

	class BindableQuery extends QueryMetadata {

		private final Query query;

		private final Query unwrapped;

		BindableQuery(QueryMetadata metadata, Query query) {
			super(metadata);
			this.query = query;
			this.unwrapped = Proxy.isProxyClass(query.getClass()) ? query.unwrap(null) : query;
		}

		private BindableQuery(Query query) {
			super(query);
			this.query = query;
			this.unwrapped = Proxy.isProxyClass(query.getClass()) ? query.unwrap(null) : query;
		}

		public static BindableQuery from(Query query) {
			return new BindableQuery(query);
		}

		public Query getQuery() {
			return this.query;
		}

		public <T> Query setParameter(Parameter<T> param, T value) {
			return this.unwrapped.setParameter(param, value);
		}

		public Query setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
			return this.unwrapped.setParameter(param, value, temporalType);
		}

		public Query setParameter(String name, Object value) {
			return this.unwrapped.setParameter(name, value);
		}

		public Query setParameter(String name, Date value, TemporalType temporalType) {
			return this.query.setParameter(name, value, temporalType);
		}

		public Query setParameter(int position, Object value) {
			return this.unwrapped.setParameter(position, value);
		}

		public Query setParameter(int position, Date value, TemporalType temporalType) {
			return this.unwrapped.setParameter(position, value, temporalType);
		}

	}

}
