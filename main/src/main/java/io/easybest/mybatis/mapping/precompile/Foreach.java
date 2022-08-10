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

import org.springframework.util.StringUtils;

/**
 * .
 *
 * @author Jarvis Song
 */
@Getter
public class Foreach extends AbstractSegment {

	private String collection;

	private String item = "item";

	private String index = "index";

	private String open = "(";

	private String close = ")";

	private String separator = ",";

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("<foreach");
		if (StringUtils.hasText(this.collection)) {
			builder.append(" collection=\"").append(this.collection).append("\"");
		}
		if (StringUtils.hasText(this.item)) {
			builder.append(" item=\"").append(this.item).append("\"");
		}
		if (StringUtils.hasText(this.index)) {
			builder.append(" index=\"").append(this.index).append("\"");
		}
		if (StringUtils.hasText(this.open)) {
			builder.append(" open=\"").append(this.open).append("\"");
		}
		if (StringUtils.hasText(this.close)) {
			builder.append(" close=\"").append(this.close).append("\"");
		}
		if (StringUtils.hasText(this.separator)) {
			builder.append(" separator=\"").append(this.separator).append("\"");
		}
		builder.append(">");
		builder.append(this.content());
		builder.append("</foreach>");
		return builder.toString();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		protected List<? extends Segment> contents;

		private String collection;

		private String item = "item";

		private String index = "index";

		private String open = "(";

		private String close = ")";

		private String separator = ",";

		public Foreach build() {

			Foreach instance = new Foreach();
			instance.contents = this.contents;
			instance.collection = this.collection;
			instance.item = this.item;
			instance.index = this.index;
			instance.open = this.open;
			instance.close = this.close;
			instance.separator = this.separator;

			return instance;
		}

		public Builder contents(final List<? extends Segment> contents) {
			this.contents = contents;
			return this;
		}

		public Builder collection(final String collection) {
			this.collection = collection;
			return this;
		}

		public Builder item(final String item) {
			this.item = item;
			return this;
		}

		public Builder index(final String index) {
			this.index = index;
			return this;
		}

		public Builder open(final String open) {
			this.open = open;
			return this;
		}

		public Builder close(final String close) {
			this.close = close;
			return this;
		}

		public Builder separator(final String separator) {
			this.separator = separator;
			return this;
		}

	}

}
