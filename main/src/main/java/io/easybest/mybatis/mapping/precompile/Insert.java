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

package io.easybest.mybatis.mapping.precompile;

import java.util.List;

import lombok.Getter;
import org.springframework.util.StringUtils;

import io.easybest.mybatis.repository.support.MybatisContext;

/**
 * .
 *
 * @author Jarvis Song
 */
@Getter
public class Insert extends SqlDefinition {

	private SelectKey selectKey;

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("<insert id=\"").append(this.getId()).append("\"");
		if (StringUtils.hasText(this.parameterType)) {
			builder.append(" parameterType=\"").append(this.parameterType).append("\"");
		}
		if (StringUtils.hasText(this.databaseId)) {
			builder.append(" databaseId=\"").append(this.databaseId).append("\"");
		}
		builder.append(">");
		if (null != this.selectKey) {
			builder.append(this.selectKey);
		}
		builder.append(this.content());
		builder.append("</insert>");
		return builder.toString();
	}

	@Getter
	public static class SelectKey extends AbstractSegment {

		private String keyProperty;

		private String keyColumn;

		private Order order;

		private String resultType;

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("<selectKey");
			if (StringUtils.hasText(this.keyProperty)) {
				builder.append(" keyProperty=\"").append(this.keyProperty).append("\"");
			}
			if (StringUtils.hasText(this.keyColumn)) {
				builder.append(" keyColumn=\"").append(this.keyColumn).append("\"");
			}
			if (null != this.order) {
				builder.append(" order=\"").append(this.order.name()).append("\"");
			}
			if (StringUtils.hasText(this.resultType)) {
				builder.append(" resultType=\"").append(this.resultType).append("\"");
			}
			builder.append(">");
			builder.append(this.content());
			builder.append("</selectKey>");
			return builder.toString();
		}

		public enum Order {

			/**
			 * Before.
			 */
			BEFORE,
			/**
			 * After.
			 */
			AFTER

		}

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {

			private List<? extends Segment> contents;

			private String keyProperty;

			private String keyColumn;

			private Order order;

			private String resultType;

			public SelectKey build() {

				SelectKey instance = new SelectKey();
				instance.contents = this.contents;
				instance.keyProperty = this.keyProperty;
				instance.keyColumn = this.keyColumn;
				instance.order = this.order;
				instance.resultType = this.resultType;

				return instance;
			}

			public Builder contents(final List<? extends Segment> contents) {
				this.contents = contents;
				return this;
			}

			public Builder keyProperty(final String keyProperty) {
				this.keyProperty = keyProperty;
				return this;
			}

			public Builder keyColumn(final String keyColumn) {
				this.keyColumn = keyColumn;
				return this;
			}

			public Builder order(final Order order) {
				this.order = order;
				return this;
			}

			public Builder resultType(final String resultType) {
				this.resultType = resultType;
				return this;
			}

		}

	}

	public static class Builder {

		private List<? extends Segment> contents;

		protected String id;

		protected String databaseId;

		protected List<? extends Segment> derived;

		protected String parameterType = MybatisContext.class.getSimpleName();

		private SelectKey selectKey;

		public Insert build() {

			Insert instance = new Insert();
			instance.contents = this.contents;
			instance.id = this.id;
			instance.databaseId = this.databaseId;
			instance.derived = this.derived;
			instance.parameterType = this.parameterType;
			instance.selectKey = this.selectKey;

			return instance;
		}

		public Builder selectKey(final SelectKey selectKey) {
			this.selectKey = selectKey;
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
