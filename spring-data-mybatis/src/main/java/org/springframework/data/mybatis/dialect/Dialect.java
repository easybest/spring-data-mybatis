package org.springframework.data.mybatis.dialect;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mybatis.dialect.function.SQLFunction;
import org.springframework.data.mybatis.dialect.identity.IdentityColumnSupport;
import org.springframework.data.mybatis.dialect.identity.impl.IdentityColumnSupportImpl;
import org.springframework.data.mybatis.dialect.pagination.LimitHandler;
import org.springframework.data.mybatis.util.StringUtil;

import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Represents a dialect of SQL implemented by a particular RDBMS.
 *
 * @author Gavin King, David Channon
 * @author Jarvis Song
 */
public abstract class Dialect {

	public static final int DEFAULT_LENGTH = 255;
	public static final int DEFAULT_PRECISION = 19;
	public static final int DEFAULT_SCALE = 2;
	/**
	 * Defines a default batch size constant
	 */
	public static final String DEFAULT_BATCH_SIZE = "15";

	/**
	 * Defines a "no batching" batch size constant
	 */
	public static final String NO_BATCH = "0";

	/**
	 * Characters used as opening for quoting SQL identifiers
	 */
	public static final String QUOTE = "`\"[";

	/**
	 * Characters used as closing for quoting SQL identifiers
	 */
	public static final String CLOSED_QUOTE = "`\"]";

	private final TypeNames typeNames = new TypeNames();
	private final Map<String, SQLFunction> sqlFunctions = new HashMap<>();
	private final Set<String> sqlKeywords = new HashSet<>();

	// private final UniqueDelegate uniqueDelegate;

	// constructors and factory methods ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	protected Dialect() {
		registerColumnType(Types.BIT, "bit");
		registerColumnType(Types.BOOLEAN, "boolean");
		registerColumnType(Types.TINYINT, "tinyint");
		registerColumnType(Types.SMALLINT, "smallint");
		registerColumnType(Types.INTEGER, "integer");
		registerColumnType(Types.BIGINT, "bigint");
		registerColumnType(Types.FLOAT, "float($p)");
		registerColumnType(Types.DOUBLE, "double precision");
		registerColumnType(Types.NUMERIC, "numeric($p,$s)");
		registerColumnType(Types.REAL, "real");

		registerColumnType(Types.DATE, "date");
		registerColumnType(Types.TIME, "time");
		registerColumnType(Types.TIMESTAMP, "timestamp");

		registerColumnType(Types.VARBINARY, "bit varying($l)");
		registerColumnType(Types.LONGVARBINARY, "bit varying($l)");
		registerColumnType(Types.BLOB, "blob");

		registerColumnType(Types.CHAR, "char($l)");
		registerColumnType(Types.VARCHAR, "varchar($l)");
		registerColumnType(Types.LONGVARCHAR, "varchar($l)");
		registerColumnType(Types.CLOB, "clob");

