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

import io.easybest.mybatis.repository.support.MybatisContext;
import lombok.Getter;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.type.JdbcType;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * .
 *
 * @author Jarvis Song
 */
@Getter
public class ResultMap extends SqlDefinition {

	private String type;

	private String extend;

	private List<ResultMapping> resultMappings;

	private List<Association> associations;

	private List<Collection> collections;

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public String toString() {

		StringBuilder builder = new StringBuilder();
		if (!CollectionUtils.isEmpty(this.resultMappings)) {
			this.resultMappings.stream()
					.sorted((o1, o2) -> o1.resultFlag == ResultFlag.ID ? (o2.resultFlag == ResultFlag.ID ? 0 : -1) : 1)
					.map(ResultMapping::toString).forEach(builder::append);
		}
		if (!CollectionUtils.isEmpty(this.associations)) {
			this.associations.stream().map(Association::toString).forEach(builder::append);
		}
		if (!CollectionUtils.isEmpty(this.collections)) {
			this.collections.stream().map(Collection::toString).forEach(builder::append);
		}

		if (StringUtils.hasText(this.getExtend())) {
			return String.format("<resultMap id=\"%s\" type=\"%s\" extends=\"%s\">%s</resultMap>", this.getId(),
					this.getType(), this.getExtend(), builder);
		}
		return String.format("<resultMap id=\"%s\" type=\"%s\">%s</resultMap>", this.getId(), this.getType(), builder);
	}

	@Getter
	@lombok.Builder
	public static class Association {

		private String property;

		private String javaType;

		private String resultMap;

		private String columnPrefix;

		private String fetchType;

		private String select;

		private String column;

		private List<ResultMapping> resultMappings;

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder("<association");
			if (StringUtils.hasText(this.property)) {
				builder.append(" property=\"").append(this.property).append("\"");
			}
			if (StringUtils.hasText(this.javaType)) {
				builder.append(" javaType=\"").append(this.javaType).append("\"");
			}
			if (StringUtils.hasText(this.resultMap)) {
				builder.append(" resultMap=\"").append(this.resultMap).append("\"");
			}
			if (StringUtils.hasText(this.columnPrefix)) {
				builder.append(" columnPrefix=\"").append(this.columnPrefix).append("\"");
			}
			if (StringUtils.hasText(this.fetchType)) {
				builder.append(" fetchType=\"").append(this.fetchType).append("\"");
			}
			if (StringUtils.hasText(this.select)) {
				builder.append(" select=\"").append(this.select).append("\"");
			}
			if (StringUtils.hasText(this.column)) {
				builder.append(" column=\"").append(this.column).append("\"");
			}
			builder.append(">");
			if (!CollectionUtils.isEmpty(this.resultMappings)) {
				this.resultMappings.stream().sorted(
						(o1, o2) -> o1.resultFlag == ResultFlag.ID ? (o2.resultFlag == ResultFlag.ID ? 0 : -1) : 1)
						.map(ResultMapping::toString).forEach(builder::append);
			}
			builder.append("</association>");
			return builder.toString();
		}

	}

	@Getter
	@lombok.Builder
	public static class Collection {

		private String property;

		private String ofType;

		private String fetchType;

		private String select;

		private String column;

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder("<collection");
			if (StringUtils.hasText(this.property)) {
				builder.append(" property=\"").append(this.property).append("\"");
			}
			if (StringUtils.hasText(this.ofType)) {
				builder.append(" ofType=\"").append(this.ofType).append("\"");
			}
			if (StringUtils.hasText(this.fetchType)) {
				builder.append(" fetchType=\"").append(this.fetchType).append("\"");
			}
			if (StringUtils.hasText(this.select)) {
				builder.append(" select=\"").append(this.select).append("\"");
			}
			if (StringUtils.hasText(this.column)) {
				builder.append(" column=\"").append(this.column).append("\"");
			}
			builder.append(">");

			builder.append("</collection>");
			return builder.toString();
		}

	}

	@Getter
	@lombok.Builder
	public static class ResultMapping {

		private String property;

		private String column;

		private String javaType;

		private JdbcType jdbcType;

		private Class<?> typeHandler;

		private ResultFlag resultFlag;

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			if (null != this.resultFlag && this.resultFlag == ResultFlag.ID) {
				builder.append("<id ");
			}
			else {
				builder.append("<result ");
			}
			if (StringUtils.hasText(this.property)) {
				builder.append(" property=\"").append(this.property).append("\"");
			}
			if (StringUtils.hasText(this.column)) {
				builder.append(" column='").append(this.column).append("'");
			}
			if (null != this.javaType) {
				builder.append(" javaType=\"").append(this.javaType).append("\"");
			}
			if (null != this.jdbcType) {
				builder.append(" jdbcType=\"").append(this.jdbcType.name()).append("\"");
			}
			if (null != this.typeHandler) {
				builder.append(" typeHandler=\"").append(this.typeHandler.getName()).append("\"");
			}
			builder.append("/>");
			return builder.toString();
		}

	}

	public static class Builder {

		private List<? extends Segment> contents;

		protected String id;

		protected String databaseId;

		protected List<? extends Segment> derived;

		protected String parameterType = MybatisContext.class.getSimpleName();

		private String type;

		private String extend;

		private List<ResultMapping> resultMappings;

		private List<Association> associations;

		private List<Collection> collections;

		public ResultMap build() {

			ResultMap instance = new ResultMap();
			instance.contents = this.contents;
			instance.id = this.id;
			instance.databaseId = this.databaseId;
			instance.derived = this.derived;
			instance.parameterType = this.parameterType;
			instance.type = this.type;
			instance.extend = this.extend;
			instance.resultMappings = this.resultMappings;
			instance.associations = this.associations;
			instance.collections = this.collections;

			return instance;
		}

		public Builder type(final String type) {
			this.type = type;
			return this;
		}

		public Builder extend(final String extend) {
			this.extend = extend;
			return this;
		}

		public Builder resultMappings(final List<ResultMapping> resultMappings) {
			this.resultMappings = resultMappings;
			return this;
		}

		public Builder associations(final List<Association> associations) {
			this.associations = associations;
			return this;
		}

		public Builder collections(final List<Collection> collections) {
			this.collections = collections;
			return this;
		}

		public Builder contents(final List<? extends Segment> contents) {
			this.contents = contents;
			return this;
		}

		public Builder id(final String id) {
			this.id = id;
			return this;
		}

		public Builder databaseId(final String databaseId) {
			this.databaseId = databaseId;
			return this;
		}

		public Builder derived(final List<? extends Segment> derived) {
			this.derived = derived;
			return this;
		}

		public Builder parameterType(final String parameterType) {
			this.parameterType = parameterType;
			return this;
		}

	}

}
