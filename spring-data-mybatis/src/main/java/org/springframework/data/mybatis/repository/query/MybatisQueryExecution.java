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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.persistence.NoResultException;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mybatis.repository.support.ResidentStatementName;
import org.springframework.data.repository.core.support.SurroundingTransactionDetectorMethodInterceptor;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Set of classes to contain query execution strategies.
 *
 * @author JARVIS SONG
 * @since 1.0.0
 */
public abstract class MybatisQueryExecution {

	private static final ConversionService CONVERSION_SERVICE;

	static {

		ConfigurableConversionService conversionService = new DefaultConversionService();

		conversionService.addConverter(MybatisResultConverters.BlobToByteArrayConverter.INSTANCE);
		conversionService.removeConvertible(Collection.class, Object.class);
		potentiallyRemoveOptionalConverter(conversionService);

		CONVERSION_SERVICE = conversionService;
	}

	@Nullable
	public Object execute(AbstractMybatisQuery query, MybatisParametersParameterAccessor accessor) {

		Assert.notNull(query, "AbstractMybatisQuery must not be null!");
		Assert.notNull(accessor, "MybatisParametersParameterAccessor must not be null!");
		Object result;

		try {
			result = doExecute(query, accessor);
		}
		catch (NoResultException ex) {
			return null;
		}

		if (result == null) {
			return null;
		}

		MybatisQueryMethod queryMethod = query.getQueryMethod();
		Class<?> requiredType = queryMethod.getReturnType();

		if (void.class.equals(requiredType) || requiredType.isAssignableFrom(result.getClass())) {
			return result;
		}

		return CONVERSION_SERVICE.canConvert(result.getClass(), requiredType) //
				? CONVERSION_SERVICE.convert(result, requiredType) //
				: result;
	}

	@Nullable
	protected abstract Object doExecute(AbstractMybatisQuery query, MybatisParametersParameterAccessor accessor);

	/**
	 * Removes the converter being able to convert any object into an {@link Optional}
	 * from the given {@link ConversionService} in case we're running on Java 8.
	 * @param conversionService must not be {@literal null}.
	 */
	static void potentiallyRemoveOptionalConverter(ConfigurableConversionService conversionService) {

		ClassLoader classLoader = MybatisQueryExecution.class.getClassLoader();

		if (ClassUtils.isPresent("java.util.Optional", classLoader)) {

			try {

				Class<?> optionalType = ClassUtils.forName("java.util.Optional", classLoader);
				conversionService.removeConvertible(Object.class, optionalType);

			}
			catch (ClassNotFoundException | LinkageError o_O) {
			}
		}
	}

	static class SingleEntityExecution extends MybatisQueryExecution {

		@Override
		protected Object doExecute(AbstractMybatisQuery query, MybatisParametersParameterAccessor accessor) {
			return query.getExecutor().getSingleResult(query.getQueryMethod().getStatementId(), accessor);
		}

	}

	static class DeleteExecution extends MybatisQueryExecution {

		@Override
		protected Object doExecute(AbstractMybatisQuery query, MybatisParametersParameterAccessor accessor) {
			List result = null;
			if (query.getQueryMethod().isCollectionQuery()) {
				result = query.getExecutor().getResultList(query.getQueryMethod().getNamespace() + '.'
						+ ResidentStatementName.QUERY_PREFIX + query.getQueryMethod().getStatementName(), accessor);
			}
			int rows = query.getExecutor().executeDelete(query.getQueryMethod().getStatementId(), accessor);
			if (query.getQueryMethod().isCollectionQuery()) {
				return result;
			}
			return rows;
		}

	}

	static class ExistsExecution extends MybatisQueryExecution {

		@Override
		protected Object doExecute(AbstractMybatisQuery query, MybatisParametersParameterAccessor accessor) {
			Long count = (Long) query.getExecutor().getSingleResult(query.getQueryMethod().getStatementId(), accessor);
			return count > 0;
		}

	}

