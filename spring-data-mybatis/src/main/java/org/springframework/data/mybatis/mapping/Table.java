package org.springframework.data.mybatis.mapping;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.util.StringUtils;

import javax.persistence.Index;
import javax.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Jarvis Song
 */
@Data
@NoArgsConstructor
public class Table {

	protected MyBatisPersistentEntity entity;

	protected String catalog;
	protected String schema;
	protected String name;
	protected String alias;

	protected IdColumn idColumn;
	/**
	 * all columns exclude primary key and associations(changed to column).
	 */
	protected List<Column> columns = new ArrayList<>();
	protected List<Association> associations = new ArrayList<>();

	protected List<UniqueConstraint> uniqueConstraints = new ArrayList<>();
	protected List<Index> indexes = new ArrayList<>();

	public void addColumn(Column column) {
		columns.add(column);
	}

	public void addAssociation(Association association) {
		associations.add(association);
	}

	public void addUniqueConstraint(UniqueConstraint constraint) {
		uniqueConstraints.add(constraint);
	}

	public void addIndex(Index index) {
		indexes.add(index);
	}

	public String getFullName(Dialect dialect) {
		StringBuilder builder = new StringBuilder();
		if (StringUtils.hasText(catalog)) {
			extractReal(dialect, builder, catalog);
			builder.append('.');
		}
		if (StringUtils.hasText(schema)) {
			extractReal(dialect, builder, schema);
			builder.append('.');
		}
		extractReal(dialect, builder, name);
		return builder.toString();
	}

	private void extractReal(Dialect dialect, StringBuilder builder, String content) {

		if (StringUtils.hasText(content) && Dialect.QUOTE.indexOf(content.charAt(0)) > -1) {
			builder.append(dialect.openQuote()).append(content, 1, content.length() - 1).append(dialect.closeQuote());
		} else {
			builder.append(content);
		}

	}

	public List<Column> getColumnsIncludeId() {
		List<Column> copy = new ArrayList<>(columns);
		if (null != idColumn) {
			if (idColumn instanceof CompositeIdColumn) {
				copy.addAll(((CompositeIdColumn) idColumn).getIdColumns());
			} else {
				copy.add(idColumn);
			}
		}

		if (!associations.isEmpty()) {
			associations.stream().filter(association -> association instanceof ToOneJoinColumnAssociation)
					.flatMap(association -> Arrays.stream(((ToOneJoinColumnAssociation) association).getJoinColumns()))
					.collect(Collectors.toCollection(() -> copy));
		}

		return copy;
	}

	public String getQuotedAlias(Dialect d) {
		return d.openQuote() + alias + d.closeQuote();
	}

	public Column findColumn(String propertyName) {
		if (null != idColumn && idColumn.getAlias().equals(propertyName)) {
			return idColumn;
		}
		return columns.stream().filter(column -> column.getAlias().equals(propertyName)).findFirst().orElse(null);

	}
}
