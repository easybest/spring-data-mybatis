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

import java.util.Locale;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public final class StringHelper {

	private static final int ALIAS_TRUNCATE_LENGTH = 10;

	private StringHelper() {
	}

	public static String generateAlias(String description, int unique) {
		return generateAliasRoot(description) + AliasConstantsHelper.get(unique);
	}

	private static String generateAliasRoot(String description) {
		String result = truncate(unqualifyEntityName(description), ALIAS_TRUNCATE_LENGTH).toLowerCase(Locale.ROOT)
				.replace('/', '_')
				// representations
				.replace('$', '_');
		result = cleanAlias(result);
		if (Character.isDigit(result.charAt(result.length() - 1))) {
			return result + "x";
		}
		else {
			return result;
		}
	}

	private static String cleanAlias(String alias) {
		char[] chars = alias.toCharArray();
		if (!Character.isLetter(chars[0])) {
			for (int i = 1; i < chars.length; i++) {
				if (Character.isLetter(chars[i])) {
					return alias.substring(i);
				}
			}
		}
		return alias;
	}

	public static String unqualify(String qualifiedName) {
		int loc = qualifiedName.lastIndexOf('.');
		return (loc < 0) ? qualifiedName : qualifiedName.substring(loc + 1);
	}

	public static String unqualifyEntityName(String entityName) {
		String result = unqualify(entityName);
		int slashPos = result.indexOf('/');
		if (slashPos > 0) {
			result = result.substring(0, slashPos - 1);
		}
		return result;
	}

	public static String truncate(String string, int length) {
		if (string.length() <= length) {
			return string;
		}
		else {
			return string.substring(0, length);
		}
	}

}
