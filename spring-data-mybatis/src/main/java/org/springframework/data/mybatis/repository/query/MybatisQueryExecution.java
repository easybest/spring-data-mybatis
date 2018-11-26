package org.springframework.data.mybatis.repository.query;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.core.support.SurroundingTransactionDetectorMethodInterceptor;
import org.springframework.data.repository.query.Parameters;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

public abstract class MybatisQueryExecution {

	private static final ConversionService CONVERSION_SERVICE;

	static {

		ConfigurableConversionService conversionService = new DefaultConversionService();

		conversionService
				.addConverter(MybatisResultConverters.BlobToByteArrayConverter.INSTANCE);
		conversionService.removeConvertible(Collection.class, Object.class);
		potentiallyRemoveOptionalConverter(conversionService);

		CONVERSION_SERVICE = conversionService;
	}

	@Nullable
	public Object execute(AbstractMybatisQuery query, Object[] values) {

		Assert.notNull(query, "AbstractMybatisQuery must not be null!");
		Assert.notNull(values, "Values must not be null!");

		Object result;

		try {
			result = doExecute(query, values);
		}
		catch (NoResultException e) {
			return null;
		}

		if (result == null) {
			return null;
		}

		MybatisQueryMethod queryMethod = query.getQueryMethod();
		Class<?> requiredType = queryMethod.getReturnType();

		if (void.class.equals(requiredType)
				|| requiredType.isAssignableFrom(result.getClass())) {
			return result;
		}

		return CONVERSION_SERVICE.canConvert(result.getClass(), requiredType) //
				? CONVERSION_SERVICE.convert(result, requiredType) //
				: result;
	}

	@Nullable
	protected abstract Object doExecute(AbstractMybatisQuery query, Object[] values);

	static class CollectionExecution extends MybatisQueryExecution {

		@Override
		protected Object doExecute(AbstractMybatisQuery query, Object[] values) {
			if (null == values || values.length == 0) {
				return query.getSqlSessionTemplate()
						.selectList(query.getQueryMethod().getStatementId());
			}

			final int[] c = { 0 };

			MybatisParameters parameters = query.getQueryMethod().getParameters();

			Map<String, Object> params = parameters.getBindableParameters().stream()
					.filter(param -> null != values[param.getIndex()])
					.collect(Collectors.toMap(
							param -> param.getName().orElse("p" + c[0]++),
							param -> values[param.getIndex()]));

			if (parameters.hasSortParameter()) {
				params.put("__sort", values[parameters.getSortIndex()]);
			}

			return query.getSqlSessionTemplate()
					.selectList(query.getQueryMethod().getStatementId(), params);
		}

	}

	static class SlicedExecution extends MybatisQueryExecution {

		private final Parameters<?, ?> parameters;

		SlicedExecution(Parameters<?, ?> parameters) {
			this.parameters = parameters;
		}

		@Override
		protected Object doExecute(AbstractMybatisQuery query, Object[] values) {
			final int[] c = { 0 };

			MybatisParameters parameters = query.getQueryMethod().getParameters();
			Map<String, Object> params = parameters.getBindableParameters().stream()
					.filter(param -> null != values[param.getIndex()])
					.collect(Collectors.toMap(
							param -> param.getName().orElse("p" + c[0]++),
							param -> values[param.getIndex()]));

			Pageable pageable = (Pageable) values[parameters.getPageableIndex()];
			if (parameters.hasSortParameter()) {
				Sort sort = (Sort) values[parameters.getSortIndex()];
				params.put("__sort", null != sort && sort.isSorted() ? sort : null);
			}
			else if (null != pageable) {
				params.put("__sort",
						null != pageable.getSort() && pageable.getSort().isSorted()
								? pageable.getSort() : null);
			}

			if (null == pageable || pageable == Pageable.unpaged()) {
				List<Object> result = query.getSqlSessionTemplate()
						.selectList(
								query.getQueryMethod().getNamespace() + ".unpaged_"
										+ query.getQueryMethod().getStatementName(),
								params);
				return new SliceImpl(result);
			}

			int pageSize = pageable.getPageSize();
			long offset = pageable.getOffset();
			long total = query.getSqlSessionTemplate()
					.selectOne(query.getQueryMethod().getCountStatementId(), params);
			Integer limit = query.getQueryMethod().getLimitSize();
			if (null != limit) {
				total = Math.min(total, limit);
				if (limit < offset || offset > total) {
					return new PageImpl(Collections.emptyList(), pageable, total);
				}
				else if (limit >= offset && limit < (offset + pageSize)) {
					pageSize = (int) (limit - offset);
				}
			}

			params.put("__offset", offset);
			params.put("__pageSize", pageSize);
			params.put("__offsetEnd", offset + pageSize);

			List<Object> result = query.getSqlSessionTemplate()
					.selectList(query.getQueryMethod().getStatementId(), params);
			return new SliceImpl(result, pageable, total > offset);
		}

	}

