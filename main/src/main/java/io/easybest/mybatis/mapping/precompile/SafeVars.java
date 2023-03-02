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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;

/**
 * .
 *
 * @author Jarvis Song
 */
@Getter
public class SafeVars extends AbstractSegment {

	private String var;

	private int stripPrefix;

	private int stripSuffix;

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		String[] parts = this.var.split("\\.");
		if (parts.length <= (this.stripPrefix + this.stripSuffix)) {
			builder.append(this.content());
			return builder.toString();
		}

		String[] copy = new String[parts.length - this.stripPrefix - this.stripSuffix];
		System.arraycopy(parts, this.stripPrefix, copy, 0, copy.length);

		String prefix = "";
		if (this.stripPrefix > 0) {
			prefix = String.join(".", Arrays.copyOf(parts, this.stripPrefix));
			prefix += ".";
		}

		String[] conds = new String[copy.length];
		for (int i = 0; i < copy.length; i++) {
			conds[i] = (i > 0 ? (conds[i - 1] + '.') : "") + copy[i];
		}

		String finalPrefix = prefix;
		builder.append("<if test=\"").append(
				Arrays.stream(conds).map(p -> finalPrefix + p + " != null").collect(Collectors.joining(" and ")))
				.append("\">");
		builder.append(this.content());
		builder.append("</if>");
		return builder.toString();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		protected List<? extends Segment> contents;

		private String var;

		private int stripPrefix;

		private int stripSuffix;

		public SafeVars build() {

			SafeVars instance = new SafeVars();
			instance.contents = this.contents;
			instance.var = this.var;
			instance.stripPrefix = this.stripPrefix;
			instance.stripSuffix = this.stripSuffix;

			return instance;
		}

		public Builder contents(final List<? extends Segment> contents) {
			this.contents = contents;
			return this;
		}

		public Builder var(final String var) {
			this.var = var;
			return this;
		}

		public Builder stripPrefix(final int stripPrefix) {
			this.stripPrefix = stripPrefix;
			return this;
		}

		public Builder stripSuffix(final int stripSuffix) {
			this.stripSuffix = stripSuffix;
			return this;
		}

	}

}
