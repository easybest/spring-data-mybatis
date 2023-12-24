/*
 * Copyright 2019-2023 the original author or authors.
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

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import jakarta.persistence.TemporalType;

import org.apache.ibatis.type.JdbcType;
import org.springframework.core.MethodParameter;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersSource;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;

import io.easybest.mybatis.annotation.TypeHandler;
import io.easybest.mybatis.mapping.TypeRegistry;
import io.easybest.mybatis.mapping.handler.UUIDTypeHandler;
import io.easybest.mybatis.repository.Temporal;

/**
 * .
 *
 * @author Jarvis Song
 */
public class MybatisParameters extends Parameters<MybatisParameters, MybatisParameters.MybatisParameter> {

	public MybatisParameters(ParametersSource parametersSource) {
		super(parametersSource,
				methodParameter -> new MybatisParameter(methodParameter, parametersSource.getDomainTypeInformation()));
	}

	public MybatisParameters(ParametersSource parametersSource,
			Function<MethodParameter, MybatisParameter> parameterFactory) {
		super(parametersSource, parameterFactory);
	}

	protected MybatisParameters(List<MybatisParameter> originals) {
		super(originals);
	}

	// @Override
	// protected MybatisParameter createParameter(MethodParameter parameter) {
	// return new MybatisParameter(parameter);
	// }

	@Override
	protected MybatisParameters createFrom(List<MybatisParameter> parameters) {
		return new MybatisParameters(parameters);
	}

	public static class MybatisParameter extends Parameter {

		private final @Nullable Temporal annotation;

		private @Nullable TemporalType temporalType;

		private final Lazy<JdbcType> jdbcType;

		private final Lazy<Class<?>> typeHandler;

		protected MybatisParameter(MethodParameter parameter, TypeInformation<?> domainType) {

			super(parameter, domainType);

			this.annotation = parameter.getParameterAnnotation(Temporal.class);
			this.temporalType = null;

			if (!this.isDateParameter() && this.hasTemporalParamAnnotation()) {
				throw new IllegalArgumentException(
						Temporal.class.getSimpleName() + " annotation is only allowed on Date parameter!");
			}

			this.jdbcType = Lazy.of(() -> {
				io.easybest.mybatis.annotation.JdbcType jdbcTypeAnn = parameter
						.getParameterAnnotation(io.easybest.mybatis.annotation.JdbcType.class);
				if (null != jdbcTypeAnn) {
					return JdbcType.forCode(jdbcTypeAnn.value());
				}
				TemporalType tt = this.getTemporalType();
				if (null != tt) {
					switch (tt) {

					case DATE:
						return JdbcType.DATE;
					case TIME:
						return JdbcType.TIME;
					case TIMESTAMP:
						return JdbcType.TIMESTAMP;
					}
				}
				return TypeRegistry.convert(parameter.getParameterType()).orElse(null);
			});
			this.typeHandler = Lazy.of(() -> {
				TypeHandler typeHandlerAnn = parameter.getParameterAnnotation(TypeHandler.class);
				if (null != typeHandlerAnn) {

					Class<?> clz = typeHandlerAnn.value();
					if (TypeHandler.class.isAssignableFrom(clz)) {
						return clz;
					}
					else {
						throw new MappingException(clz.getName() + " is not a validated type handler.");
					}
				}
				if (this.getType() == UUID.class) {
					return UUIDTypeHandler.class;
				}
				return null;
			});
		}

		public Class<?> getTypeHandler() {
			return this.typeHandler.getNullable();
		}

		public JdbcType getJdbcType() {
			return this.jdbcType.getNullable();
		}

		@Override
		public boolean isBindable() {
			return super.isBindable() || this.isTemporalParameter();
		}

		boolean isTemporalParameter() {
			return this.isDateParameter() && this.hasTemporalParamAnnotation();
		}

		@Nullable
		TemporalType getTemporalType() {

			if (this.temporalType == null) {
				this.temporalType = null == this.annotation ? null : this.annotation.value();
			}

			return this.temporalType;
		}

		TemporalType getRequiredTemporalType() throws IllegalStateException {

			TemporalType temporalType = this.getTemporalType();

			if (temporalType != null) {
				return temporalType;
			}

			throw new IllegalStateException(String.format("Required temporal type not found for %s!", this.getType()));
		}

		private boolean hasTemporalParamAnnotation() {
			return null != this.annotation;
		}

		private boolean isDateParameter() {
			return this.getType().equals(Date.class);
		}

	}

}
