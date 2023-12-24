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

import lombok.Getter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import io.easybest.mybatis.repository.support.MybatisContext;

/**
 * .
 *
 * @author Jarvis Song
 */
@Getter
public class Select extends SqlDefinition {

	private String resultMap;

	private String resultType;

	@Override
	public String toString() {
		String sql = this.content();
		if (!StringUtils.hasText(sql)) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		builder.append("<select id=\"").append(this.getId()).append("\"");
		if (StringUtils.hasText(this.resultMap)) {
			builder.append(" resultMap=\"").append(this.resultMap).append("\"");
		}
		if (StringUtils.hasText(this.resultType)) {
			builder.append(" resultType=\"").append(this.resultType).append("\"");
		}
		if (StringUtils.hasText(this.parameterType)) {
			builder.append(" parameterType=\"").append(this.parameterType).append("\"");
		}

		if (StringUtils.hasText(this.databaseId)) {
			builder.append(" databaseId=\"").append(this.databaseId).append("\"");
		}

		builder.append(">").append(sql).append("</select>");
		if (!CollectionUtils.isEmpty(this.derived)) {
			this.derived.forEach(builder::append);
		}
		return builder.toString();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private List<? extends Segment> contents;

		protected String id;

		protected String databaseId;

		protected List<? extends Segment> derived;

		protected String parameterType = MybatisContext.class.getSimpleName();

		private String resultMap;

		private String resultType;

		public Select build() {

			Select instance = new Select();
			instance.contents = this.contents;
			instance.id = this.id;
			instance.databaseId = this.databaseId;
			instance.derived = this.derived;
			instance.parameterType = this.parameterType;
			instance.resultMap = this.resultMap;
			instance.resultType = this.resultType;

			return instance;
		}

		public Builder resultMap(final String resultMap) {
			this.resultMap = resultMap;
			return this;
		}

		public Builder resultType(final String resultType) {
			this.resultType = resultType;
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
