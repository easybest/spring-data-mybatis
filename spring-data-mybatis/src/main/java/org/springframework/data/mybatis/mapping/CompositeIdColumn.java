package org.springframework.data.mybatis.mapping;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jarvis Song
 */
@Getter
@Setter
public class CompositeIdColumn extends IdColumn {

	private Class<?> idClass;
	private List<IdColumn> idColumns = new ArrayList<>();

	public CompositeIdColumn(Table table) {
		super(table);
	}

	public void addIdColumn(IdColumn idColumn) {
		idColumns.add(idColumn);
	}

	@Override
	public CompositeIdColumn clone() {
		CompositeIdColumn copy = new CompositeIdColumn(this.table);
		BeanUtils.copyProperties(this, copy);
		return copy;
	}
}
