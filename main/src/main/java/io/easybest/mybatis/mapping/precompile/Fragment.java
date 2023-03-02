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

import io.easybest.mybatis.repository.support.MybatisContext;

/**
 * .
 *
 * @author Jarvis Song
 */
@Getter
public class Fragment extends SqlDefinition {

	@Override
	public String toString() {
		return String.format("<sql id=\"%s\">%s</sql>", this.getId(), this.content());
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

		public Fragment build() {

			Fragment instance = new Fragment();
			instance.contents = this.contents;
			instance.id = this.id;
			instance.databaseId = this.databaseId;
			instance.derived = this.derived;
			instance.parameterType = this.parameterType;

			return instance;
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
