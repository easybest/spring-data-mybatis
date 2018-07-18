package org.springframework.data.mybatis.mapping;

import lombok.Getter;
import lombok.Setter;

import org.springframework.beans.BeanUtils;

/**
 * @author Jarvis Song
 */
@Getter
@Setter
public class ToManyAssociation extends Association {
	protected AssociationTable inverseJoinTable;
	protected MyBatisPersistentProperty joinProperty;

	public ToManyAssociation(Table table, Table targetTable, MyBatisPersistentProperty property) {
		super(table);
		// this.targetTable = targetTable;
		this.joinProperty = property;
		if (null != targetTable) {
			this.inverseJoinTable = new AssociationTable();
			BeanUtils.copyProperties(targetTable, this.inverseJoinTable);
			this.inverseJoinTable.setAlias(table.getAlias() + '.' + property.getName());
		}
	}
}
