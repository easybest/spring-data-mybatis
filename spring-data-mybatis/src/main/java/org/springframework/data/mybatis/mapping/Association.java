package org.springframework.data.mybatis.mapping;

import lombok.Getter;
import lombok.Setter;

import org.springframework.beans.BeanUtils;

/**
 * @author Jarvis Song
 */
@Getter
@Setter
public class Association implements Cloneable {

	protected final Table table;

	public Association(Table table) {
		this.table = table;
	}

	@Override
	public Association clone() {
		Association copy = new Association(this.table);
		BeanUtils.copyProperties(this, copy);
		return copy;
	}

}
