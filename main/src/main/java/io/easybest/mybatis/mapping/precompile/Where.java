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

import lombok.Getter;

/**
 * .
 *
 * @author Jarvis Song
 */
@Getter
public class Where extends AbstractSegment {

	public static Where of(Segment... segments) {
		return Where.builder().contents(Arrays.asList(segments)).build();
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public String toString() {

		return "<where>" + this.content() + "</where>";
	}

	public static class Builder {

		private List<? extends Segment> contents;

		public Where build() {

			Where where = new Where();
			where.contents = this.contents;
			return where;

		}

		public Builder contents(final List<? extends Segment> contents) {
			this.contents = contents;
			return this;
		}

	}

}