	static class ModifyingExecution extends MybatisQueryExecution {

		ModifyingExecution(MybatisQueryMethod method) {
			Class<?> returnType = method.getReturnType();

			boolean isVoid = void.class.equals(returnType) || Void.class.equals(returnType);
			boolean isInt = int.class.equals(returnType) || Integer.class.equals(returnType);

			Assert.isTrue(isInt || isVoid, "Modifying queries can only use void or int/Integer as return type!");

		}

		@Override
		protected Object doExecute(AbstractMybatisQuery query, MybatisParametersParameterAccessor accessor) {

			return query.getExecutor().executeUpdate(query.getQueryMethod().getStatementId(), accessor);

		}

	}

	static class StreamExecution extends MybatisQueryExecution {

		private static final String NO_SURROUNDING_TRANSACTION = "You're trying to execute a streaming query method without a surrounding transaction that keeps the connection open so that the Stream can actually be consumed. Make sure the code consuming the stream uses @Transactional or any other way of declaring a (read-only) transaction.";

		@Override
		protected Object doExecute(AbstractMybatisQuery query, MybatisParametersParameterAccessor accessor) {

			if (!SurroundingTransactionDetectorMethodInterceptor.INSTANCE.isSurroundingTransactionActive()) {
				throw new InvalidDataAccessApiUsageException(NO_SURROUNDING_TRANSACTION);
			}

			return null;
		}

	}

	static class ProcedureExecution extends MybatisQueryExecution {

		@Override
		protected Object doExecute(AbstractMybatisQuery query, MybatisParametersParameterAccessor accessor) {
			return null;
		}

	}

	static class CollectionExecution extends MybatisQueryExecution {

		@Override
		protected Object doExecute(AbstractMybatisQuery query, MybatisParametersParameterAccessor accessor) {
			return query.getExecutor().getResultList(query.getQueryMethod().getStatementId(), accessor);
		}

	}

	static class SlicedExecution extends MybatisQueryExecution {

		@Override
		protected Object doExecute(AbstractMybatisQuery query, MybatisParametersParameterAccessor accessor) {
			// Pageable pageable = accessor.getPageable();
			// Query createQuery = query.createQuery(accessor);
			//
			// int pageSize = 0;
			// if (pageable.isPaged()) {
			//
			// pageSize = pageable.getPageSize();
			// createQuery.setMaxResults(pageSize + 1);
			// }
			//
			// List<Object> resultList = createQuery.getResultList();
			//
			// boolean hasNext = pageable.isPaged() && resultList.size() > pageSize;
			//
			// return new SliceImpl<>(hasNext ? resultList.subList(0, pageSize) :
			// resultList, pageable, hasNext);
			return null;
		}

	}

	static class PagedExecution extends MybatisQueryExecution {

		@Override
		protected Object doExecute(AbstractMybatisQuery query, MybatisParametersParameterAccessor accessor) {
			List content;
			Pageable pageable = accessor.getPageable();
			if (null == pageable || pageable.isUnpaged()) {
				content = query.getExecutor()
						.getResultList(query.getQueryMethod().getNamespace() + '.'
								+ ResidentStatementName.UNPAGED_PREFIX + query.getQueryMethod().getStatementName(),
								accessor);
			}
			else {
				content = query.getExecutor().getResultList(query.getQueryMethod().getStatementId(), accessor);
			}
			return PageableExecutionUtils.getPage(content, pageable, () -> count(query, accessor));
		}

		private long count(AbstractMybatisQuery query, MybatisParametersParameterAccessor accessor) {

			Long total = (Long) query.getExecutor().getSingleResult(//
					query.getQueryMethod().getNamespace() + '.' + ResidentStatementName.COUNT_PREFIX
							+ query.getQueryMethod().getStatementName(),
					accessor);
			return total;
		}

	}

}
