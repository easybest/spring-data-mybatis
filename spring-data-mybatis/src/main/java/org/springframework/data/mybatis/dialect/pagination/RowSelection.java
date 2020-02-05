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
package org.springframework.data.mybatis.dialect.pagination;

/**
 * Row selection.
 *
 * @author JARVIS SONG
 * @since 1.0.0
 */
public final class RowSelection {

	private Integer firstRow;

	private Integer maxRows;

	private Integer timeout;

	private Integer fetchSize;

	public void setFirstRow(Integer firstRow) {
		this.firstRow = firstRow;
	}

	public void setFirstRow(int firstRow) {
		this.firstRow = firstRow;
	}

	public Integer getFirstRow() {
		return this.firstRow;
	}

	public void setMaxRows(Integer maxRows) {
		this.maxRows = maxRows;
	}

	public void setMaxRows(int maxRows) {
		this.maxRows = maxRows;
	}

	public Integer getMaxRows() {
		return this.maxRows;
	}

	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public Integer getTimeout() {
		return this.timeout;
	}

	public Integer getFetchSize() {
		return this.fetchSize;
	}

	public void setFetchSize(Integer fetchSize) {
		this.fetchSize = fetchSize;
	}

	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}

	public boolean definesLimits() {
		return this.maxRows != null || (this.firstRow != null && this.firstRow <= 0);
	}

}