	static class PagedExecution extends MybatisQueryExecution {

		private final Parameters<?, ?> parameters;

		PagedExecution(Parameters<?, ?> parameters) {
			this.parameters = parameters;
		}

		@Override
		protected Object doExecute(AbstractMybatisQuery query, Object[] values) {
			final int[] c = { 0 };

			MybatisParameters parameters = query.getQueryMethod().getParameters();
			Map<String, Object> params = parameters.getBindableParameters().stream()
					.filter(param -> null != values[param.getIndex()])
					.collect(Collectors.toMap(
							param -> param.getName().orElse("p" + c[0]++),
							param -> values[param.getIndex()]));

			Pageable pageable = (Pageable) values[parameters.getPageableIndex()];
			if (parameters.hasSortParameter()) {
				Sort sort = (Sort) values[parameters.getSortIndex()];
				params.put("__sort", null != sort && sort.isSorted() ? sort : null);
			}
			else if (null != pageable) {
				params.put("__sort",
						null != pageable.getSort() && pageable.getSort().isSorted()
								? pageable.getSort() : null);
			}

			if (null == pageable || pageable == Pageable.unpaged()) {
				List<Object> result = query.getSqlSessionTemplate()
						.selectList(
								query.getQueryMethod().getNamespace() + ".unpaged_"
										+ query.getQueryMethod().getStatementName(),
								params);
				return new PageImpl(result, pageable, null == result ? 0 : result.size());
			}

			int pageSize = pageable.getPageSize();
			long offset = pageable.getOffset();
			long total = query.getSqlSessionTemplate()
					.selectOne(query.getQueryMethod().getCountStatementId(), params);
			Integer limit = query.getQueryMethod().getLimitSize();
			if (null != limit) {
				total = Math.min(total, limit);
				if (limit < offset || offset > total) {
					return new PageImpl(Collections.emptyList(), pageable, total);
				}
				else if (limit >= offset && limit < (offset + pageSize)) {
					pageSize = (int) (limit - offset);
				}
			}

			params.put("__offset", offset);
			params.put("__pageSize", pageSize);
			params.put("__offsetEnd", offset + pageSize);

			List<Object> result = query.getSqlSessionTemplate()
					.selectList(query.getQueryMethod().getStatementId(), params);
			return new PageImpl(result, pageable, total);
		}

	}

	static class SingleEntityExecution extends MybatisQueryExecution {

		@Override
		protected Object doExecute(AbstractMybatisQuery query, Object[] values) {
			if (null == values || values.length == 0) {
				return query.getSqlSessionTemplate()
						.selectOne(query.getQueryMethod().getStatementId());
			}

			final int[] c = { 0 };

			MybatisParameters parameters = query.getQueryMethod().getParameters();

			Map<String, Object> params = parameters.getBindableParameters().stream()
					.filter(param -> null != values[param.getIndex()])
					.collect(Collectors.toMap(
							param -> param.getName().orElse("p" + c[0]++),
							param -> values[param.getIndex()]));

			if (parameters.hasSortParameter()) {
				params.put("__sort", values[parameters.getSortIndex()]);
			}

			return query.getSqlSessionTemplate()
					.selectOne(query.getQueryMethod().getStatementId(), params);
		}

	}

