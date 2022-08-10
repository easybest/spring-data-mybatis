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

package io.easybest.mybatis.mapping;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.UnaryOperator;

import io.easybest.mybatis.mapping.sql.IdentifierProcessing;
import io.easybest.mybatis.mapping.sql.SqlIdentifier;

import org.springframework.util.Assert;

/**
 * .
 *
 * @author Jarvis Song
 */
class DerivedSqlIdentifier implements SqlIdentifier {

	private final String name;

	private final boolean quoted;

	DerivedSqlIdentifier(String name, boolean quoted) {

		Assert.hasText(name, "A database object must have at least on name part.");
		this.name = name;
		this.quoted = quoted;
	}

	@Override
	public Iterator<SqlIdentifier> iterator() {
		return Collections.<SqlIdentifier>singleton(this).iterator();
	}

	@Override
	public SqlIdentifier transform(UnaryOperator<String> transformationFunction) {

		Assert.notNull(transformationFunction, "Transformation function must not be null");

		return new DerivedSqlIdentifier(transformationFunction.apply(this.name), this.quoted);
	}

	@Override
	public String toSql(IdentifierProcessing processing) {

		String normalized = processing.standardizeLetterCase(this.name);

		return this.quoted ? processing.quote(normalized) : normalized;
	}

	@Override
	public String getReference(IdentifierProcessing processing) {
		return this.name;
	}

	@Override
	public boolean equals(Object o) {

		if (this == o) {
			return true;
		}

		if (o instanceof SqlIdentifier) {
			return this.toString().equals(o.toString());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	@Override
	public String toString() {
		return this.quoted ? this.toSql(IdentifierProcessing.ANSI) : this.name;
	}

}
