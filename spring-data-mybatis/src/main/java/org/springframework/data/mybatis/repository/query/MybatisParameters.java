package org.springframework.data.mybatis.repository.query;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.springframework.core.MethodParameter;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mybatis.repository.query.MybatisParameters.MybatisParameter;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import org.apache.ibatis.type.TypeHandler;

public class MybatisParameters extends Parameters<MybatisParameters, MybatisParameter> {

	public MybatisParameters(Method method) {
		super(method);
	}

	private MybatisParameters(List<MybatisParameter> parameters) {
		super(parameters);
	}

	@Override
	protected MybatisParameter createParameter(MethodParameter parameter) {
		return new MybatisParameter(parameter);
	}

	@Override
	protected MybatisParameters createFrom(List<MybatisParameter> parameters) {
		return new MybatisParameters(parameters);
	}

	public static class MybatisParameter extends Parameter {

		private final @Nullable Temporal annotation;

		private @Nullable TemporalType temporalType;

		private Class<? extends TypeHandler<?>> specifiedTypeHandler;

		/**
		 * Creates a new {@link Parameter} for the given {@link MethodParameter}.
		 * @param parameter must not be {@literal null}.
		 */
		protected MybatisParameter(MethodParameter parameter) {

			super(parameter);

			this.annotation = parameter.getParameterAnnotation(Temporal.class);
			this.temporalType = null;

			if (!isDateParameter() && hasTemporalParamAnnotation()) {
				throw new IllegalArgumentException(Temporal.class.getSimpleName()
						+ " annotation is only allowed on Date parameter!");
			}

			org.springframework.data.mybatis.annotation.TypeHandler typeHandler = parameter
					.getParameterAnnotation(
							org.springframework.data.mybatis.annotation.TypeHandler.class);
			if (null != typeHandler && StringUtils.hasText(typeHandler.value())) {
				String value = typeHandler.value();
				try {
					Class<?> clz = ClassUtils.forName(value,
							ClassUtils.getDefaultClassLoader());

					if (!TypeHandler.class.isAssignableFrom(clz)) {
						throw new MappingException(
								"The specified type handler with value: " + value
										+ " must implement from org.apache.ibatis.type.TypeHandler");
					}
					this.specifiedTypeHandler = (Class<? extends TypeHandler<?>>) clz;
				}
				catch (ClassNotFoundException e) {
					throw new MappingException("The specified type handler with value: "
							+ value + " not found.");
				}
			}
		}

		public Class<? extends TypeHandler<?>> getSpecifiedTypeHandler() {
			return specifiedTypeHandler;
		}

		@Override
		public boolean isBindable() {
			return super.isBindable() || isTemporalParameter();
		}

		boolean isTemporalParameter() {
			return isDateParameter() && hasTemporalParamAnnotation();
		}

		@Nullable
		TemporalType getTemporalType() {

			if (temporalType == null) {
				this.temporalType = annotation == null ? null : annotation.value();
			}

			return this.temporalType;
		}

		TemporalType getRequiredTemporalType() throws IllegalStateException {

			TemporalType temporalType = getTemporalType();

			if (temporalType != null) {
				return temporalType;
			}

			throw new IllegalStateException(
					String.format("Required temporal type not found for %s!", getType()));
		}

		private boolean hasTemporalParamAnnotation() {
			return annotation != null;
		}

		private boolean isDateParameter() {
			return getType().equals(Date.class);
		}

	}

}
