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

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

import javax.persistence.TemporalType;

import org.springframework.core.MethodParameter;
import org.springframework.data.mybatis.repository.Temporal;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.lang.Nullable;

/**
 * Custom extension of {@link Parameters} discovering additional query parameter
 * annotations.
 *
 * @author JARVIS SONG
 * @since 1.0.0
 */
public class MybatisParameters extends Parameters<MybatisParameters, MybatisParameters.MybatisParameter> {

	public MybatisParameters(Method method) {
		super(method);
	}

	protected MybatisParameters(List<MybatisParameter> originals) {
		super(originals);
	}

	@Override
	protected MybatisParameter createParameter(MethodParameter parameter) {
		return new MybatisParameter(parameter);
	}

	@Override
	protected MybatisParameters createFrom(List<MybatisParameter> parameters) {
		return new MybatisParameters(parameters);
	}

	static class MybatisParameter extends Parameter {

		private final @Nullable Temporal annotation;

		private @Nullable TemporalType temporalType;

		MybatisParameter(MethodParameter parameter) {

			super(parameter);

			this.annotation = parameter.getParameterAnnotation(Temporal.class);
			this.temporalType = null;

			if (!this.isDateParameter() && this.hasTemporalParamAnnotation()) {
				throw new IllegalArgumentException(
						Temporal.class.getSimpleName() + " annotation is only allowed on Date parameter!");
			}
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
				this.temporalType = (this.annotation != null) ? this.annotation.value() : null;
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
			return this.annotation != null;
		}

		private boolean isDateParameter() {
			return this.getType().equals(Date.class);
		}

	}

}
