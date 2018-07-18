package org.springframework.data.mybatis.mapping;

import lombok.Getter;
import lombok.Setter;

import org.springframework.beans.BeanUtils;

/**
 * @author Jarvis Song
 */
@Getter
@Setter
public class IdColumn extends Column {

	public IdColumn(Table table) {
		super(table);
	}

	@Override
	public IdColumn clone() {
		IdColumn copy = new IdColumn(table);
		BeanUtils.copyProperties(this, copy);
		return copy;
	}
}
