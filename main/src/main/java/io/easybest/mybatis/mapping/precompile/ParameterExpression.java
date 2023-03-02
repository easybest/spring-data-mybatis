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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * .
 *
 * @author Jarvis Song
 * @param <T> type
 */
@Getter
@NoArgsConstructor
@ToString
public class ParameterExpression<T> {

	private String name;

	private Class<? extends T> javaType;

	public ParameterExpression(String name, Class<? extends T> javaType) {
		this.name = name;
		this.javaType = javaType;
	}

	public ParameterExpression(Class<? extends T> javaType) {
		this.javaType = javaType;
	}

}