	static class ModifyingExecution extends MybatisQueryExecution {

		@Override
		protected Object doExecute(AbstractMybatisQuery query, Object[] values) {
			final int[] c = { 0 };
			MybatisParameters parameters = query.getQueryMethod().getParameters();
			Map<String, Object> params = parameters.getBindableParameters().stream()
					.filter(param -> null != values[param.getIndex()])
					.collect(Collectors.toMap(
							param -> param.getName().orElse("p" + c[0]++),
							param -> values[param.getIndex()]));

			int rows = query.getSqlSessionTemplate()
					.update(query.getQueryMethod().getStatementId(), params);

			return rows;
		}

	}

	static class DeleteExecution extends MybatisQueryExecution {

		@Override
		protected Object doExecute(AbstractMybatisQuery query, Object[] values) {

			final int[] c = { 0 };
			MybatisParameters parameters = query.getQueryMethod().getParameters();
			Map<String, Object> params = parameters.getBindableParameters().stream()
					.filter(param -> null != values[param.getIndex()])
					.collect(Collectors.toMap(
							param -> param.getName().orElse("p" + c[0]++),
							param -> values[param.getIndex()]));

			boolean collectionQuery = query.getQueryMethod().isCollectionQuery();

			Object result = null;
			if (collectionQuery) {
				result = query.getSqlSessionTemplate()
						.selectList(
								query.getQueryMethod().getNamespace() + ".query_"
										+ query.getQueryMethod().getStatementName(),
								params);
			}

			int rows = query.getSqlSessionTemplate()
					.delete(query.getQueryMethod().getStatementId(), params);
			if (!collectionQuery) {
				return rows;
			}
			return result;
		}

	}

	static class ExistsExecution extends MybatisQueryExecution {

		@Override
		protected Object doExecute(AbstractMybatisQuery query, Object[] values) {
			if (null == values || values.length == 0) {
				return ((long) query.getSqlSessionTemplate()
						.selectOne(query.getQueryMethod().getStatementId())) > 0;
			}

			final int[] c = { 0 };

			MybatisParameters parameters = query.getQueryMethod().getParameters();

			Map<String, Object> params = parameters.getBindableParameters().stream()
					.filter(param -> null != values[param.getIndex()])
					.collect(Collectors.toMap(
							param -> param.getName().orElse("p" + c[0]++),
							param -> values[param.getIndex()]));

			return ((long) query.getSqlSessionTemplate()
					.selectOne(query.getQueryMethod().getStatementId(), params)) > 0;
		}

	}

	static class StreamExecution extends MybatisQueryExecution {

		private static final String NO_SURROUNDING_TRANSACTION = "You're trying to execute a streaming query method without a surrounding transaction that keeps the connection open so that the Stream can actually be consumed. Make sure the code consuming the stream uses @Transactional or any other way of declaring a (read-only) transaction.";

		private static Method streamMethod = ReflectionUtils.findMethod(Query.class,
				"getResultStream");

		@Override
		protected Object doExecute(AbstractMybatisQuery query, Object[] values) {

			if (!SurroundingTransactionDetectorMethodInterceptor.INSTANCE
					.isSurroundingTransactionActive()) {
				throw new InvalidDataAccessApiUsageException(NO_SURROUNDING_TRANSACTION);
			}

			return null;
		}

	}

	public static void potentiallyRemoveOptionalConverter(
			ConfigurableConversionService conversionService) {

		ClassLoader classLoader = MybatisQueryExecution.class.getClassLoader();

		if (ClassUtils.isPresent("java.util.Optional", classLoader)) {

			try {

				Class<?> optionalType = ClassUtils.forName("java.util.Optional",
						classLoader);
				conversionService.removeConvertible(Object.class, optionalType);

			}
			catch (ClassNotFoundException | LinkageError o_O) {
			}
		}
	}

}
