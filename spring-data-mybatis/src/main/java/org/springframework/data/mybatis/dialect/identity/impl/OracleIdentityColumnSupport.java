package org.springframework.data.mybatis.dialect.identity.impl;

public class OracleIdentityColumnSupport extends IdentityColumnSupportImpl {

	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public boolean supportsInsertSelectIdentity() {
		return true;
	}

	@Override
	public String getIdentityColumnString(int type) {
		return "generated as identity";
	}
}
