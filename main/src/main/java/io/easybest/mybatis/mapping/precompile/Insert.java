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

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import org.springframework.util.StringUtils;

/**
 * .
 *
 * @author Jarvis Song
 */
@SuperBuilder
@Getter
public class Insert extends SqlDefinition {

	private SelectKey selectKey;

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

	@SuperBuilder
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

	}

}
