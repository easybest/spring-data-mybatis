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

import java.util.Collection;
import java.util.List;

import javax.persistence.NoResultException;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.repository.core.support.SurroundingTransactionDetectorMethodInterceptor;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import io.easybest.mybatis.repository.support.ResidentStatementName;

import static io.easybest.mybatis.repository.support.ResidentStatementName.QUERY_PREFIX;

/**
 * .
 *
 * @author Jarvis Song
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
			result = this.doExecute(query, accessor);
		}
		catch (NoResultException ex) {
			return null;
		}

		if (result == null) {
			return null;
		}

		MybatisQueryMethod queryMethod = query.getQueryMethod();
		Class<?> requiredType = queryMethod.getReturnType();

		if (ClassUtils.isAssignable(requiredType, void.class) || ClassUtils.isAssignableValue(requiredType, result)) {
			return result;
		}

		return CONVERSION_SERVICE.canConvert(result.getClass(), requiredType) //
				? CONVERSION_SERVICE.convert(result, requiredType) //
				: result;

	}

	public static void potentiallyRemoveOptionalConverter(ConfigurableConversionService conversionService) {

		ClassLoader classLoader = MybatisQueryExecution.class.getClassLoader();

		if (ClassUtils.isPresent("java.util.Optional", classLoader)) {

			try {

				Class<?> optionalType = ClassUtils.forName("java.util.Optional", classLoader);
				conversionService.removeConvertible(Object.class, optionalType);

			}
			catch (ClassNotFoundException | LinkageError ex) {
				// ignore
			}
		}
	}

	@Nullable
	protected abstract Object doExecute(AbstractMybatisQuery query, MybatisParametersParameterAccessor accessor);

	static class SingleEntityExecution extends MybatisQueryExecution {

		@Override
		protected Object doExecute(AbstractMybatisQuery query, MybatisParametersParameterAccessor accessor) {

			return query.parameterCallback().andThen(context -> query.getEntityManager().getSqlSessionTemplate()
					.selectOne(query.getQueryMethod().getStatementId(), context)).apply(accessor);
		}

	}

	static class ModifyingExecution extends MybatisQueryExecution {

		public ModifyingExecution(MybatisQueryMethod method) {

			Class<?> returnType = method.getReturnType();

			boolean isVoid = void.class.equals(returnType) || Void.class.equals(returnType);
			boolean isInt = int.class.equals(returnType) || Integer.class.equals(returnType);

			Assert.isTrue(isInt || isVoid, "Modifying queries can only use void or int/Integer as return type!");
		}

		@Override
		protected Object doExecute(AbstractMybatisQuery query, MybatisParametersParameterAccessor accessor) {

			return query.parameterCallback().andThen(context -> query.getEntityManager().getSqlSessionTemplate()
					.update(query.getQueryMethod().getStatementId(), context)).apply(accessor);
		}

	}

	static class StreamExecution extends MybatisQueryExecution {

		private static final String NO_SURROUNDING_TRANSACTION = "You're trying to execute a streaming query method without a surrounding transaction that keeps the connection open so that the Stream can actually be consumed. Make sure the code consuming the stream uses @Transactional or any other way of declaring a (read-only) transaction.";

		@Override
		protected Object doExecute(AbstractMybatisQuery query, MybatisParametersParameterAccessor accessor) {

			if (!SurroundingTransactionDetectorMethodInterceptor.INSTANCE.isSurroundingTransactionActive()) {
				throw new InvalidDataAccessApiUsageException(NO_SURROUNDING_TRANSACTION);
			}

			return query.parameterCallback()
					.andThen(context -> query.getEntityManager().getSqlSessionTemplate()
							.selectList(query.getQueryMethod().getStatementId(), context))
					.andThen(Collection::stream).apply(accessor);
		}

	}

	static class ProcedureExecution extends MybatisQueryExecution {

		private static final String NO_SURROUNDING_TRANSACTION = "You're trying to execute a @Procedure method without a surrounding transaction that keeps the connection open so that the ResultSet can actually be consumed. Make sure the consumer code uses @Transactional or any other way of declaring a (read-only) transaction.";

		@Override
		protected Object doExecute(AbstractMybatisQuery query, MybatisParametersParameterAccessor accessor) {

			if (!SurroundingTransactionDetectorMethodInterceptor.INSTANCE.isSurroundingTransactionActive()) {
				throw new InvalidDataAccessApiUsageException(NO_SURROUNDING_TRANSACTION);
			}

			// TODO
			return null;
		}

	}

	static class ExistsExecution extends MybatisQueryExecution {

		@Override
		protected Object doExecute(AbstractMybatisQuery query, MybatisParametersParameterAccessor accessor) {

			return query.parameterCallback().andThen(context -> query.getEntityManager().getSqlSessionTemplate()
					.selectOne(query.getQueryMethod().getStatementId(), context)).apply(accessor);
		}

	}

	static class DeleteExecution extends MybatisQueryExecution {

		@Override
		protected Object doExecute(AbstractMybatisQuery query, MybatisParametersParameterAccessor accessor) {

			MybatisQueryMethod method = query.getQueryMethod();

			List<?> result = null;

			if (method.isCollectionQuery()) {
				result = query.parameterCallback()
						.andThen(context -> query.getEntityManager().getSqlSessionTemplate().selectList(
								method.getNamespace() + '.' + QUERY_PREFIX + method.getStatementName(), context))
						.apply(accessor);
			}

			int affectRows = query.parameterCallback().andThen(context -> query.getEntityManager()
					.getSqlSessionTemplate().delete(method.getStatementId(), context)).apply(accessor);

			// TODO logic delete

			return method.isCollectionQuery() ? result : affectRows;
		}

	}

	static class PagedExecution extends MybatisQueryExecution {

		@Override
		protected Object doExecute(AbstractMybatisQuery query, MybatisParametersParameterAccessor accessor) {

			Pageable pageable = accessor.getPageable();
			MybatisQueryMethod method = query.getQueryMethod();
			return query.parameterCallback()
					.andThen(context -> query.getEntityManager().getSqlSessionTemplate()
							.selectList(pageable.isUnpaged() ? (method.getNamespace() + '.'
									+ ResidentStatementName.UNPAGED_PREFIX + method.getStatementName())
									: method.getStatementId(), context))
					.andThen(content -> PageableExecutionUtils.getPage(content, pageable,
							() -> this.count(query, accessor)))
					.apply(accessor);
		}

		private long count(AbstractMybatisQuery query, MybatisParametersParameterAccessor accessor) {

			MybatisQueryMethod method = query.getQueryMethod();
			return (long) query.parameterCallback()
					.andThen(context -> query.getEntityManager().getSqlSessionTemplate()
							.selectOne(method.getNamespace() + '.' + method.getCountStatementName(), context))
					.apply(accessor);
		}

	}

	static class SlicedExecution extends MybatisQueryExecution {

		@Override
		protected Object doExecute(AbstractMybatisQuery query, MybatisParametersParameterAccessor accessor) {

			Pageable pageable = accessor.getPageable();
			MybatisQueryMethod method = query.getQueryMethod();

			List<Object> content = query.parameterCallback().andThen(context -> {

				if (pageable.isPaged()) {
					context.getPageable().setSize(pageable.getPageSize() + 1);
				}

				return query.getEntityManager().getSqlSessionTemplate()
						.selectList(pageable.isUnpaged() ? (method.getNamespace() + '.'
								+ ResidentStatementName.UNPAGED_PREFIX + method.getStatementName())
								: method.getStatementId(), context);
			}).apply(accessor);

			int pageSize = pageable.isPaged() ? pageable.getPageSize() : 0;
			boolean hasNext = pageable.isPaged() && content.size() > pageSize;

			return new SliceImpl<>(hasNext ? content.subList(0, pageSize) : content, pageable, hasNext);
		}

	}

	static class CollectionExecution extends MybatisQueryExecution {

		@Override
		protected Object doExecute(AbstractMybatisQuery query, MybatisParametersParameterAccessor accessor) {

			return query.parameterCallback().andThen(context -> query.getEntityManager().getSqlSessionTemplate()
					.selectList(query.getQueryMethod().getStatementId(), context)).apply(accessor);
		}

	}

}
