/*
 * Copyright 2019-2023 the original author or authors.
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

import java.util.Collections;
import java.util.Iterator;
import java.util.function.UnaryOperator;

import org.springframework.data.util.Streamable;

/**
 * .
 *
 * @author Jarvis Song
 */
public interface SqlIdentifier extends Streamable<SqlIdentifier> {

	/**
	 * Empty instance.
	 */
	SqlIdentifier EMPTY = new SqlIdentifier() {
		@Override
		public String getReference(IdentifierProcessing processing) {
			throw new UnsupportedOperationException(
					"An empty SqlIdentifier can not be used in to create column names.");
		}

		@Override
		public String toSql(IdentifierProcessing processing) {
			throw new UnsupportedOperationException(
					"An empty SqlIdentifier can not be used in to create SQL snippets.");
		}

		@Override
		public SqlIdentifier transform(UnaryOperator<String> transformationFunction) {
			return this;
		}

		@Override
		public Iterator<SqlIdentifier> iterator() {
			return Collections.emptyIterator();
		}

		@Override
		public String toString() {
			return "<NULL-IDENTIFIER>";
		}
	};

	String getReference(IdentifierProcessing processing);

	default String getReference() {
		return this.getReference(IdentifierProcessing.NONE);
	}

	String toSql(IdentifierProcessing processing);

	SqlIdentifier transform(UnaryOperator<String> transformationFunction);

	static SqlIdentifier quoted(String name) {
		return new DefaultSqlIdentifier(name, true);
	}

	static SqlIdentifier unquoted(String name) {
		return new DefaultSqlIdentifier(name, false);
	}

	static SqlIdentifier from(SqlIdentifier... sqlIdentifiers) {
		return new CompositeSqlIdentifier(sqlIdentifiers);
	}

}
