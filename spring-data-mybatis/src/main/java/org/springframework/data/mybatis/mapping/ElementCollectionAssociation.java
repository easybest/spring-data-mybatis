package org.springframework.data.mybatis.mapping;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import javax.persistence.CollectionTable;
import java.util.stream.Stream;

/**
 * @author Jarvis Song
 */
@Getter
@Setter
public class ElementCollectionAssociation extends ToManyAssociation {

	protected JoinTableColumn[] joinColumns;
	protected Column mappingColumn;

	public ElementCollectionAssociation(Table table, Table targetTable, MyBatisPersistentProperty property) {
		super(table, targetTable, property);

		CollectionTable collectionTable = property.findAnnotation(CollectionTable.class);
		this.inverseJoinTable = new AssociationTable();
		if (null != collectionTable) {
			this.inverseJoinTable.setCatalog(collectionTable.catalog());
			this.inverseJoinTable.setSchema(collectionTable.schema());
		}
		this.inverseJoinTable
				.setName(null != collectionTable && StringUtils.hasText(collectionTable.name()) ? collectionTable.name()
						: (table.getName() + "_" + property.getName()));
		this.inverseJoinTable.setAlias(table.getAlias() + '.' + property.getName());

		if (null != collectionTable && collectionTable.joinColumns().length > 0) {
			this.joinColumns = Stream.of(collectionTable.joinColumns()).map(jt -> {
				JoinTableColumn joinTableColumn = new JoinTableColumn(this.inverseJoinTable);
				joinTableColumn.setProperty(property);
				joinTableColumn.setPrefix(table.getAlias());
				joinTableColumn.setName(StringUtils.hasText(jt.name()) ? jt.name()
						: (table.getAlias() + '_' + targetTable.getIdColumn().getName()));
				joinTableColumn.setReferencedColumnName(
						StringUtils.hasText(jt.referencedColumnName()) ? jt.referencedColumnName() : table.getIdColumn().getName());
				return joinTableColumn;
			}).toArray(JoinTableColumn[]::new);
		} else {
			JoinTableColumn joinTableColumn = new JoinTableColumn(this.inverseJoinTable);
			joinTableColumn.setProperty(property);
			joinTableColumn.setPrefix(table.getAlias());
			joinTableColumn.setName(table.getAlias() + "_" + targetTable.getIdColumn().getName());
			joinTableColumn.setReferencedColumnName(table.getIdColumn().getName());
			this.joinColumns = new JoinTableColumn[] { joinTableColumn };
		}

		mappingColumn = new Column(this.inverseJoinTable);
		javax.persistence.Column columnAnnotation = property.findAnnotation(javax.persistence.Column.class);
		if (null != columnAnnotation && StringUtils.hasText(columnAnnotation.name())) {
			mappingColumn.setName(columnAnnotation.name());
		} else {
			mappingColumn.setName(property.getName());
		}
		mappingColumn.setPrefix(this.inverseJoinTable.getAlias());
		mappingColumn.setProperty(property);
		mappingColumn.setColumnAnnotation(columnAnnotation);

	}

}
