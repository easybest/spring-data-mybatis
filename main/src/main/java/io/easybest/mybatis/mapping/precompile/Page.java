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

import io.easybest.mybatis.dialect.Dialect;

/**
 * .
 *
 * @author Jarvis Song
 */
@Getter
public class Page extends AbstractSegment {

	private Segment offset;

	private Segment fetchSize;

	private Segment offsetEnd;

	private Dialect dialect;

	public static Page of(Dialect dialect, Segment offset, Segment fetchSize, Segment offsetEnd, Segment... segments) {

		return Page.builder().dialect(dialect).offset(offset).fetchSize(fetchSize).offsetEnd(offsetEnd)
				.contents(Arrays.asList(segments)).build();
	}

	@Override
	public String toString() {

		String sql = this.content();

		return this.dialect.getPaginationHandler().processSql(sql, this.offset, this.fetchSize, this.offsetEnd);

	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		protected List<? extends Segment> contents;

		private Segment offset;

		private Segment fetchSize;

		private Segment offsetEnd;

		private Dialect dialect;

		public Page build() {

			Page instance = new Page();
			instance.contents = this.contents;
			instance.offset = this.offset;
			instance.fetchSize = this.fetchSize;
			instance.offsetEnd = this.offsetEnd;
			instance.dialect = this.dialect;

			return instance;
		}

		public Builder contents(final List<? extends Segment> contents) {
			this.contents = contents;
			return this;
		}

		public Builder offset(final Segment offset) {
			this.offset = offset;
			return this;
		}

		public Builder fetchSize(final Segment fetchSize) {
			this.fetchSize = fetchSize;
			return this;
		}

		public Builder offsetEnd(final Segment offsetEnd) {
			this.offsetEnd = offsetEnd;
			return this;
		}

		public Builder dialect(final Dialect dialect) {
			this.dialect = dialect;
			return this;
		}

	}

}
