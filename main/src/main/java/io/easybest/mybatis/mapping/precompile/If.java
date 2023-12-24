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

import java.util.Arrays;
import java.util.List;

import lombok.Getter;

/**
 * .
 *
 * @author Jarvis Song
 */
@Getter
public class If extends AbstractSegment {

	private String test;

	public static If of(String test, Segment... segments) {
		return If.builder().test(test).contents(Arrays.asList(segments)).build();
	}

	@Override
	public String toString() {

		return "<if" + " test=\"" + this.test + "\"" + ">" + this.content() + "</if>";
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		protected List<? extends Segment> contents;

		private String test;

		public If build() {

			If instance = new If();
			instance.test = this.test;

			return instance;
		}

		public Builder contents(final List<? extends Segment> contents) {
			this.contents = contents;
			return this;
		}

		public Builder test(final String test) {
			this.test = test;
			return this;
		}

	}

}
