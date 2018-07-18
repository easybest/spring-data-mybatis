package org.springframework.data.mybatis.mapping;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import javax.persistence.JoinTable;
import java.util.stream.Stream;

/**
 * @author Jarvis Song
 */
@Getter
@Setter
public class ToOneJoinTableAssociation extends ToOneAssociation {

	protected AssociationTable joinTable;
	protected JoinTableColumn[] joinColumns;
	protected JoinTableColumn[] inverseJoinColumns;

	public ToOneJoinTableAssociation(Table table, Table targetTable, MyBatisPersistentProperty property) {
		super(table, targetTable, property);
		JoinTable joinTable = property.findAnnotation(JoinTable.class);
		this.joinTable = new AssociationTable();
		if (null != joinTable) {
			this.joinTable.setCatalog(joinTable.catalog());
			this.joinTable.setSchema(joinTable.schema());
		}
		this.joinTable.setName(null != joinTable && StringUtils.hasText(joinTable.name()) ? joinTable.name()
				: (table.getName() + "_" + targetTable.getName()));
		this.joinTable.setAlias(property.getName() + '_' + this.joinTable.getName());

		if (null != joinTable && joinTable.joinColumns().length > 0) {
			this.joinColumns = Stream.of(joinTable.joinColumns()).map(jt -> {
				JoinTableColumn joinTableColumn = new JoinTableColumn(this.joinTable);
				joinTableColumn.setProperty(property);
				joinTableColumn.setName(StringUtils.hasText(jt.name()) ? jt.name()
						: (property.getName() + "_" + targetTable.getIdColumn().getName()));
				joinTableColumn.setReferencedColumnName(
						StringUtils.hasText(jt.referencedColumnName()) ? jt.referencedColumnName() : table.getIdColumn().getName());
				return joinTableColumn;
			}).toArray(JoinTableColumn[]::new);
		} else {
			JoinTableColumn joinTableColumn = new JoinTableColumn(this.joinTable);
			joinTableColumn.setProperty(property);
			joinTableColumn.setName(table.getAlias() + "_" + targetTable.getIdColumn().getName());
			joinTableColumn.setReferencedColumnName(table.getIdColumn().getName());
			this.joinColumns = new JoinTableColumn[] { joinTableColumn };
		}

		if (null != joinTable && joinTable.inverseJoinColumns().length > 0) {
			this.inverseJoinColumns = Stream.of(joinTable.inverseJoinColumns()).map(jt -> {
				JoinTableColumn joinTableColumn = new JoinTableColumn(this.joinTable);
				joinTableColumn.setProperty(property);
				joinTableColumn.setName(StringUtils.hasText(jt.name()) ? jt.name()
						: (property.getName() + "_" + targetTable.getIdColumn().getName()));
				joinTableColumn
						.setReferencedColumnName(StringUtils.hasText(jt.referencedColumnName()) ? jt.referencedColumnName()
								: targetTable.getIdColumn().getName());
				return joinTableColumn;
			}).toArray(JoinTableColumn[]::new);
		} else {
			JoinTableColumn joinTableColumn = new JoinTableColumn(this.joinTable);
			joinTableColumn.setProperty(property);
			joinTableColumn.setName(property.getName() + '_' + targetTable.getIdColumn().getName());
			joinTableColumn.setReferencedColumnName(table.getIdColumn().getName());
			this.inverseJoinColumns = new JoinTableColumn[] { joinTableColumn };
		}

	}
}
