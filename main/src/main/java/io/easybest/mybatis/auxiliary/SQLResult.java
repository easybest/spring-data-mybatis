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

package io.easybest.mybatis.auxiliary;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * .
 *
 * @author Jarvis Song
 */
@Getter
@AllArgsConstructor
@SuperBuilder
@ToString
public class SQLResult implements Serializable {

	private static final long serialVersionUID = 1605358872060161377L;

	/**
	 * Empty.
	 */
	public static final SQLResult EMPTY = SQLResult.builder().build();

	/**
	 * Params name.
	 */
	public static final String PARAM_NAME = "__SQLResult";

	/**
	 * Params name.
	 */
	public static final String PARAM_CONNECTOR_NAME = PARAM_NAME + ".connector";

	/**
	 * Params name.
	 */
	public static final String PARAM_CONDITION_NAME = PARAM_NAME + ".condition";

	/**
	 * Params name.
	 */
	public static final String PARAM_SORTING_NAME = PARAM_NAME + ".sorting";

	@Builder.Default
	private String connector = "";

	@Builder.Default
	private String condition = "";

	@Builder.Default
	private String sorting = "";

}
