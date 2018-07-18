package org.springframework.data.mybatis.mapping;

import lombok.Getter;
import lombok.Setter;

import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.util.StringUtils;

/**
 * @author Jarvis Song
 */
@Getter
@Setter
public class JoinColumn extends Column {

	protected String referencedColumnName;

	public JoinColumn(Table table) {
		super(table);
	}

	public String getActualReferencedColumnName(Dialect d) {
		if (StringUtils.hasText(referencedColumnName) && Dialect.QUOTE.indexOf(referencedColumnName.charAt(0)) > -1) {
			return d.openQuote() + referencedColumnName.substring(1, referencedColumnName.length() - 1) + d.closeQuote();
		}
		return referencedColumnName;
	}
}