		registerColumnType(Types.NCHAR, "nchar($l)");
		registerColumnType(Types.NVARCHAR, "nvarchar($l)");
		registerColumnType(Types.LONGNVARCHAR, "nvarchar($l)");
		registerColumnType(Types.NCLOB, "nclob");

	}

	@Override
	public String toString() {
		return getClass().getName();
	}

	// database type mapping support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	/**
	 * Get the name of the database type associated with the given {@link java.sql.Types} typecode.
	 *
	 * @param code The {@link java.sql.Types} typecode
	 * @return the database type name
	 * @throws MappingException If no mapping was specified for that type.
	 */
	public String getTypeName(int code) throws MappingException {
		final String result = typeNames.get(code);
		if (result == null) {
			throw new MappingException("No default type mapping for (java.sql.Types) " + code);
		}
		return result;
	}

	/**
	 * Get the name of the database type associated with the given {@link java.sql.Types} typecode with the given storage
	 * specification parameters.
	 *
	 * @param code The {@link java.sql.Types} typecode
	 * @param length The datatype length
	 * @param precision The datatype precision
	 * @param scale The datatype scale
	 * @return the database type name
	 * @throws MappingException If no mapping was specified for that type.
	 */
	public String getTypeName(int code, long length, int precision, int scale) throws MappingException {
		final String result = typeNames.get(code, length, precision, scale);
		if (result == null) {
			throw new MappingException(
					String.format("No type mapping for java.sql.Types code: %s, length: %s", code, length));
		}
		return result;
	}

	/**
	 * Get the name of the database type appropriate for casting operations (via the CAST() SQL function) for the given
	 * {@link java.sql.Types} typecode.
	 *
	 * @param code The {@link java.sql.Types} typecode
	 * @return The database type name
	 */
	public String getCastTypeName(int code) {
		return getTypeName(code, DEFAULT_LENGTH, DEFAULT_PRECISION, DEFAULT_SCALE);
	}

	/**
	 * Return an expression casting the value to the specified type
	 *
	 * @param value The value to cast
	 * @param jdbcTypeCode The JDBC type code to cast to
	 * @param length The type length
	 * @param precision The type precision
	 * @param scale The type scale
	 * @return The cast expression
	 */
	public String cast(String value, int jdbcTypeCode, int length, int precision, int scale) {
		if (jdbcTypeCode == Types.CHAR) {
			return "cast(" + value + " as char(" + length + "))";
		} else {
			return "cast(" + value + "as " + getTypeName(jdbcTypeCode, length, precision, scale) + ")";
		}
	}

	/**
	 * Return an expression casting the value to the specified type. Simply calls
	 * {@link #cast(String, int, int, int, int)} passing {@link #DEFAULT_PRECISION} and {@link #DEFAULT_SCALE} as the
	 * precision/scale.
	 *
	 * @param value The value to cast
	 * @param jdbcTypeCode The JDBC type code to cast to
	 * @param length The type length
	 * @return The cast expression
	 */
	public String cast(String value, int jdbcTypeCode, int length) {
		return cast(value, jdbcTypeCode, length, DEFAULT_PRECISION, DEFAULT_SCALE);
	}

	/**
	 * Return an expression casting the value to the specified type. Simply calls
	 * {@link #cast(String, int, int, int, int)} passing {@link #DEFAULT_LENGTH} as the length
	 *
	 * @param value The value to cast
	 * @param jdbcTypeCode The JDBC type code to cast to
	 * @param precision The type precision
	 * @param scale The type scale
	 * @return The cast expression
	 */
	public String cast(String value, int jdbcTypeCode, int precision, int scale) {
		return cast(value, jdbcTypeCode, DEFAULT_LENGTH, precision, scale);
	}

	/**
	 * Subclasses register a type name for the given type code and maximum column length. <tt>$l</tt> in the type name
	 * with be replaced by the column length (if appropriate).
	 *
	 * @param code The {@link java.sql.Types} typecode
	 * @param capacity The maximum length of database type
	 * @param name The database type name
	 */
	protected void registerColumnType(int code, long capacity, String name) {
		typeNames.put(code, capacity, name);
	}

	/**
	 * Subclasses register a type name for the given type code. <tt>$l</tt> in the type name with be replaced by the
	 * column length (if appropriate).
	 *
	 * @param code The {@link java.sql.Types} typecode
	 * @param name The database type name
	 */
	protected void registerColumnType(int code, String name) {
		typeNames.put(code, name);
	}

	// function support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	// native identifier generation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Resolves the native generation strategy associated to this dialect.
	 * <p/>
	 * Comes into play whenever the user specifies the native generator.
	 *
	 * @return The native generator strategy.
	 */
	public String getNativeIdentifierGeneratorStrategy() {
		if (getIdentityColumnSupport().supportsIdentityColumns()) {
			return "identity";
		} else {
			return "sequence";
		}
	}

	// IDENTITY support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Get the appropriate {@link IdentityColumnSupport}
	 *
	 * @return the IdentityColumnSupport
	 */
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new IdentityColumnSupportImpl();
	}

	// SEQUENCE support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Does this dialect support sequences?
	 *
	 * @return True if sequences supported; false otherwise.
	 */
	public boolean supportsSequences() {
		return false;
	}

	/**
	 * Does this dialect support "pooled" sequences. Not aware of a better name for this. Essentially can we specify the
	 * initial and increment values?
	 *
	 * @return True if such "pooled" sequences are supported; false otherwise.
	 * @see #getCreateSequenceStrings(String, int, int)
	 * @see #getCreateSequenceString(String, int, int)
	 */
	public boolean supportsPooledSequences() {
		return false;
	}

	/**
	 * Generate the appropriate select statement to to retrieve the next value of a sequence.
	 * <p/>
	 * This should be a "stand alone" select statement.
	 *
	 * @param sequenceName the name of the sequence
	 * @return String The "nextval" select string.
	 * @throws MappingException If sequences are not supported.
	 */
	public String getSequenceNextValString(String sequenceName) throws MappingException {
		throw new MappingException(getClass().getName() + " does not support sequences");
	}

	/**
	 * Generate the select expression fragment that will retrieve the next value of a sequence as part of another
	 * (typically DML) statement.
	 * <p/>
	 * This differs from {@link #getSequenceNextValString(String)} in that this should return an expression usable within
	 * another statement.
	 *
	 * @param sequenceName the name of the sequence
	 * @return The "nextval" fragment.
	 * @throws MappingException If sequences are not supported.
	 */
	public String getSelectSequenceNextValString(String sequenceName) throws MappingException {
		throw new MappingException(getClass().getName() + " does not support sequences");
	}

	/**
	 * An optional multi-line form for databases which {@link #supportsPooledSequences()}.
	 *
	 * @param sequenceName The name of the sequence
	 * @param initialValue The initial value to apply to 'create sequence' statement
	 * @param incrementSize The increment value to apply to 'create sequence' statement
	 * @return The sequence creation commands
	 * @throws MappingException If sequences are not supported.
	 */
	public String[] getCreateSequenceStrings(String sequenceName, int initialValue, int incrementSize)
			throws MappingException {
		return new String[] { getCreateSequenceString(sequenceName, initialValue, incrementSize) };
	}

	/**
	 * Typically dialects which support sequences can create a sequence with a single command. This is convenience form of
	 * {@link #getCreateSequenceStrings} to help facilitate that.
	 * <p/>
	 * Dialects which support sequences and can create a sequence in a single command need *only* override this method.
	 * Dialects which support sequences but require multiple commands to create a sequence should instead override
	 * {@link #getCreateSequenceStrings}.
	 *
	 * @param sequenceName The name of the sequence
	 * @return The sequence creation command
	 * @throws MappingException If sequences are not supported.
	 */
	protected String getCreateSequenceString(String sequenceName) throws MappingException {
		throw new MappingException(getClass().getName() + " does not support sequences");
	}

	/**
	 * Overloaded form of {@link #getCreateSequenceString(String)}, additionally taking the initial value and increment
	 * size to be applied to the sequence definition.
	 * </p>
	 * The default definition is to suffix {@link #getCreateSequenceString(String)} with the string: " start with
	 * {initialValue} increment by {incrementSize}" where {initialValue} and {incrementSize} are replacement placeholders.
	 * Generally dialects should only need to override this method if different key phrases are used to apply the
	 * allocation information.
	 *
	 * @param sequenceName The name of the sequence
	 * @param initialValue The initial value to apply to 'create sequence' statement
	 * @param incrementSize The increment value to apply to 'create sequence' statement
	 * @return The sequence creation command
	 * @throws MappingException If sequences are not supported.
	 */
	protected String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize)
			throws MappingException {
		if (supportsPooledSequences()) {
			return getCreateSequenceString(sequenceName) + " start with " + initialValue + " increment by " + incrementSize;
		}
		throw new MappingException(getClass().getName() + " does not support pooled sequences");
	}

	/**
	 * The multiline script used to drop a sequence.
	 *
	 * @param sequenceName The name of the sequence
	 * @return The sequence drop commands
	 * @throws MappingException If sequences are not supported.
	 */
	public String[] getDropSequenceStrings(String sequenceName) throws MappingException {
		return new String[] { getDropSequenceString(sequenceName) };
	}

	/**
	 * Typically dialects which support sequences can drop a sequence with a single command. This is convenience form of
	 * {@link #getDropSequenceStrings} to help facilitate that.
	 * <p/>
	 * Dialects which support sequences and can drop a sequence in a single command need *only* override this method.
	 * Dialects which support sequences but require multiple commands to drop a sequence should instead override
	 * {@link #getDropSequenceStrings}.
	 *
	 * @param sequenceName The name of the sequence
	 * @return The sequence drop commands
	 * @throws MappingException If sequences are not supported.
	 */
	protected String getDropSequenceString(String sequenceName) throws MappingException {
		throw new MappingException(getClass().getName() + " does not support sequences");
	}

	/**
	 * Get the select command used retrieve the names of all sequences.
	 *
	 * @return The select command; or null if sequences are not supported.
	 */
	public String getQuerySequencesString() {
		return null;
	}

	// GUID support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Get the command used to select a GUID from the underlying database.
	 * <p/>
	 * Optional operation.
	 *
	 * @return The appropriate command.
	 */
	public String getSelectGUIDString() {
		throw new UnsupportedOperationException(getClass().getName() + " does not support GUIDs");
	}

	// limit/offset support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Returns the delegate managing LIMIT clause.
	 *
	 * @return LIMIT clause delegate.
	 */
	public LimitHandler getLimitHandler() {
		return null;
	}

	// lock acquisition support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Informational metadata about whether this dialect is known to support specifying timeouts for requested lock
	 * acquisitions.
	 *
	 * @return True is this dialect supports specifying lock timeouts.
	 */
	public boolean supportsLockTimeouts() {
		return true;

	}

	/**
	 * If this dialect supports specifying lock timeouts, are those timeouts rendered into the <tt>SQL</tt> string as
	 * parameters. The implication is that Hibernate will need to bind the timeout value as a parameter in the
	 * {@link java.sql.PreparedStatement}. If true, the param position is always handled as the last parameter; if the
	 * dialect specifies the lock timeout elsewhere in the <tt>SQL</tt> statement then the timeout value should be
	 * directly rendered into the statement and this method should return false.
	 *
	 * @return True if the lock timeout is rendered into the <tt>SQL</tt> string as a parameter; false otherwise.
	 */
	public boolean isLockTimeoutParameterized() {
		return false;
	}

	/**
	 * Get the string to append to SELECT statements to acquire locks for this dialect.
	 *
	 * @return The appropriate <tt>FOR UPDATE</tt> clause string.
	 */
	public String getForUpdateString() {
		return " for update";
	}

	/**
	 * Get the string to append to SELECT statements to acquire WRITE locks for this dialect. Location of the of the
	 * returned string is treated the same as getForUpdateString.
	 *
	 * @param timeout in milliseconds, -1 for indefinite wait and 0 for no wait.
	 * @return The appropriate <tt>LOCK</tt> clause string.
	 */
	public String getWriteLockString(int timeout) {
		return getForUpdateString();
	}

	/**
	 * Get the string to append to SELECT statements to acquire WRITE locks for this dialect given the aliases of the
	 * columns to be write locked. Location of the of the returned string is treated the same as getForUpdateString.
	 *
	 * @param aliases The columns to be read locked.
	 * @param timeout in milliseconds, -1 for indefinite wait and 0 for no wait.
	 * @return The appropriate <tt>LOCK</tt> clause string.
	 */
	public String getWriteLockString(String aliases, int timeout) {
		// by default we simply return the getWriteLockString(timeout) result since
		// the default is to say no support for "FOR UPDATE OF ..."
		return getWriteLockString(timeout);
	}

	/**
	 * Get the string to append to SELECT statements to acquire READ locks for this dialect. Location of the of the
	 * returned string is treated the same as getForUpdateString.
	 *
	 * @param timeout in milliseconds, -1 for indefinite wait and 0 for no wait.
	 * @return The appropriate <tt>LOCK</tt> clause string.
	 */
	public String getReadLockString(int timeout) {
		return getForUpdateString();
	}

	/**
	 * Get the string to append to SELECT statements to acquire READ locks for this dialect given the aliases of the
	 * columns to be read locked. Location of the of the returned string is treated the same as getForUpdateString.
	 *
	 * @param aliases The columns to be read locked.
	 * @param timeout in milliseconds, -1 for indefinite wait and 0 for no wait.
	 * @return The appropriate <tt>LOCK</tt> clause string.
	 */
	public String getReadLockString(String aliases, int timeout) {
		// by default we simply return the getReadLockString(timeout) result since
		// the default is to say no support for "FOR UPDATE OF ..."
		return getReadLockString(timeout);
	}

	/**
	 * Is <tt>FOR UPDATE OF</tt> syntax supported?
	 *
	 * @return True if the database supports <tt>FOR UPDATE OF</tt> syntax; false otherwise.
	 */
	public boolean forUpdateOfColumns() {
		// by default we report no support
		return false;
	}

	/**
	 * Does this dialect support <tt>FOR UPDATE</tt> in conjunction with outer joined rows?
	 *
	 * @return True if outer joined rows can be locked via <tt>FOR UPDATE</tt>.
	 */
	public boolean supportsOuterJoinForUpdate() {
		return true;
	}

	/**
	 * Get the <tt>FOR UPDATE OF column_list</tt> fragment appropriate for this dialect given the aliases of the columns
	 * to be write locked.
	 *
	 * @param aliases The columns to be write locked.
	 * @return The appropriate <tt>FOR UPDATE OF column_list</tt> clause string.
	 */
	public String getForUpdateString(String aliases) {
		// by default we simply return the getForUpdateString() result since
		// the default is to say no support for "FOR UPDATE OF ..."
		return getForUpdateString();
	}

	/**
	 * Retrieves the <tt>FOR UPDATE NOWAIT</tt> syntax specific to this dialect.
	 *
	 * @return The appropriate <tt>FOR UPDATE NOWAIT</tt> clause string.
	 */
	public String getForUpdateNowaitString() {
		// by default we report no support for NOWAIT lock semantics
		return getForUpdateString();
	}

	/**
	 * Retrieves the <tt>FOR UPDATE SKIP LOCKED</tt> syntax specific to this dialect.
	 *
	 * @return The appropriate <tt>FOR UPDATE SKIP LOCKED</tt> clause string.
	 */
	public String getForUpdateSkipLockedString() {
		// by default we report no support for SKIP_LOCKED lock semantics
		return getForUpdateString();
	}

	/**
	 * Get the <tt>FOR UPDATE OF column_list NOWAIT</tt> fragment appropriate for this dialect given the aliases of the
	 * columns to be write locked.
	 *
	 * @param aliases The columns to be write locked.
	 * @return The appropriate <tt>FOR UPDATE OF colunm_list NOWAIT</tt> clause string.
	 */
	public String getForUpdateNowaitString(String aliases) {
		return getForUpdateString(aliases);
	}

	/**
	 * Get the <tt>FOR UPDATE OF column_list SKIP LOCKED</tt> fragment appropriate for this dialect given the aliases of
	 * the columns to be write locked.
	 *
	 * @param aliases The columns to be write locked.
	 * @return The appropriate <tt>FOR UPDATE colunm_list SKIP LOCKED</tt> clause string.
	 */
	public String getForUpdateSkipLockedString(String aliases) {
		return getForUpdateString(aliases);
	}

	// table support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Command used to create a table.
	 *
	 * @return The command used to create a table.
	 */
	public String getCreateTableString() {
		return "create table";
	}

	/**
	 * Command used to alter a table.
	 *
	 * @param tableName The name of the table to alter
	 * @return The command used to alter a table.
	 */
	public String getAlterTableString(String tableName) {
		final StringBuilder sb = new StringBuilder("alter table ");
		if (supportsIfExistsAfterAlterTable()) {
			sb.append("if exists ");
		}
		sb.append(tableName);
		return sb.toString();
	}

	/**
	 * Slight variation on {@link #getCreateTableString}. Here, we have the command used to create a table when there is
	 * no primary key and duplicate rows are expected.
	 * <p/>
	 * Most databases do not care about the distinction; originally added for Teradata support which does care.
	 *
	 * @return The command used to create a multiset table.
	 */
	public String getCreateMultisetTableString() {
		return getCreateTableString();
	}

	// callable statement support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Registers a parameter (either OUT, or the new REF_CURSOR param type available in Java 8) capable of returning
	 * {@link java.sql.ResultSet} *by position*. Pre-Java 8, registering such ResultSet-returning parameters varied
	 * greatly across database and drivers; hence its inclusion as part of the Dialect contract.
	 *
	 * @param statement The callable statement.
	 * @param position The bind position at which to register the output param.
	 * @return The number of (contiguous) bind positions used.
	 * @throws SQLException Indicates problems registering the param.
	 */
	public int registerResultSetOutParameter(CallableStatement statement, int position) throws SQLException {
		throw new UnsupportedOperationException(
				getClass().getName() + " does not support resultsets via stored procedures");
	}

	/**
	 * Registers a parameter (either OUT, or the new REF_CURSOR param type available in Java 8) capable of returning
	 * {@link java.sql.ResultSet} *by name*. Pre-Java 8, registering such ResultSet-returning parameters varied greatly
	 * across database and drivers; hence its inclusion as part of the Dialect contract.
	 *
	 * @param statement The callable statement.
	 * @param name The parameter name (for drivers which support named parameters).
	 * @return The number of (contiguous) bind positions used.
	 * @throws SQLException Indicates problems registering the param.
	 */
	@SuppressWarnings("UnusedParameters")
	public int registerResultSetOutParameter(CallableStatement statement, String name) throws SQLException {
		throw new UnsupportedOperationException(
				getClass().getName() + " does not support resultsets via stored procedures");
	}

	/**
	 * Given a callable statement previously processed by {@link #registerResultSetOutParameter}, extract the
	 * {@link java.sql.ResultSet} from the OUT parameter.
	 *
	 * @param statement The callable statement.
	 * @return The extracted result set.
	 * @throws SQLException Indicates problems extracting the result set.
	 */
	public ResultSet getResultSet(CallableStatement statement) throws SQLException {
		throw new UnsupportedOperationException(
				getClass().getName() + " does not support resultsets via stored procedures");
	}

	/**
	 * Given a callable statement previously processed by {@link #registerResultSetOutParameter}, extract the
	 * {@link java.sql.ResultSet}.
	 *
	 * @param statement The callable statement.
	 * @param position The bind position at which to register the output param.
	 * @return The extracted result set.
	 * @throws SQLException Indicates problems extracting the result set.
	 */
	@SuppressWarnings("UnusedParameters")
	public ResultSet getResultSet(CallableStatement statement, int position) throws SQLException {
		throw new UnsupportedOperationException(
				getClass().getName() + " does not support resultsets via stored procedures");
	}

	/**
	 * Given a callable statement previously processed by {@link #registerResultSetOutParameter}, extract the
	 * {@link java.sql.ResultSet} from the OUT parameter.
	 *
	 * @param statement The callable statement.
	 * @param name The parameter name (for drivers which support named parameters).
	 * @return The extracted result set.
	 * @throws SQLException Indicates problems extracting the result set.
	 */
	@SuppressWarnings("UnusedParameters")
	public ResultSet getResultSet(CallableStatement statement, String name) throws SQLException {
		throw new UnsupportedOperationException(
				getClass().getName() + " does not support resultsets via stored procedures");
	}

	// current timestamp support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Does this dialect support a way to retrieve the database's current timestamp value?
	 *
	 * @return True if the current timestamp can be retrieved; false otherwise.
	 */
	public boolean supportsCurrentTimestampSelection() {
		return false;
	}

	/**
	 * Should the value returned by {@link #getCurrentTimestampSelectString} be treated as callable. Typically this
	 * indicates that JDBC escape syntax is being used...
	 *
	 * @return True if the {@link #getCurrentTimestampSelectString} return is callable; false otherwise.
	 */
	public boolean isCurrentTimestampSelectStringCallable() {
		throw new UnsupportedOperationException("Database not known to define a current timestamp function");
	}

	/**
	 * Retrieve the command used to retrieve the current timestamp from the database.
	 *
	 * @return The command.
	 */
	public String getCurrentTimestampSelectString() {
		throw new UnsupportedOperationException("Database not known to define a current timestamp function");
	}

	/**
	 * The name of the database-specific SQL function for retrieving the current timestamp.
	 *
	 * @return The function name.
	 */
	public String getCurrentTimestampSQLFunctionName() {
		// the standard SQL function name is current_timestamp...
		return "current_timestamp";
	}

	// SQLException support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	// union subclass support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Given a {@link java.sql.Types} type code, determine an appropriate null value to use in a select clause.
	 * <p/>
	 * One thing to consider here is that certain databases might require proper casting for the nulls here since the
	 * select here will be part of a UNION/UNION ALL.
	 *
	 * @param sqlType The {@link java.sql.Types} type code.
	 * @return The appropriate select clause value fragment.
	 */
	public String getSelectClauseNullString(int sqlType) {
		return "null";
	}

	/**
	 * Does this dialect support UNION ALL, which is generally a faster variant of UNION?
	 *
	 * @return True if UNION ALL is supported; false otherwise.
	 */
	public boolean supportsUnionAll() {
		return false;
	}

	// miscellaneous support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * The fragment used to insert a row without specifying any column values. This is not possible on some databases.
	 *
	 * @return The appropriate empty values clause.
	 */
	public String getNoColumnsInsertString() {
		return "values ( )";
	}

	/**
	 * The name of the SQL function that transforms a string to lowercase
	 *
	 * @return The dialect-specific lowercase function.
	 */
	public String getLowercaseFunction() {
		return "lower";
	}

	/**
	 * The name of the SQL function that can do case insensitive <b>like</b> comparison.
	 *
	 * @return The dialect-specific "case insensitive" like function.
	 */
	public String getCaseInsensitiveLike() {
		return "like";
	}

	/**
	 * Does this dialect support case insensitive LIKE restrictions?
	 *
	 * @return {@code true} if the underlying database supports case insensitive like comparison, {@code false} otherwise.
	 *         The default is {@code false}.
	 */
	public boolean supportsCaseInsensitiveLike() {
		return false;
	}

	/**
	 * Meant as a means for end users to affect the select strings being sent to the database and perhaps manipulate them
	 * in some fashion.
	 * <p/>
	 *
	 * @param select The select command
	 * @return The mutated select command, or the same as was passed in.
	 */
	public String transformSelectString(String select) {
		return select;
	}

	/**
	 * What is the maximum length Spring Data MyBatis can use for generated aliases?
	 * <p/>
	 * The maximum here should account for the fact that Spring Data MyBatis often needs to append "uniqueing" information
	 * to the end of generated aliases. That "uniqueing" information will be added to the end of a identifier generated to
	 * the length specified here; so be sure to leave some room (generally speaking 5 positions will suffice).
	 *
	 * @return The maximum length.
	 */
	public int getMaxAliasLength() {
		return 10;
	}

	/**
	 * The SQL literal value to which this database maps boolean values.
	 *
	 * @param bool The boolean value
	 * @return The appropriate SQL literal.
	 */
	public String toBooleanValueString(boolean bool) {
		return bool ? "1" : "0";
	}

	// keyword support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	protected void registerKeyword(String word) {
		// When tokens are checked for keywords, they are always compared against the lower-case version of the token.
		// For instance, Template#renderWhereStringTemplate transforms all tokens to lower-case too.
		sqlKeywords.add(word.toLowerCase(Locale.ROOT));
	}

	// identifier quoting support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * The character specific to this dialect used to begin a quoted identifier.
	 *
	 * @return The dialect's specific open quote character.
	 */
	public char openQuote() {
		return '"';
	}

	/**
	 * The character specific to this dialect used to close a quoted identifier.
	 *
	 * @return The dialect's specific close quote character.
	 */
	public char closeQuote() {
		return '"';
	}

	/**
	 * Apply dialect-specific quoting.
	 * <p/>
	 * By default, the incoming value is checked to see if its first character is the back-tick (`). If so, the dialect
	 * specific quoting is applied.
	 *
	 * @param name The value to be quoted.
	 * @return The quoted (or unmodified, if not starting with back-tick) value.
	 * @see #openQuote()
	 * @see #closeQuote()
	 */
	public final String quote(String name) {
		if (name == null) {
			return null;
		}

		if (name.charAt(0) == '`') {
			return openQuote() + name.substring(1, name.length() - 1) + closeQuote();
		} else {
			return name;
		}
	}

	// DDL support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Does this dialect support catalog creation?
	 *
	 * @return True if the dialect supports catalog creation; false otherwise.
	 */
	public boolean canCreateCatalog() {
		return false;
	}

	/**
	 * Get the SQL command used to create the named catalog
	 *
	 * @param catalogName The name of the catalog to be created.
	 * @return The creation commands
	 */
	public String[] getCreateCatalogCommand(String catalogName) {
		throw new UnsupportedOperationException("No create catalog syntax supported by " + getClass().getName());
	}

	/**
	 * Get the SQL command used to drop the named catalog
	 *
	 * @param catalogName The name of the catalog to be dropped.
	 * @return The drop commands
	 */
	public String[] getDropCatalogCommand(String catalogName) {
		throw new UnsupportedOperationException("No drop catalog syntax supported by " + getClass().getName());
	}

	/**
	 * Does this dialect support schema creation?
	 *
	 * @return True if the dialect supports schema creation; false otherwise.
	 */
	public boolean canCreateSchema() {
		return true;
	}

	/**
	 * Get the SQL command used to create the named schema
	 *
	 * @param schemaName The name of the schema to be created.
	 * @return The creation commands
	 */
	public String[] getCreateSchemaCommand(String schemaName) {
		return new String[] { "create schema " + schemaName };
	}

	/**
	 * Get the SQL command used to drop the named schema
	 *
	 * @param schemaName The name of the schema to be dropped.
	 * @return The drop commands
	 */
	public String[] getDropSchemaCommand(String schemaName) {
		return new String[] { "drop schema " + schemaName };
	}

	/**
	 * Get the SQL command used to retrieve the current schema name. Works in conjunction with getSchemaNameResolver() ,
	 * unless the return from there does not need this information. E.g., a custom impl might make use of the Java 1.7
	 * addition of the {@link java.sql.Connection#getSchema()} method
	 *
	 * @return The current schema retrieval SQL
	 */
	public String getCurrentSchemaCommand() {
		return null;
	}

	/**
	 * Does this dialect support the <tt>ALTER TABLE</tt> syntax?
	 *
	 * @return True if we support altering of tables; false otherwise.
	 */
	public boolean hasAlterTable() {
		return true;
	}

	/**
	 * Do we need to drop constraints before dropping tables in this dialect?
	 *
	 * @return True if constraints must be dropped prior to dropping the table; false otherwise.
	 */
	public boolean dropConstraints() {
		return true;
	}

	/**
	 * Do we need to qualify index names with the schema name?
	 *
	 * @return boolean
	 */
	public boolean qualifyIndexName() {
		return true;
	}

	/**
	 * The syntax used to add a column to a table (optional).
	 *
	 * @return The "add column" fragment.
	 */
	public String getAddColumnString() {
		throw new UnsupportedOperationException("No add column syntax supported by " + getClass().getName());
	}

	/**
	 * The syntax for the suffix used to add a column to a table (optional).
	 *
	 * @return The suffix "add column" fragment.
	 */
	public String getAddColumnSuffixString() {
		return "";
	}

	public String getDropForeignKeyString() {
		return " drop constraint ";
	}

	public String getTableTypeString() {
		// grrr... for differentiation of mysql storage engines
		return "";
	}

	/**
	 * The syntax used to add a foreign key constraint to a table.
	 *
	 * @param constraintName The FK constraint name.
	 * @param foreignKey The names of the columns comprising the FK
	 * @param referencedTable The table referenced by the FK
	 * @param primaryKey The explicit columns in the referencedTable referenced by this FK.
	 * @param referencesPrimaryKey if false, constraint should be explicit about which column names the constraint refers
	 *          to
	 * @return the "add FK" fragment
	 */
	public String getAddForeignKeyConstraintString(String constraintName, String[] foreignKey, String referencedTable,
			String[] primaryKey, boolean referencesPrimaryKey) {
		final StringBuilder res = new StringBuilder(30);

		res.append(" add constraint ").append(quote(constraintName)).append(" foreign key (")
				.append(StringUtil.join(", ", foreignKey)).append(") references ").append(referencedTable);

		if (!referencesPrimaryKey) {
			res.append(" (").append(StringUtil.join(", ", primaryKey)).append(')');
		}

		return res.toString();
	}

	public String getAddForeignKeyConstraintString(String constraintName, String foreignKeyDefinition) {
		return new StringBuilder(30).append(" add constraint ").append(quote(constraintName)).append(" ")
				.append(foreignKeyDefinition).toString();
	}

	/**
	 * The syntax used to add a primary key constraint to a table.
	 *
	 * @param constraintName The name of the PK constraint.
	 * @return The "add PK" fragment
	 */
	public String getAddPrimaryKeyConstraintString(String constraintName) {
		return " add constraint " + constraintName + " primary key ";
	}

	/**
	 * Does the database/driver have bug in deleting rows that refer to other rows being deleted in the same query?
	 *
	 * @return {@code true} if the database/driver has this bug
	 */
	public boolean hasSelfReferentialForeignKeyBug() {
		return false;
	}

	/**
	 * The keyword used to specify a nullable column.
	 *
	 * @return String
	 */
	public String getNullColumnString() {
		return "";
	}

	/**
	 * Does this dialect/database support commenting on tables, columns, etc?
	 *
	 * @return {@code true} if commenting is supported
	 */
	public boolean supportsCommentOn() {
		return false;
	}

	/**
	 * Get the comment into a form supported for table definition.
	 *
	 * @param comment The comment to apply
	 * @return The comment fragment
	 */
	public String getTableComment(String comment) {
		return "";
	}

	/**
	 * Get the comment into a form supported for column definition.
	 *
	 * @param comment The comment to apply
	 * @return The comment fragment
	 */
	public String getColumnComment(String comment) {
		return "";
	}

	/**
	 * For dropping a table, can the phrase "if exists" be applied before the table name?
	 * <p/>
	 * NOTE : Only one or the other (or neither) of this and {@link #supportsIfExistsAfterTableName} should return true
	 *
	 * @return {@code true} if the "if exists" can be applied before the table name
	 */
	public boolean supportsIfExistsBeforeTableName() {
		return false;
	}

	/**
	 * For dropping a table, can the phrase "if exists" be applied after the table name?
	 * <p/>
	 * NOTE : Only one or the other (or neither) of this and {@link #supportsIfExistsBeforeTableName} should return true
	 *
	 * @return {@code true} if the "if exists" can be applied after the table name
	 */
	public boolean supportsIfExistsAfterTableName() {
		return false;
	}

	/**
	 * For dropping a constraint with an "alter table", can the phrase "if exists" be applied before the constraint name?
	 * <p/>
	 * NOTE : Only one or the other (or neither) of this and {@link #supportsIfExistsAfterConstraintName} should return
	 * true
	 *
	 * @return {@code true} if the "if exists" can be applied before the constraint name
	 */
	public boolean supportsIfExistsBeforeConstraintName() {
		return false;
	}

	/**
	 * For dropping a constraint with an "alter table", can the phrase "if exists" be applied after the constraint name?
	 * <p/>
	 * NOTE : Only one or the other (or neither) of this and {@link #supportsIfExistsBeforeConstraintName} should return
	 * true
	 *
	 * @return {@code true} if the "if exists" can be applied after the constraint name
	 */
	public boolean supportsIfExistsAfterConstraintName() {
		return false;
	}

	/**
	 * For an "alter table", can the phrase "if exists" be applied?
	 *
	 * @return {@code true} if the "if exists" can be applied after ALTER TABLE
	 */
	public boolean supportsIfExistsAfterAlterTable() {
		return false;
	}

	/**
	 * Generate a DROP TABLE statement
	 *
	 * @param tableName The name of the table to drop
	 * @return The DROP TABLE command
	 */
	public String getDropTableString(String tableName) {
		final StringBuilder buf = new StringBuilder("drop table ");
		if (supportsIfExistsBeforeTableName()) {
			buf.append("if exists ");
		}
		buf.append(tableName).append(getCascadeConstraintsString());
		if (supportsIfExistsAfterTableName()) {
			buf.append(" if exists");
		}
		return buf.toString();
	}

	/**
	 * Does this dialect support column-level check constraints?
	 *
	 * @return True if column-level CHECK constraints are supported; false otherwise.
	 */
	public boolean supportsColumnCheck() {
		return true;
	}

	/**
	 * Does this dialect support table-level check constraints?
	 *
	 * @return True if table-level CHECK constraints are supported; false otherwise.
	 */
	public boolean supportsTableCheck() {
		return true;
	}

	/**
	 * Does this dialect support cascaded delete on foreign key definitions?
	 *
	 * @return {@code true} indicates that the dialect does support cascaded delete on foreign keys.
	 */
	public boolean supportsCascadeDelete() {
		return true;
	}

	/**
	 * Completely optional cascading drop clause
	 *
	 * @return String
	 */
	public String getCascadeConstraintsString() {
		return "";
	}

	/**
	 * Returns the separator to use for defining cross joins when translating HQL queries.
	 * <p/>
	 * Typically this will be either [<tt> cross join </tt>] or [<tt>, </tt>]
	 * <p/>
	 * Note that the spaces are important!
	 *
	 * @return The cross join separator
	 */
	public String getCrossJoinSeparator() {
		return " cross join ";
	}

	// Informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Does this dialect support empty IN lists?
	 * <p/>
	 * For example, is [where XYZ in ()] a supported construct?
	 *
	 * @return True if empty in lists are supported; false otherwise.
	 */
	public boolean supportsEmptyInList() {
		return true;
	}

	/**
	 * Are string comparisons implicitly case insensitive.
	 * <p/>
	 * In other words, does [where 'XYZ' = 'xyz'] resolve to true?
	 *
	 * @return True if comparisons are case insensitive.
	 */
	public boolean areStringComparisonsCaseInsensitive() {
		return false;
	}

	/**
	 * Is this dialect known to support what ANSI-SQL terms "row value constructor" syntax; sometimes called tuple syntax.
	 * <p/>
	 * Basically, does it support syntax like "... where (FIRST_NAME, LAST_NAME) = ('Steve', 'Ebersole') ...".
	 *
	 * @return True if this SQL dialect is known to support "row value constructor" syntax; false otherwise.
	 */
	public boolean supportsRowValueConstructorSyntax() {
		// return false here, as most databases do not properly support this construct...
		return false;
	}

	/**
	 * If the dialect supports {@link #supportsRowValueConstructorSyntax() row values}, does it offer such support in IN
	 * lists as well?
	 * <p/>
	 * For example, "... where (FIRST_NAME, LAST_NAME) IN ( (?, ?), (?, ?) ) ..."
	 *
	 * @return True if this SQL dialect is known to support "row value constructor" syntax in the IN list; false
	 *         otherwise.
	 */
	public boolean supportsRowValueConstructorSyntaxInInList() {
		return false;
	}

	/**
	 * Should LOBs (both BLOB and CLOB) be bound using stream operations (i.e.
	 * {@link java.sql.PreparedStatement#setBinaryStream}).
	 *
	 * @return True if BLOBs and CLOBs should be bound using stream operations.
	 */
	public boolean useInputStreamToInsertBlob() {
		return true;
	}

	/**
	 * Does this dialect support parameters within the <tt>SELECT</tt> clause of <tt>INSERT ... SELECT ...</tt>
	 * statements?
	 *
	 * @return True if this is supported; false otherwise.
	 */
	public boolean supportsParametersInInsertSelect() {
		return true;
	}

	/**
	 * Does this dialect require that references to result variables (i.e, select expresssion aliases) in an ORDER BY
	 * clause be replaced by column positions (1-origin) as defined by the select clause?
	 * 
	 * @return true if result variable references in the ORDER BY clause should be replaced by column positions; false
	 *         otherwise.
	 */
	public boolean replaceResultVariableInOrderByClauseWithPosition() {
		return false;
	}

	/**
	 * Does this dialect require that parameters appearing in the <tt>SELECT</tt> clause be wrapped in <tt>cast()</tt>
	 * calls to tell the db parser the expected type.
	 *
	 * @return True if select clause parameter must be cast()ed
	 */
	public boolean requiresCastingOfParametersInSelectClause() {
		return false;
	}

	/**
	 * Does this dialect support asking the result set its positioning information on forward only cursors. Specifically,
	 * in the case of scrolling fetches, needs to use {@link java.sql.ResultSet#isAfterLast} and
	 * {@link java.sql.ResultSet#isBeforeFirst}. Certain drivers do not allow access to these methods for forward only
	 * cursors.
	 * <p/>
	 * NOTE : this is highly driver dependent!
	 *
	 * @return True if methods like {@link java.sql.ResultSet#isAfterLast} and {@link java.sql.ResultSet#isBeforeFirst}
	 *         are supported for forward only cursors; false otherwise.
	 */
	public boolean supportsResultSetPositionQueryMethodsOnForwardOnlyCursor() {
		return true;
	}

	/**
	 * Does this dialect support definition of cascade delete constraints which can cause circular chains?
	 *
	 * @return True if circular cascade delete constraints are supported; false otherwise.
	 */
	public boolean supportsCircularCascadeDeleteConstraints() {
		return true;
	}

	/**
	 * Are subselects supported as the left-hand-side (LHS) of IN-predicates.
	 * <p/>
	 * In other words, is syntax like "... <subquery> IN (1, 2, 3) ..." supported?
	 *
	 * @return True if subselects can appear as the LHS of an in-predicate; false otherwise.
	 */
	public boolean supportsSubselectAsInPredicateLHS() {
		return true;
	}

	/**
	 * Expected LOB usage pattern is such that I can perform an insert via prepared statement with a parameter binding for
	 * a LOB value without crazy casting to JDBC driver implementation-specific classes...
	 * <p/>
	 * Part of the trickiness here is the fact that this is largely driver dependent. For example, Oracle (which is
	 * notoriously bad with LOB support in their drivers historically) actually does a pretty good job with LOB support as
	 * of the 10.2.x versions of their drivers...
	 *
	 * @return True if normal LOB usage patterns can be used with this driver; false if driver-specific hookiness needs to
	 *         be applied.
	 */
	public boolean supportsExpectedLobUsagePattern() {
		return true;
	}

	/**
	 * Does the dialect support propagating changes to LOB values back to the database? Talking about mutating the
	 * internal value of the locator as opposed to supplying a new locator instance...
	 * <p/>
	 * For BLOBs, the internal value might be changed by: {@link java.sql.Blob#setBinaryStream},
	 * {@link java.sql.Blob#setBytes(long, byte[])}, {@link java.sql.Blob#setBytes(long, byte[], int, int)}, or
	 * {@link java.sql.Blob#truncate(long)}.
	 * <p/>
	 * For CLOBs, the internal value might be changed by: {@link java.sql.Clob#setAsciiStream(long)},
	 * {@link java.sql.Clob#setCharacterStream(long)}, {@link java.sql.Clob#setString(long, String)},
	 * {@link java.sql.Clob#setString(long, String, int, int)}, or {@link java.sql.Clob#truncate(long)}.
	 * <p/>
	 * NOTE : I do not know the correct answer currently for databases which (1) are not part of the cruise control
	 * process or (2) do not {@link #supportsExpectedLobUsagePattern}.
	 *
	 * @return True if the changes are propagated back to the database; false otherwise.
	 */
	public boolean supportsLobValueChangePropogation() {
		// todo : pretty sure this is the same as the java.sql.DatabaseMetaData.locatorsUpdateCopy method added in JDBC 4,
		// see HHH-6046
		return true;
	}

	/**
	 * Is it supported to materialize a LOB locator outside the transaction in which it was created?
	 * <p/>
	 * Again, part of the trickiness here is the fact that this is largely driver dependent.
	 * <p/>
	 * NOTE: all database I have tested which {@link #supportsExpectedLobUsagePattern()} also support the ability to
	 * materialize a LOB outside the owning transaction...
	 *
	 * @return True if unbounded materialization is supported; false otherwise.
	 */
	public boolean supportsUnboundedLobLocatorMaterialization() {
		return true;
	}

	/**
	 * Does this dialect support referencing the table being mutated in a subquery. The "table being mutated" is the table
	 * referenced in an UPDATE or a DELETE query. And so can that table then be referenced in a subquery of said
	 * UPDATE/DELETE query.
	 * <p/>
	 * For example, would the following two syntaxes be supported:
	 * <ul>
	 * <li>delete from TABLE_A where ID not in ( select ID from TABLE_A )</li>
	 * <li>update TABLE_A set NON_ID = 'something' where ID in ( select ID from TABLE_A)</li>
	 * </ul>
	 *
	 * @return True if this dialect allows references the mutating table from a subquery.
	 */
	public boolean supportsSubqueryOnMutatingTable() {
		return true;
	}

	/**
	 * Does the dialect support an exists statement in the select clause?
	 *
	 * @return True if exists checks are allowed in the select clause; false otherwise.
	 */
	public boolean supportsExistsInSelect() {
		return true;
	}

	/**
	 * For the underlying database, is READ_COMMITTED isolation implemented by forcing readers to wait for write locks to
	 * be released?
	 *
	 * @return True if writers block readers to achieve READ_COMMITTED; false otherwise.
	 */
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		return false;
	}

	/**
	 * For the underlying database, is REPEATABLE_READ isolation implemented by forcing writers to wait for read locks to
	 * be released?
	 *
	 * @return True if readers block writers to achieve REPEATABLE_READ; false otherwise.
	 */
	public boolean doesRepeatableReadCauseReadersToBlockWriters() {
		return false;
	}

	/**
	 * Does this dialect support using a JDBC bind parameter as an argument to a function or procedure call?
	 *
	 * @return Returns {@code true} if the database supports accepting bind params as args, {@code false} otherwise. The
	 *         default is {@code true}.
	 */
	@SuppressWarnings({ "UnusedDeclaration" })
	public boolean supportsBindAsCallableArgument() {
		return true;
	}

	/**
	 * Does this dialect support `count(a,b)`?
	 *
	 * @return True if the database supports counting tuples; false otherwise.
	 */
	public boolean supportsTupleCounts() {
		return false;
	}

	/**
	 * Does this dialect support `count(distinct a,b)`?
	 *
	 * @return True if the database supports counting distinct tuples; false otherwise.
	 */
	public boolean supportsTupleDistinctCounts() {
		// oddly most database in fact seem to, so true is the default.
		return true;
	}

	/**
	 * If {@link #supportsTupleDistinctCounts()} is true, does the Dialect require the tuple to be wrapped with parens?
	 *
	 * @return boolean
	 */
	public boolean requiresParensForTupleDistinctCounts() {
		return false;
	}

	/**
	 * Return the limit that the underlying database places on the number elements in an {@code IN} predicate. If the
	 * database defines no such limits, simply return zero or less-than-zero.
	 *
	 * @return int The limit, or zero-or-less to indicate no limit.
	 */
	public int getInExpressionCountLimit() {
		return 0;
	}

	/**
	 * HHH-4635 Oracle expects all Lob values to be last in inserts and updates.
	 *
	 * @return boolean True of Lob values should be last, false if it does not matter.
	 */
	public boolean forceLobAsLastValue() {
		return false;
	}

	/**
	 * Negate an expression
	 *
	 * @param expression The expression to negate
	 * @return The negated expression
	 */
	public String getNotExpression(String expression) {
		return "not " + expression;
	}

	/**
	 * Apply a hint to the query. The entire query is provided, allowing the Dialect full control over the placement and
	 * syntax of the hint. By default, ignore the hint and simply return the query.
	 *
	 * @param query The query to which to apply the hint.
	 * @param hintList The hints to apply
	 * @return The modified SQL
	 */
	public String getQueryHintString(String query, List<String> hintList) {
		final String hints = StringUtil.join(", ", hintList.iterator());

		if (StringUtil.isEmpty(hints)) {
			return query;
		}

		return getQueryHintString(query, hints);
	}

	/**
	 * Apply a hint to the query. The entire query is provided, allowing the Dialect full control over the placement and
	 * syntax of the hint. By default, ignore the hint and simply return the query.
	 *
	 * @param query The query to which to apply the hint.
	 * @param hints The hints to apply
	 * @return The modified SQL
	 */
	public String getQueryHintString(String query, String hints) {
		return query;
	}

	/**
	 * Does this dialect support tuples in subqueries? Ex: delete from Table1 where (col1, col2) in (select col1, col2
	 * from Table2)
	 *
	 * @return boolean
	 */
	public boolean supportsTuplesInSubqueries() {
		return true;
	}

	/**
	 * Does the fetching JDBC statement warning for logging is enabled by default
	 *
	 * @return boolean
	 */
	public boolean isJdbcLogWarningsEnabledByDefault() {
		return true;
	}

	public void augmentRecognizedTableTypes(List<String> tableTypesList) {
		// noihing to do
	}

	/**
	 * Does the underlying database support partition by
	 *
	 * @return boolean
	 */
	public boolean supportsPartitionBy() {
		return false;
	}

	/**
	 * Override the DatabaseMetaData#supportsNamedParameters()
	 *
	 * @return boolean
	 */
	public boolean supportsNamedParameters(DatabaseMetaData databaseMetaData) throws SQLException {
		return databaseMetaData != null && databaseMetaData.supportsNamedParameters();
	}

	/**
	 * Does this dialect supports Nationalized Types
	 *
	 * @return boolean
	 */
	public boolean supportsNationalizedTypes() {
		return true;
	}

	/**
	 * Does this dialect/database support non-query statements (e.g. INSERT, UPDATE, DELETE) with CTE (Common Table
	 * Expressions)?
	 *
	 * @return {@code true} if non-query statements are supported with CTE
	 */
	public boolean supportsNonQueryWithCTE() {
		return false;
	}

	/**
	 * Does this dialect/database support VALUES list (e.g. VALUES (1), (2), (3) )
	 *
	 * @return {@code true} if VALUES list are supported
	 */
	public boolean supportsValuesList() {
		return false;
	}

	/**
	 * Does this dialect/database support SKIP_LOCKED timeout.
	 *
	 * @return {@code true} if SKIP_LOCKED is supported
	 */
	public boolean supportsSkipLocked() {
		return false;
	}

	/**
	 * Inline String literal.
	 *
	 * @return escaped String
	 */
	public String inlineLiteral(String literal) {
		return String.format("\'%s\'", escapeLiteral(literal));
	}

	/**
	 * Escape String literal.
	 *
	 * @return escaped String
	 */
	protected String escapeLiteral(String literal) {
		return literal.replace("'", "''");
	}

	protected String prependComment(String sql, String comment) {
		return "/* " + comment + " */ " + sql;
	}

}
