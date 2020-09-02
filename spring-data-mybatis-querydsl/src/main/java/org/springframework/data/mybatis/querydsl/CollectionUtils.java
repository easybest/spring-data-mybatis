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
package org.springframework.data.mybatis.querydsl;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public final class CollectionUtils {

	private CollectionUtils() {
	}

	public static Map mapOf(Object... values) {
		int len = values.length;
		if (len % 2 != 0) {
			throw new IllegalArgumentException(
					"Number of arguments should be an even number representing the keys and values");
		}

		Map answer = new LinkedHashMap(len / 2);
		int i = 0;
		while (i < values.length - 1) {
			answer.put(values[i++], values[i++]);
		}
		return answer;
	}

}
