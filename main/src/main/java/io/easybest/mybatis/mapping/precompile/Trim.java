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
public class Trim extends AbstractSegment {

	private String prefix;

	private String prefixOverrides;

	private String suffix;

	private String suffixOverrides;

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append("<trim");
		if (StringUtils.hasText(this.prefix)) {
			builder.append(" prefix=\"").append(this.prefix).append("\"");
		}
		if (StringUtils.hasText(this.prefixOverrides)) {
			builder.append(" prefixOverrides=\"").append(this.prefixOverrides).append("\"");
		}
		if (StringUtils.hasText(this.suffix)) {
			builder.append(" suffix=\"").append(this.suffix).append("\"");
		}
		if (StringUtils.hasText(this.suffixOverrides)) {
			builder.append(" suffixOverrides=\"").append(this.suffixOverrides).append("\"");
		}
		builder.append(">");
		builder.append(this.content());
		builder.append("</trim>");

		return builder.toString();
	}

	public static class Builder {

		private List<? extends Segment> contents;

		private String prefix;

		private String prefixOverrides;

		private String suffix;

		private String suffixOverrides;

		public Trim build() {

			Trim instance = new Trim();
			instance.contents = this.contents;
			instance.prefix = this.prefix;
			instance.prefixOverrides = this.prefixOverrides;
			instance.suffix = this.suffix;
			instance.suffixOverrides = this.suffixOverrides;
			return instance;
		}

		public Builder contents(final List<? extends Segment> contents) {
			this.contents = contents;
			return this;
		}

		public Builder prefix(final String prefix) {
			this.prefix = prefix;
			return this;
		}

		public Builder prefixOverrides(final String prefixOverrides) {
			this.prefixOverrides = prefixOverrides;
			return this;
		}

		public Builder suffix(final String suffix) {
			this.suffix = suffix;
			return this;
		}

		public Builder suffixOverrides(final String suffixOverrides) {
			this.suffixOverrides = suffixOverrides;
			return this;
		}

	}

}
