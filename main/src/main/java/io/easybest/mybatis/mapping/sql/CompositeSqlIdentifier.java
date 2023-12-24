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

import java.util.Arrays;
import java.util.Iterator;
import java.util.StringJoiner;
import java.util.function.UnaryOperator;

import org.springframework.util.Assert;

/**
 * .
 *
 * @author Jarvis Song
 */
class CompositeSqlIdentifier implements SqlIdentifier {

	private final SqlIdentifier[] parts;

	public CompositeSqlIdentifier(SqlIdentifier... parts) {

		Assert.notNull(parts, "SqlIdentifier parts must not be null.");
		Assert.noNullElements(parts, "SqlIdentifier parts must not contain null elements.");
		Assert.isTrue(parts.length > 0, "SqlIdentifier parts must not be empty.");

		this.parts = parts;
	}

	@Override
	public String getReference(IdentifierProcessing processing) {
		throw new UnsupportedOperationException("Composite SQL Identifiers can't be used for reference name retrieval");
	}

	@Override
	public String toSql(IdentifierProcessing processing) {

		StringJoiner joiner = new StringJoiner(".");
		for (SqlIdentifier part : this.parts) {
			joiner.add(part.toSql(processing));
		}

		return joiner.toString();
	}

	@Override
	public SqlIdentifier transform(UnaryOperator<String> transformationFunction) {
		throw new UnsupportedOperationException("Composite SQL Identifiers cannot be transformed");
	}

	@Override
	public Iterator<SqlIdentifier> iterator() {
		return Arrays.asList(this.parts).iterator();
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
		return this.toSql(IdentifierProcessing.ANSI);
	}

}
