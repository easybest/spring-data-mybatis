package org.springframework.data.mybatis.dialect;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mybatis.dialect.function.SQLFunction;
import org.springframework.data.mybatis.dialect.identity.IdentityColumnSupport;
import org.springframework.data.mybatis.dialect.identity.impl.IdentityColumnSupportImpl;
import org.springframework.data.mybatis.dialect.pagination.LimitHandler;

import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
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
		log.info("USE DIALECT: " + getClass().getName());
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

}
