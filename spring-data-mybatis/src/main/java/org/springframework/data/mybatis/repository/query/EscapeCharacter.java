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
package org.springframework.data.mybatis.repository.query;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import lombok.Value;

import org.springframework.lang.Nullable;

/**
 * A value type encapsulating an escape character for LIKE queries and the actually usage
 * of it in escaping {@link String}s.
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
@Value(staticConstructor = "of")
public class EscapeCharacter {

	/**
	 * Default instance.
	 */
	public static final EscapeCharacter DEFAULT = EscapeCharacter.of('\\');

	/**
	 * To be replaced characters.
	 */
	private static final List<String> TO_REPLACE = Arrays.asList("_", "%");

	char escapeCharacter;

	/**
	 * Escapes all special like characters ({@code _}, {@code %}) using the configured
	 * escape character.
	 * @param value may be {@literal null}.
	 * @return escaped literal
	 */
	@Nullable
	public String escape(@Nullable String value) {

		return (null != value) ? Stream.concat(Stream.of(String.valueOf(this.escapeCharacter)), TO_REPLACE.stream())
				.reduce(value, (it, character) -> it.replace(character, this.escapeCharacter + character)) : null;
	}

}
