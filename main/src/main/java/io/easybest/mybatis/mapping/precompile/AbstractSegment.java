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
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.Getter;

import org.springframework.util.CollectionUtils;

/**
 * .
 *
 * @author Jarvis Song
 */
@Getter
public abstract class AbstractSegment implements Segment {

	private static final String BLANK = "";

	protected List<? extends Segment> contents;

	@Override
	public String toString() {
		return "";
	}

	protected String content() {

		if (CollectionUtils.isEmpty(this.contents)) {
			return BLANK;
		}

		return this.contents.stream().filter(Objects::nonNull).map(Segment::toString).collect(Collectors.joining(" "));
	}

}
