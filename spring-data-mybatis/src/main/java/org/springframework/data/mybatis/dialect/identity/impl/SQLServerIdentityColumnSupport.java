package org.springframework.data.mybatis.dialect.identity.impl;

/**
 * @author Jarvis Song
 */
public class SQLServerIdentityColumnSupport extends AbstractTransactSQLIdentityColumnSupport {
	/**
	 * Use <tt>insert table(...) values(...) select SCOPE_IDENTITY()</tt>
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public String appendIdentitySelectToInsert(String insertSQL) {
		return insertSQL + " select scope_identity()";
	}
}
