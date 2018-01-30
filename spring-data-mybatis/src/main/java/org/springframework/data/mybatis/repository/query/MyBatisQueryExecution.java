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
import org.springframework.util.CollectionUtils;

import javax.persistence.NoResultException;
import java.util.Collection;
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
					.collect(Collectors.toMap(param -> param.getName().orElse("p" + c[0]++), param -> values[param.getIndex()]));

			Pageable pageable = (Pageable) values[parameters.getPageableIndex()];
			if (parameters.hasSortParameter()) {
				params.put("_sorts", values[parameters.getSortIndex()]);
			} else {
				params.put("_sorts", pageable.getSort());
			}
			params.put("offset", pageable.getOffset());
			params.put("pageSize", pageable.getPageSize() + 1);
			params.put("offsetEnd", pageable.getOffset() + pageable.getPageSize());
			List<Object> resultList = query.getTemplate().selectList(query.getQueryMethod().getStatementId(), params);

			int pageSize = pageable.getPageSize();
			boolean hasNext = resultList.size() > pageSize;

			return new SliceImpl<>(hasNext ? resultList.subList(0, pageSize) : resultList, pageable, hasNext);

		}
	}

	static class PagedExecution extends MyBatisQueryExecution {

		@Override
		protected Object doExecute(AbstractMyBatisQuery query, Object[] values) {
			final int[] c = { 0 };

			MyBatisParameters parameters = query.getQueryMethod().getParameters();
			Map<String, Object> params = parameters.getBindableParameters().stream()
					.collect(Collectors.toMap(param -> param.getName().orElse("p" + c[0]++), param -> values[param.getIndex()]));

			Pageable pageable = (Pageable) values[parameters.getPageableIndex()];
			if (parameters.hasSortParameter()) {
				Sort sort = (Sort) values[parameters.getSortIndex()];
				params.put("_sorts", null != sort && sort.isSorted() ? sort : null);
			} else {
				params.put("_sorts", null != pageable.getSort() && pageable.getSort().isSorted() ? pageable.getSort() : null);
			}
			params.put("offset", null == pageable ? 0 : pageable.getOffset());
			params.put("pageSize", null == pageable ? Integer.MAX_VALUE : pageable.getPageSize());
			params.put("offsetEnd", null == pageable ? Integer.MAX_VALUE : pageable.getOffset() + pageable.getPageSize());
			List<Object> result = query.getTemplate().selectList(query.getQueryMethod().getStatementId(), params);
			if (null == pageable) {
				return new PageImpl(result);
			}
			long total = calculateTotal(pageable, result);
			if (total < 0) {
				total = query.getTemplate().selectOne(query.getQueryMethod().getCountStatementId(), params);
			}
			return new PageImpl(result, pageable, total);
		}

		protected <X> long calculateTotal(Pageable pager, List<X> result) {
			if (pager.hasPrevious()) {
				if (CollectionUtils.isEmpty(result)) {
					return -1;
				}
				if (result.size() == pager.getPageSize()) {
					return -1;
				}
				return (pager.getPageNumber() - 1) * pager.getPageSize() + result.size();
			}
			if (result.size() < pager.getPageSize()) {
				return result.size();
			}
			return -1;
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
					.collect(Collectors.toMap(param -> param.getName().orElse("p" + c[0]++), param -> values[param.getIndex()]));

			if (parameters.hasSortParameter()) {
				params.put("_sorts", values[parameters.getSortIndex()]);
			}

			return query.getTemplate().selectOne(query.getQueryMethod().getStatementId(), params);
		}
	}

	static class ModifyingExecution extends MyBatisQueryExecution {

		public ModifyingExecution(MyBatisQueryMethod method) {}

		@Override
		protected Object doExecute(AbstractMyBatisQuery query, Object[] values) {
			return null;
		}
	}

	static class DeleteExecution extends MyBatisQueryExecution {
		@Override
		protected Object doExecute(AbstractMyBatisQuery query, Object[] values) {
			return null;
		}
	}

	static class ExistsExecution extends MyBatisQueryExecution {
		@Override
		protected Object doExecute(AbstractMyBatisQuery query, Object[] values) {
			return null;
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
