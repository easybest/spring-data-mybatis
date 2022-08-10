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

package io.easybest.mybatis.dialect;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for {@link Dialect} implementations.
 *
 * @author Jarvis Song
 */
public abstract class AbstractDialect implements Dialect {

	protected Map<String, String> functions = new HashMap<>();

	public AbstractDialect() {

		this.registerFunction("lower", "lower");
		this.registerFunction("concat", "concat");

	}

	@Override
	public String getFunction(String name) {
		return this.functions.get(name);
	}

	protected void registerFunction(String name, String fun) {
		this.functions.put(name, fun);
	}

}
