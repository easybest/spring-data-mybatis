package org.springframework.data.mybatis.dialect.identity.impl;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mybatis.dialect.identity.IdentityColumnSupport;

/**
 * @author Jarvis Song
 */
public class IdentityColumnSupportImpl implements IdentityColumnSupport {
	@Override
	public boolean supportsIdentityColumns() {
		return false;
	}

	@Override
	public boolean supportsInsertSelectIdentity() {
		return false;
	}

	@Override
	public boolean hasDataTypeInIdentityColumn() {
		return false;
	}

	@Override
	public String appendIdentitySelectToInsert(String insertString) {
		return null;
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type) throws MappingException {
		return null;
	}

	@Override
	public String getIdentityColumnString(int type) throws MappingException {
		return null;
	}

	@Override
	public String getIdentityInsertString() {
		return null;
	}
}
