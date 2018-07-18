package org.springframework.data.mybatis.dialect;

public class MySQL5Dialect extends MySQLDialect {
	public MySQL5Dialect() {
		super();
	}

	protected String getEngineKeyword() {
		return "engine";
	}

}
