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

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class ColumnResult implements Serializable {

	private static final long serialVersionUID = -6530771390280831066L;

	private Domain upstage;

	private List<Association> associations = new LinkedList<>();

	private Column column;

	public ColumnResult(Domain upstage, Column column) {
		this.upstage = upstage;
		this.column = column;
	}

	public String getTableAlias() {

		StringBuilder builder = new StringBuilder();
		builder.append(this.upstage.getTableAlias());

		if (this.associations.isEmpty()) {
			return builder.toString();
		}

		for (Association association : this.associations) {
			if (association.isEmbedding()) {
				continue;
			}

			builder.append('.').append(association.getProperty().getName());
		}

		return builder.toString();

	}

	public Domain getUpstage() {
		return this.upstage;
	}

	public void setUpstage(Domain upstage) {
		this.upstage = upstage;
	}

	public List<Association> getAssociations() {
		return this.associations;
	}

	public void setAssociations(List<Association> associations) {
		this.associations = associations;
	}

	public Column getColumn() {
		return this.column;
	}

	public void setColumn(Column column) {
		this.column = column;
	}

}
