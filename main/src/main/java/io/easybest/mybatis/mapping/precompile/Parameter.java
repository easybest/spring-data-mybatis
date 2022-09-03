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

package io.easybest.mybatis.mapping.precompile;

import java.util.List;

import io.easybest.mybatis.repository.query.criteria.ParamValue;
import io.easybest.mybatis.repository.support.MybatisContext;
import lombok.Getter;
import org.apache.ibatis.type.JdbcType;

import org.springframework.util.StringUtils;

/**
 * .
 *
 * @author Jarvis Song
 */
@Getter
public class Parameter extends AbstractSegment {

	private String property;

	private String javaType;

	private JdbcType jdbcType;

	private Class<?> typeHandler;

	private Class<?> javaTypeClass;

	public static Parameter of(String property) {
		return Parameter.builder().property(property).build();
	}

	public static Parameter of(ParamValue pv) {
		return Parameter.builder().property(pv.getName()).javaType(pv.getJavaType()).jdbcType(pv.getJdbcType())
				.typeHandler(pv.getTypeHandler()).build();
	}

	public static Parameter additionalValue(String key) {

		return of(MybatisContext.PARAM_ADDITIONAL_VALUES_PREFIX + key);
	}

	public static Parameter bindValue(String key) {

		return of(MybatisContext.PARAM_BINDABLE_PREFIX + key);
	}

	public static Parameter instance(String key) {

		return of(MybatisContext.PARAM_INSTANCE_PREFIX + key);
	}

	public static Parameter pageOffset() {
		return of(MybatisContext.PARAM_PAGEABLE_PREFIX + "offset");
	}

	public static Parameter pageOffsetEnd() {
		return of(MybatisContext.PARAM_PAGEABLE_PREFIX + "offsetEnd");
	}

	public static Parameter pageSize() {
		return of(MybatisContext.PARAM_PAGEABLE_PREFIX + "size");
	}

	@Override
	public String toString() {

		StringBuilder builder = new StringBuilder();
		builder.append("#{");
		builder.append(this.property);
		if (StringUtils.hasText(this.javaType)) {
			builder.append(",javaType=").append(this.javaType);
		}
		else if (null != this.javaTypeClass) {
			builder.append(",javaType=").append(this.javaTypeClass.getName());
		}
		if (null != this.jdbcType) {
			builder.append(",jdbcType=").append(this.jdbcType.name());
		}
		if (null != this.typeHandler) {
			builder.append(",typeHandler=").append(this.typeHandler.getName());
		}
		builder.append("}");
		return builder.toString();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		protected List<? extends Segment> contents;

		private String property;

		private String javaType;

		private JdbcType jdbcType;

		private Class<?> typeHandler;

		private Class<?> javaTypeClass;

		public Parameter build() {

			Parameter instance = new Parameter();
			instance.contents = this.contents;
			instance.property = this.property;
			instance.javaType = this.javaType;
			instance.jdbcType = this.jdbcType;
			instance.typeHandler = this.typeHandler;
			instance.javaTypeClass = this.javaTypeClass;

			return instance;
		}

		public Builder contents(final List<? extends Segment> contents) {
			this.contents = contents;
			return this;
		}

		public Builder property(final String property) {
			this.property = property;
			return this;
		}

		public Builder javaType(final String javaType) {
			this.javaType = javaType;
			return this;
		}

		public Builder jdbcType(final JdbcType jdbcType) {
			this.jdbcType = jdbcType;
			return this;
		}

		public Builder typeHandler(final Class<?> typeHandler) {
			this.typeHandler = typeHandler;
			return this;
		}

		public Builder javaTypeClass(final Class<?> javaTypeClass) {
			this.javaTypeClass = javaTypeClass;
			return this;
		}

	}

}
