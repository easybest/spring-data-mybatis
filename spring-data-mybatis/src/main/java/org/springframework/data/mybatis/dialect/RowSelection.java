package org.springframework.data.mybatis.dialect;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RowSelection {

	private Integer firstRow;
	private Integer maxRows;

	public RowSelection(Integer firstRow) {
		this.firstRow = firstRow;
	}

	public boolean definesLimits() {
		return maxRows != null || (firstRow != null && firstRow <= 0);
	}

}
