package org.springframework.data.mybatis.repository.query;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import javax.persistence.NoResultException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Jarvis Song
 */
public abstract class MyBatisQueryExecution {

	private static final ConversionService CONVERSION_SERVICE;

	static {

		ConfigurableConversionService conversionService = new DefaultConversionService();

		conversionService.addConverter(MyBatisResultConverters.BlobToByteArrayConverter.INSTANCE);
		conversionService.removeConvertible(Collection.class, Object.class);
		potentiallyRemoveOptionalConverter(conversionService);

		CONVERSION_SERVICE = conversionService;
	}

	@Nullable
	public Object execute(AbstractMyBatisQuery query, Object[] values) {
		Assert.notNull(query, "AbstractMyBatisQuery must not be null!");
		Assert.notNull(values, "Values must not be null!");

		Object result;

		try {
			result = doExecute(query, values);
		} catch (NoResultException e) {
			return null;
		}

		if (result == null) {
			return null;
		}

		MyBatisQueryMethod queryMethod = query.getQueryMethod();
		Class<?> requiredType = queryMethod.getReturnType();

		if (void.class.equals(requiredType) || requiredType.isAssignableFrom(result.getClass())) {
			return result;
		}

		return CONVERSION_SERVICE.canConvert(result.getClass(), requiredType)
				? CONVERSION_SERVICE.convert(result, requiredType)
				: result;
	}

	@Nullable
	protected abstract Object doExecute(AbstractMyBatisQuery query, Object[] values);

	static class CollectionExecution extends MyBatisQueryExecution {
		@Override
		protected Object doExecute(AbstractMyBatisQuery query, final Object[] values) {

			if (null == values || values.length == 0) {
				return query.getTemplate().selectList(query.getQueryMethod().getStatementId());
			}

			final int[] c = { 0 };

			MyBatisParameters parameters = query.getQueryMethod().getParameters();

			Map<String, Object> params = parameters.getBindableParameters().stream()
					.filter(param -> null != values[param.getIndex()])
					.collect(Collectors.toMap(param -> param.getName().orElse("p" + c[0]++), param -> values[param.getIndex()]));

			if (parameters.hasSortParameter()) {
				params.put("_sorts", values[parameters.getSortIndex()]);
			}

			return query.getTemplate().selectList(query.getQueryMethod().getStatementId(), params);
		}
	}

	static class SlicedExecution extends MyBatisQueryExecution {

		@Override
		protected Object doExecute(AbstractMyBatisQuery query, Object[] values) {

			final int[] c = { 0 };

			MyBatisParameters parameters = query.getQueryMethod().getParameters();
			Map<String, Object> params = parameters.getBindableParameters().stream()
					.filter(param -> null != values[param.getIndex()])
					.collect(Collectors.toMap(param -> param.getName().orElse("p" + c[0]++), param -> values[param.getIndex()]));

			Pageable pageable = (Pageable) values[parameters.getPageableIndex()];
			if (parameters.hasSortParameter()) {
				Sort sort = (Sort) values[parameters.getSortIndex()];
				params.put("_sorts", null != sort && sort.isSorted() ? sort : null);
			} else if (null != pageable) {
				params.put("_sorts", null != pageable.getSort() && pageable.getSort().isSorted() ? pageable.getSort() : null);
			}

			if (null == pageable || pageable == Pageable.unpaged()) {
				List<Object> result = query.getTemplate().selectList(
						query.getQueryMethod().getNamespace() + ".unpaged_" + query.getQueryMethod().getStatementName(), params);
				return new SliceImpl(result);
			}

			int pageSize = pageable.getPageSize();
			long offset = pageable.getOffset();
			long total = query.getTemplate().selectOne(query.getQueryMethod().getCountStatementId(), params);
			Integer limit = query.getQueryMethod().getLimitSize();
			if (null != limit) {
				total = Math.min(total, limit);
				if (limit < offset || offset > total) {
					return new PageImpl(Collections.emptyList(), pageable, total);
				} else if (limit >= offset && limit < (offset + pageSize)) {
					pageSize = (int) (limit - offset);
				}
			}

			params.put("offset", offset);
			params.put("pageSize", pageSize);
			params.put("offsetEnd", offset + pageSize);

			List<Object> result = query.getTemplate().selectList(query.getQueryMethod().getStatementId(), params);
			return new SliceImpl(result, pageable, total > offset);

		}
	}

