package org.springframework.data.mybatis.mapping;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Jarvis Song
 */
@Getter
@Setter
public class JoinTableColumn extends JoinColumn {
	public JoinTableColumn(Table table) {
		super(table);
	}
}
