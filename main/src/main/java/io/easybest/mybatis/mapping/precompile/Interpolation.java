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

/**
 * .
 *
 * @author Jarvis Song
 */
@Getter
public class Interpolation extends AbstractSegment {

	private String property;

	public static Interpolation of(String property) {
		return Interpolation.builder().property(property).build();
	}

	public static Interpolation of(Segment property) {
		return Interpolation.builder().property(property.toString()).build();
	}

	@Override
	public String toString() {

		return "${" + this.property + "}";
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		protected List<? extends Segment> contents;

		private String property;

		public Interpolation build() {

			Interpolation instance = new Interpolation();
			instance.contents = this.contents;
			instance.property = this.property;

			return instance;
		}

		public Builder contents(final List<? extends Segment> contents) {
			this.contents = contents;
			return this;
		}

		public Builder property(final String property) {
			this.property = property;
			return this;
		}

	}

}
