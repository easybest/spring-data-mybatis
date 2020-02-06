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
package org.springframework.data.mybatis.processor;

import java.util.ArrayList;
import java.util.List;

/**
 * Process table metadata.
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public class TableMeta {

	private String name;

	private String domainClassName;

	private String exampleClassName;

	private String exampleClassSimpleName;

	private String packageName;

	private List<ColumnMeta> columns = new ArrayList<>();

	public List<ColumnMeta> getColumns() {
		return this.columns;
	}

	public void setColumns(List<ColumnMeta> columns) {
		this.columns = columns;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDomainClassName() {
		return this.domainClassName;
	}

	public void setDomainClassName(String domainClassName) {
		this.domainClassName = domainClassName;
	}

	public String getExampleClassName() {
		return this.exampleClassName;
	}

	public void setExampleClassName(String exampleClassName) {
		this.exampleClassName = exampleClassName;
		if (null != exampleClassName) {
			this.exampleClassSimpleName = exampleClassName.substring(exampleClassName.lastIndexOf(".") + 1);
		}
	}

	public String getPackageName() {
		return this.packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getExampleClassSimpleName() {
		return this.exampleClassSimpleName;
	}

	public void setExampleClassSimpleName(String exampleClassSimpleName) {
		this.exampleClassSimpleName = exampleClassSimpleName;
	}

	public void addColumn(ColumnMeta column) {
		this.columns.add(column);
	}

}
