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
package org.springframework.data.mybatis.mapping.model;

import lombok.Getter;

/**
 * Table model.
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public class Table {

	@Getter
	private Identifier schema;

	@Getter
	private Identifier catalog;

	@Getter
	private Identifier name;

	public Table(String schema, String catalog, String name) {
		this.schema = Identifier.toIdentifier(schema);
		this.catalog = Identifier.toIdentifier(catalog);
		this.name = Identifier.toIdentifier(name);
	}

	public Table(String name) {
		this.name = Identifier.toIdentifier(name);
	}

	public String getFullName() {
		return ((null != this.schema) ? (this.schema.getCanonicalName() + '.') : "") + //
				((null != this.catalog) ? (this.catalog.getCanonicalName() + '.') : "") + //
				this.name.getCanonicalName();
	}

}