	static class PagedExecution extends MyBatisQueryExecution {
		@Override
		protected Object doExecute(AbstractMyBatisQuery query, Object[] values) {
			final int[] c = { 0 };

			MyBatisParameters parameters = query.getQueryMethod().getParameters();
			Map<String, Object> params = parameters.getBindableParameters().stream()
					.filter(param -> null != values[param.getIndex()])
					.collect(Collectors.toMap(param -> param.getName().orElse("p" + c[0]++), param -> values[param.getIndex()]));

			Pageable pageable = (Pageable) values[parameters.getPageableIndex()];
			if (parameters.hasSortParameter()) {
				Sort sort = (Sort) values[parameters.getSortIndex()];
				params.put("_sorts", null != sort && sort.isSorted() ? sort : null);
			} else if (null != pageable) {
				params.put("_sorts", null != pageable.getSort() && pageable.getSort().isSorted() ? pageable.getSort() : null);
			}

			if (null == pageable || pageable == Pageable.unpaged()) {
				List<Object> result = query.getTemplate().selectList(
						query.getQueryMethod().getNamespace() + ".unpaged_" + query.getQueryMethod().getStatementName(), params);
				return new PageImpl(result, pageable, null == result ? 0 : result.size());
			}

			int pageSize = pageable.getPageSize();
			long offset = pageable.getOffset();
			long total = query.getTemplate().selectOne(query.getQueryMethod().getCountStatementId(), params);
			Integer limit = query.getQueryMethod().getLimitSize();
			if (null != limit) {
				total = Math.min(total, limit);
				if (limit < offset || offset > total) {
					return new PageImpl(Collections.emptyList(), pageable, total);
				} else if (limit >= offset && limit < (offset + pageSize)) {
					pageSize = (int) (limit - offset);
				}
			}

			params.put("offset", offset);
			params.put("pageSize", pageSize);
			params.put("offsetEnd", offset + pageSize);

			List<Object> result = query.getTemplate().selectList(query.getQueryMethod().getStatementId(), params);
			return new PageImpl(result, pageable, total);
		}
	}

	static class StreamExecution extends MyBatisQueryExecution {

		@Override
		protected Object doExecute(AbstractMyBatisQuery query, Object[] values) {
			return null;
		}
	}

	static class SingleEntityExecution extends MyBatisQueryExecution {

		@Override
		protected Object doExecute(AbstractMyBatisQuery query, Object[] values) {

			if (null == values || values.length == 0) {
				return query.getTemplate().selectOne(query.getQueryMethod().getStatementId());
			}

			final int[] c = { 0 };

			MyBatisParameters parameters = query.getQueryMethod().getParameters();

			Map<String, Object> params = parameters.getBindableParameters().stream()
					.filter(param -> null != values[param.getIndex()])
					.collect(Collectors.toMap(param -> param.getName().orElse("p" + c[0]++), param -> values[param.getIndex()]));

			if (parameters.hasSortParameter()) {
				params.put("_sorts", values[parameters.getSortIndex()]);
			}

			return query.getTemplate().selectOne(query.getQueryMethod().getStatementId(), params);
		}
	}

	static class ModifyingExecution extends MyBatisQueryExecution {

		@Override
		protected Object doExecute(AbstractMyBatisQuery query, Object[] values) {
			return null;
		}
	}

	static class DeleteExecution extends MyBatisQueryExecution {
		@Override
		protected Object doExecute(AbstractMyBatisQuery query, Object[] values) {
			final int[] c = { 0 };
			MyBatisParameters parameters = query.getQueryMethod().getParameters();
			Map<String, Object> params = parameters.getBindableParameters().stream()
					.filter(param -> null != values[param.getIndex()])
					.collect(Collectors.toMap(param -> param.getName().orElse("p" + c[0]++), param -> values[param.getIndex()]));

			boolean collectionQuery = query.getQueryMethod().isCollectionQuery();

			Object result = null;
			if (collectionQuery) {
				result = query.getTemplate().selectList(
						query.getQueryMethod().getNamespace() + ".query_" + query.getQueryMethod().getStatementName(), params);
			}

			int rows = query.getTemplate().delete(query.getQueryMethod().getStatementId(), params);
			if (!collectionQuery) {
				return rows;
			}
			return result;
		}
	}

	static class ExistsExecution extends MyBatisQueryExecution {
		@Override
		protected Object doExecute(AbstractMyBatisQuery query, Object[] values) {

			if (null == values || values.length == 0) {
				return ((long) query.getTemplate().selectOne(query.getQueryMethod().getStatementId())) > 0;
			}

			final int[] c = { 0 };

			MyBatisParameters parameters = query.getQueryMethod().getParameters();

			Map<String, Object> params = parameters.getBindableParameters().stream()
					.filter(param -> null != values[param.getIndex()])
					.collect(Collectors.toMap(param -> param.getName().orElse("p" + c[0]++), param -> values[param.getIndex()]));

			return ((long) query.getTemplate().selectOne(query.getQueryMethod().getStatementId(), params)) > 0;

		}
	}

	static class ProcedureExecution extends MyBatisQueryExecution {
		@Override
		protected Object doExecute(AbstractMyBatisQuery query, Object[] values) {
			return null;
		}
	}

	/**
	 * Removes the converter being able to convert any object into an {@link Optional} from the given
	 * {@link ConversionService} in case we're running on Java 8.
	 *
	 * @param conversionService must not be {@literal null}.
	 */
	public static void potentiallyRemoveOptionalConverter(ConfigurableConversionService conversionService) {

		ClassLoader classLoader = MyBatisQueryExecution.class.getClassLoader();

		if (ClassUtils.isPresent("java.util.Optional", classLoader)) {

			try {

				Class<?> optionalType = ClassUtils.forName("java.util.Optional", classLoader);
				conversionService.removeConvertible(Object.class, optionalType);

			} catch (ClassNotFoundException | LinkageError o_O) {}
		}
	}

}
