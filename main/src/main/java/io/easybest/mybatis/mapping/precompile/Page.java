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

import io.easybest.mybatis.dialect.Dialect;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * .
 *
 * @author Jarvis Song
 */
@SuperBuilder
@Getter
public class Page extends AbstractSegment {

	private Segment offset;

	private Segment fetchSize;

	private Segment offsetEnd;

	private Dialect dialect;

	public static Page of(Dialect dialect, Segment offset, Segment fetchSize, Segment... segments) {
		return Page.builder().dialect(dialect).offset(offset).fetchSize(fetchSize).contents(Arrays.asList(segments))
				.build();
	}

	@Override
	public String toString() {

		String sql = this.content();

		return this.dialect.getLimitHandler().processSql(sql, this.offset, this.fetchSize, this.offsetEnd);

	}

}
