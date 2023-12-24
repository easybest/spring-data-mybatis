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

package io.easybest.mybatis.mapping.sql;

/**
 * .
 *
 * @author Jarvis Song
 */
public interface IdentifierProcessing {

	/**
	 * Create ANSI instance.
	 */
	IdentifierProcessing ANSI = create(Quoting.ANSI, LetterCasing.UPPER_CASE);

	/**
	 * Create AS IS instance.
	 */
	IdentifierProcessing NONE = create(Quoting.NONE, LetterCasing.AS_IS);

	static DefaultIdentifierProcessing create(Quoting quoting, LetterCasing letterCasing) {
		return new DefaultIdentifierProcessing(quoting, letterCasing);
	}

	String quote(String identifier);

	String standardizeLetterCase(String identifier);

	class Quoting {

		/**
		 * ANSI Quoting.
		 */
		public static final Quoting ANSI = new Quoting("\"");

		/**
		 * None quoting.
		 */
		public static final Quoting NONE = new Quoting("");

		private final String prefix;

		private final String suffix;

		public Quoting(String prefix, String suffix) {

			this.prefix = prefix;
			this.suffix = suffix;
		}

		public Quoting(String quoteCharacter) {

			this(quoteCharacter, quoteCharacter);
		}

		public String apply(String identifier) {
			return this.prefix + identifier + this.suffix;
		}

	}

	enum LetterCasing {

		UPPER_CASE {
			@Override
			String apply(String identifier) {
				return identifier.toUpperCase();
			}
		},
		LOWER_CASE {
			@Override
			String apply(String identifier) {
				return identifier.toLowerCase();
			}
		},
		AS_IS {
			@Override
			String apply(String identifier) {
				return identifier;
			}
		};

		abstract String apply(String identifier);

	}

}
