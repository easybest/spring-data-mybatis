package org.springframework.data.mybatis.repository.query;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mybatis.repository.query.MyBatisQueryExecution.CollectionExecution;
import org.springframework.data.mybatis.repository.query.MyBatisQueryExecution.ModifyingExecution;
import org.springframework.data.mybatis.repository.query.MyBatisQueryExecution.PagedExecution;
import org.springframework.data.mybatis.repository.query.MyBatisQueryExecution.SingleEntityExecution;
import org.springframework.data.mybatis.repository.query.MyBatisQueryExecution.SlicedExecution;
import org.springframework.data.mybatis.repository.query.MyBatisQueryExecution.StreamExecution;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jarvis Song
 */
public abstract class AbstractMyBatisQuery implements RepositoryQuery {

	protected final SqlSessionTemplate template;
	protected final MyBatisQueryMethod method;

	public AbstractMyBatisQuery(SqlSessionTemplate template, MyBatisQueryMethod method) {

		this.template = template;
		this.method = method;

	}

	public SqlSessionTemplate getTemplate() {
		return template;
	}

	@Override
	public MyBatisQueryMethod getQueryMethod() {
		return method;
	}

	@Override
	@Nullable
	public Object execute(Object[] parameters) {
		return doExecute(getExecution(), parameters);
	}

	/**
	 * Get Execution.
	 * 
	 * @return
	 */
	protected abstract MyBatisQueryExecution getExecution();

	protected Query createQuery(Object[] values) {
		return doCreateQuery(values);
	}

	protected abstract Query doCreateQuery(Object[] values);

	@Nullable
	private Object doExecute(MyBatisQueryExecution execution, Object[] values) {

		Object result = execution.execute(this, values);

		ParametersParameterAccessor accessor = new ParametersParameterAccessor(method.getParameters(), values);
		ResultProcessor withDynamicProjection = method.getResultProcessor().withDynamicProjection(accessor);

		return withDynamicProjection.processResult(result, new TupleConverter(withDynamicProjection.getReturnedType()));
	}

	protected MyBatisQueryExecution createExecution() {

		if (method.isStreamQuery()) {
			return new StreamExecution();
		}
		if (method.isCollectionQuery()) {
			return new CollectionExecution();
		}
		if (method.isSliceQuery()) {
			return new SlicedExecution();
		}
		if (method.isPageQuery()) {
			return new PagedExecution();
		}
		if (method.isModifyingQuery()) {
			return new ModifyingExecution();
		}

		return new SingleEntityExecution();
	}

	static class TupleConverter implements Converter<Object, Object> {

		private final ReturnedType type;

		/**
		 * Creates a new {@link TupleConverter} for the given {@link ReturnedType}.
		 *
		 * @param type must not be {@literal null}.
		 */
		public TupleConverter(ReturnedType type) {

			Assert.notNull(type, "Returned type must not be null!");

			this.type = type;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
		 */
		@Override
		public Object convert(Object source) {

			if (!(source instanceof Tuple)) {
				return source;
			}

			Tuple tuple = (Tuple) source;
			List<TupleElement<?>> elements = tuple.getElements();

			if (elements.size() == 1) {

				Object value = tuple.get(elements.get(0));

				if (type.isInstance(value) || value == null) {
					return value;
				}
			}

			Map<String, Object> result = new HashMap<>();
			for (TupleElement<?> element : elements) {

				String alias = element.getAlias();

				if (alias == null || isIndexAsString(alias)) {
					throw new IllegalStateException("No aliases found in result tuple! Make sure your query defines aliases!");
				}

				result.put(element.getAlias(), tuple.get(element));
			}

			return result;
		}

		private static boolean isIndexAsString(String source) {

			try {
				Integer.parseInt(source);
				return true;
			} catch (NumberFormatException o_O) {
				return false;
			}
		}
	}

}
