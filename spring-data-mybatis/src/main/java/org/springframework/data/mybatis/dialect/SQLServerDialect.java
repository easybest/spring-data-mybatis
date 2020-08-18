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

import org.springframework.data.mybatis.dialect.identity.IdentityColumnSupport;
import org.springframework.data.mybatis.dialect.identity.SQLServerIdentityColumnSupport;
import org.springframework.data.mybatis.dialect.pagination.LimitHandler;
import org.springframework.data.mybatis.dialect.pagination.TopLimitHandler;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public class SQLServerDialect extends Dialect {

	private final LimitHandler limitHandler;

	public SQLServerDialect() {
		this.limitHandler = new TopLimitHandler();
	}

	@Override
	public char openQuote() {
		return '[';
	}

	@Override
	public char closeQuote() {
		return ']';
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new SQLServerIdentityColumnSupport();
	}

	@Override
	public LimitHandler getLimitHandler() {
		return this.limitHandler;
	}

}
