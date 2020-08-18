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

import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.mybatis.repository.support.ResidentParameterName;

/**
 * Row selection.
 *
 * @author JARVIS SONG
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
public final class RowSelection {

	private String firstRow;

	private String maxRows;

	private String lastRow;

	public RowSelection(boolean variable) {
		if (variable) {
			this.firstRow = "#{" + ResidentParameterName.OFFSET + "}";
			this.maxRows = "#{" + ResidentParameterName.PAGE_SIZE + "}";
			this.lastRow = "#{" + ResidentParameterName.OFFSET_END + "}";
		}
	}

	public RowSelection(int firstRow, int maxRows) {
		this.firstRow = String.valueOf(firstRow);
		this.maxRows = String.valueOf(maxRows);
		this.lastRow = String.valueOf(firstRow + maxRows);
	}

	public void setFirstRow(int firstRow) {
		this.firstRow = String.valueOf(firstRow);
	}

	public void setMaxRows(int maxRows) {
		this.maxRows = String.valueOf(maxRows);
	}

}
