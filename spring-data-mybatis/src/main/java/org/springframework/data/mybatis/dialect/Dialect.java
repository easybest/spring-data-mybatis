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

import lombok.extern.slf4j.Slf4j;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mybatis.dialect.identity.IdentityColumnSupport;
import org.springframework.data.mybatis.dialect.identity.IdentityColumnSupportImpl;
import org.springframework.data.mybatis.dialect.pagination.LegacyLimitHandler;
import org.springframework.data.mybatis.dialect.pagination.LimitHandler;

/**
 * Database dialect.
 *
 * @author JARVIS SONG
 * @since 1.0.0
 */
@Slf4j
public abstract class Dialect {

	protected Dialect() {
		log.info("Using dialect: {}", this);

	}

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

	public final String quoteCertainly(String name) {
		if (null == name) {
			return null;
		}
		if (name.charAt(0) == '`') {
			return this.openQuote() + name.substring(1, name.length() - 1) + this.closeQuote();
		}
		return this.openQuote() + name + this.closeQuote();

	}

	public IdentityColumnSupport getIdentityColumnSupport() {
		return new IdentityColumnSupportImpl();
	}

	public String getNativeIdentifierGeneratorStrategy() {
		if (this.getIdentityColumnSupport().supportsIdentityColumns()) {
			return "identity";
		}
		else {
			return "sequence";
		}
	}

	public String getSequenceNextValString(String sequenceName) throws MappingException {
		throw new MappingException(this.getClass().getName() + " does not support sequences");
	}

	public LimitHandler getLimitHandler() {
		return new LegacyLimitHandler(this);
	}

	public String getLowercaseFunction() {
		return "lower";
	}

	public String getRegexLikeFunction(String column, String parameter) {
		// throw new InvalidDataAccessApiUsageException("Unsupported regex like query.");
		return "";
	}

	@Override
	public String toString() {
		return this.getClass().getName();
	}

}
