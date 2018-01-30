package org.springframework.data.mybatis.dialect.identity.impl;

import java.sql.Types;

/**
 * @author Jarvis Song
 */
public class PostgreSQL81IdentityColumnSupport extends IdentityColumnSupportImpl {

	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type) {
		return "select currval('" + table + '_' + column + "_seq')";
	}

	@Override
	public String getIdentityColumnString(int type) {
		return type == Types.BIGINT ? "bigserial not null" : "serial not null";
	}

	@Override
	public boolean hasDataTypeInIdentityColumn() {
		return false;
	}

}
