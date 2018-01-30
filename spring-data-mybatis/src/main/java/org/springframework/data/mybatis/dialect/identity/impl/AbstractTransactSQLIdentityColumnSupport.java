package org.springframework.data.mybatis.dialect.identity.impl;

import org.springframework.data.mapping.MappingException;

/**
 * @author Jarvis Song
 */
public class AbstractTransactSQLIdentityColumnSupport extends IdentityColumnSupportImpl {
	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public String getIdentityColumnString(int type) throws MappingException {
		// starts with 1, implicitly
		return "identity not null";
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type) throws MappingException {
		return "select @@identity";
	}

	@Override
	public boolean supportsInsertSelectIdentity() {
		return true;
	}

	@Override
	public String appendIdentitySelectToInsert(String insertSQL) {
		return insertSQL + "\nselect @@identity";
	}
}
