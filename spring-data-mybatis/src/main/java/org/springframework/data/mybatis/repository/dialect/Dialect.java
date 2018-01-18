package org.springframework.data.mybatis.repository.dialect;

import org.springframework.util.StringUtils;

public class Dialect {

	public String openQuote() {
		return "";
	}

	public String closeQuote() {
		return openQuote();
	}

	public String wrap(String field) {

		if (null == openQuote() || null == closeQuote() || StringUtils.isEmpty(field)) {
			return field;
		}
		return openQuote() + field + closeQuote();

	}

}
