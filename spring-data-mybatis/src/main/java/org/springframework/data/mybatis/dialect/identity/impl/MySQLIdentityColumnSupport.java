package org.springframework.data.mybatis.dialect.identity.impl;

/**
 * @author Jarvis Song
 */
public class MySQLIdentityColumnSupport extends IdentityColumnSupportImpl {

	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type) {
		return "select last_insert_id()";
	}

	@Override
	public String getIdentityColumnString(int type) {
		// starts with 1, implicitly
		return "not null auto_increment";
	}
}
