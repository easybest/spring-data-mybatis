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

package io.easybest.mybatis.dialect;

import javax.persistence.GenerationType;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.mapping.MappingException;

import io.easybest.mybatis.mapping.sql.IdentifierProcessing;

/**
 * .
 *
 * @author Jarvis Song
 */
public interface Dialect {

	PaginationHandler getPaginationHandler();

	String getFunction(String name);

	default String escape(String character) {

		return String.format("ESCAPE %s", character);
	}

	default String regexpLike(String column, String pattern) {
		throw new InvalidDataAccessApiUsageException("Unsupported regexpLike");
	}

	default IdentifierProcessing getIdentifierProcessing() {
		return IdentifierProcessing.ANSI;
	}

	default String getNativeIdentifierGeneratorStrategy() {
		return GenerationType.SEQUENCE.name().toLowerCase();
	}

	default String getIdentitySelectString(String table, String column, int type) {
		throw new MappingException(this.getClass().getName() + " does not support identity select.");
	}

	default String getIdentityInsertString() {
		return null;
	}

	default String getSequenceNextValString(String sequenceName) {
		throw new MappingException(this.getClass().getName() + " does not support sequences");
	}

	default String limitN(int n) {
		return "";
	}

	default boolean supportsBoolean() {
		return true;
	}

}
