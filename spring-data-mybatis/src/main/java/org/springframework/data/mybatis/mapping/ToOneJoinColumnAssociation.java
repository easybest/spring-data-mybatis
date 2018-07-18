package org.springframework.data.mybatis.mapping;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mapping.MappingException;
import org.springframework.util.StringUtils;

import javax.persistence.JoinColumns;
import java.util.stream.Stream;

/**
 * @author Jarvis Song
 */
@Getter
@Setter
public class ToOneJoinColumnAssociation extends ToOneAssociation {

	protected JoinColumn[] joinColumns;

	public ToOneJoinColumnAssociation(Table table, Table targetTable, MyBatisPersistentProperty property) {
		super(table, targetTable, property);

		if (property.isAnnotationPresent(JoinColumns.class)
				&& property.findAnnotation(JoinColumns.class).value().length > 0) {
			this.joinColumns = Stream.of(property.findAnnotation(JoinColumns.class).value())
					.map(jc -> extractJoinColumn(table, targetTable, property, jc)).toArray(JoinColumn[]::new);
		} else if (property.isAnnotationPresent(javax.persistence.JoinColumn.class)) {
			this.joinColumns = new JoinColumn[] { extractJoinColumn(table, targetTable, property,
					property.getRequiredAnnotation(javax.persistence.JoinColumn.class)) };
		} else {
			Column targetColumn = targetTable.getIdColumn();
			JoinColumn joinColumn = new JoinColumn(table);
			joinColumn.setName(property.getName() + '.' + table.getIdColumn().getName());
			joinColumn.setReferencedColumnName(targetColumn.getName());
			joinColumn.setAlias(property.getName() + '.' + targetColumn.getAlias());
			joinColumn.setPrefix(table.getAlias());
			joinColumn.setProperty(targetColumn.getProperty());
		}
	}

	private JoinColumn extractJoinColumn(Table table, Table targetTable, MyBatisPersistentProperty property,
			javax.persistence.JoinColumn jc) {
		Column targetColumn = targetTable.getColumnsIncludeId().stream()
				.filter(column -> column.getName().equals(
						StringUtils.hasText(jc.referencedColumnName()) ? jc.referencedColumnName() : table.getIdColumn().getName()))
				.findFirst()
				.orElseThrow(() -> new MappingException("could not find referencedColumnName: "
						+ (StringUtils.hasText(jc.referencedColumnName()) ? jc.referencedColumnName()
								: table.getIdColumn().getName())
						+ " in " + targetTable.getName()));

		JoinColumn joinColumn = new JoinColumn(table);
		joinColumn.setName(
				StringUtils.hasText(jc.name()) ? jc.name() : (property.getName() + '.' + table.getIdColumn().getName()));
		joinColumn.setReferencedColumnName(
				StringUtils.hasText(jc.referencedColumnName()) ? jc.referencedColumnName() : targetColumn.getName());
		joinColumn.setAlias(property.getName() + '.' + targetColumn.getAlias());
		joinColumn.setPrefix(table.getAlias());
		joinColumn.setProperty(targetColumn.getProperty());
		return joinColumn;
	}
}
