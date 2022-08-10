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
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import org.springframework.util.StringUtils;

/**
 * .
 *
 * @author Jarvis Song
 */
@SuperBuilder
@Getter
public class Function implements Segment {

	private String name;

	private List<String> parameters;

	public static Function of(String name, String... parameters) {
		return Function.builder().name(name).parameters(Arrays.asList(parameters)).build();
	}

	public static Function of(String name, Segment... parameters) {
		return Function.builder().name(name).parameters(
				Arrays.stream(parameters).filter(Objects::nonNull).map(Segment::toString).collect(Collectors.toList()))
				.build();
	}

	@Override
	public String toString() {

		return this.name + "(" + (null == this.parameters ? ""
				: this.parameters.stream().filter(StringUtils::hasText).collect(Collectors.joining(","))) + ")";
	}

}
