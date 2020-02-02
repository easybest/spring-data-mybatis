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
package org.springframework.data.mybatis.dialect;

/**
 * Database dialect.
 *
 * @author JARVIS SONG
 * @since 1.0.0
 */
public abstract class Dialect {

	/**
	 * The character specific to this dialect used to begin a quoted identifier.
	 * @return the dialect's specific open quote character.
	 */
	public char openQuote() {
		return '"';
	}

	/**
	 * The character specific to this dialect used to close a quoted identifier.
	 * @return the dialect's specific close quote character.
	 */
	public char closeQuote() {
		return '"';
	}

	/**
	 * Apply dialect-specific quoting.
	 * <p/>
	 * By default, the incoming value is checked to see if its first character is the
	 * back-tick (`). If so, the dialect specific quoting is applied.
	 * @param name the value to be quoted.
	 * @return the quoted (or unmodified, if not starting with back-tick) value.
	 * @see #openQuote()
	 * @see #closeQuote()
	 */
	public final String quote(String name) {
		if (null == name) {
			return null;
		}

		if (name.charAt(0) == '`') {
			return this.openQuote() + name.substring(1, name.length() - 1) + this.closeQuote();
		}
		else {
			return name;
		}
	}

}
