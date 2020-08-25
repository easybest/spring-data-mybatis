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
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Collection association.
 *
 * @author JARVIS SONG
 * @since 2.1.0
 */
@Data
@Accessors(chain = true)
public class Collection implements Serializable {

	private String property;

	private String ofType;

	private String column;

	private String select;

	private String fetch;

	private JoinTable joinTable;

	private List<JoinColumn> joinColumns = new ArrayList<>();

	private String targetTable;

	private boolean manyToMany;

	public Collection addJoinColumn(JoinColumn jc) {
		this.joinColumns.add(jc);
		return this;
	}

	@Data
	@Accessors(chain = true)
	public static class JoinTable {

		private String tableName;

		private List<JoinColumn> joinColumns = new ArrayList<>();

		private List<JoinColumn> inverseJoinColumns = new ArrayList<>();

		public JoinTable(String tableName) {
			this.tableName = tableName;
		}

		public JoinTable addJoinColumn(JoinColumn jc) {
			this.joinColumns.add(jc);
			return this;
		}

		public JoinTable addInverseJoinColumns(JoinColumn jc) {
			this.inverseJoinColumns.add(jc);
			return this;
		}

	}

	@Data
	@Accessors(chain = true)
	public static class JoinColumn {

		private String name;

		private String referencedColumnName;

		public JoinColumn(String name, String referencedColumnName) {
			this.name = name;
			this.referencedColumnName = referencedColumnName;
		}

	}

}
