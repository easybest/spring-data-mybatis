/*
 * Copyright 2012-2019 the original author or authors.
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
package org.springframework.data.mybatis.dialect.internal;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public final class AliasConstantsHelper {

	private static final int MAX_POOL_SIZE = 40;

	private static final String[] pool = initPool(MAX_POOL_SIZE);

	private AliasConstantsHelper() {
	}

	public static String get(final int i) {
		if (i < MAX_POOL_SIZE && i >= 0) {
			return pool[i];
		}
		else {
			return internalAlias(i);
		}
	}

	private static String[] initPool(final int maxPoolSize) {
		String[] pool = new String[maxPoolSize];
		for (int i = 0; i < maxPoolSize; i++) {
			pool[i] = internalAlias(i);
		}
		return pool;
	}

	private static String internalAlias(final int i) {
		return Integer.toString(i) + '_';
	}

}
