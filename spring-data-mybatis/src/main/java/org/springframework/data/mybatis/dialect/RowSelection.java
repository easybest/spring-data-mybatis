package org.springframework.data.mybatis.dialect;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a selection criteria for rows in a JDBC {@link java.sql.ResultSet}.
 * 
 * @author Jarvis Song
 */
@Data
@NoArgsConstructor
public class RowSelection {

	private Integer firstRow;
	private Integer maxRows;
	private Integer timeout;
	private Integer fetchSize;

	public RowSelection(Integer firstRow) {
		this.firstRow = firstRow;
	}

	public boolean definesLimits() {
		return maxRows != null || (firstRow != null && firstRow <= 0);
	}

}
