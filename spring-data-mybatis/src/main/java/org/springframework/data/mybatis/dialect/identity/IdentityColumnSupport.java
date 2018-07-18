package org.springframework.data.mybatis.dialect.identity;

import org.springframework.data.mapping.MappingException;

public interface IdentityColumnSupport {

	/**
	 * Does this dialect support identity column key generation?
	 *
	 * @return True if IDENTITY columns are supported; false otherwise.
	 */
	boolean supportsIdentityColumns();

	/**
	 * Does the dialect support some form of inserting and selecting the generated IDENTITY value all in the same
	 * statement.
	 *
	 * @return True if the dialect supports selecting the just generated IDENTITY in the insert statement.
	 */
	boolean supportsInsertSelectIdentity();

	/**
	 * Whether this dialect have an Identity clause added to the data type or a completely separate identity data type
	 *
	 * @return boolean
	 */
	boolean hasDataTypeInIdentityColumn();

	/**
	 * Provided we {@link #supportsInsertSelectIdentity}, then attach the "select identity" clause to the insert
	 * statement.
	 * <p/>
	 * Note, if {@link #supportsInsertSelectIdentity} == false then the insert-string should be returned without
	 * modification.
	 *
	 * @param insertString The insert command
	 * @return The insert command with any necessary identity select clause attached.
	 */
	String appendIdentitySelectToInsert(String insertString);

	/**
	 * Get the select command to use to retrieve the last generated IDENTITY value for a particular table
	 *
	 * @param table The table into which the insert was done
	 * @param column The PK column.
	 * @param type The {@link java.sql.Types} type code.
	 * @return The appropriate select command
	 * @throws MappingException If IDENTITY generation is not supported.
	 */
	String getIdentitySelectString(String table, String column, int type) throws MappingException;

	/**
	 * The syntax used during DDL to define a column as being an IDENTITY of a particular type.
	 *
	 * @param type The {@link java.sql.Types} type code.
	 * @return The appropriate DDL fragment.
	 * @throws MappingException If IDENTITY generation is not supported.
	 */
	String getIdentityColumnString(int type) throws MappingException;

	/**
	 * The keyword used to insert a generated value into an identity column (or null). Need if the dialect does not
	 * support inserts that specify no column values.
	 *
	 * @return The appropriate keyword.
	 */
	String getIdentityInsertString();

}
