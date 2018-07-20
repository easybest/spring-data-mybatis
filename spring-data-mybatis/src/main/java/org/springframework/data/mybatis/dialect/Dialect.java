package org.springframework.data.mybatis.dialect;

import lombok.extern.slf4j.Slf4j;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mybatis.dialect.identity.IdentityColumnSupport;
import org.springframework.data.mybatis.dialect.identity.impl.IdentityColumnSupportImpl;
import org.springframework.data.mybatis.dialect.pagination.LimitHandler;

@Slf4j
public abstract class Dialect {

	/**
	 * Characters used as opening for quoting SQL identifiers
	 */
	public static final String QUOTE = "`\"[";

	/**
	 * Characters used as closing for quoting SQL identifiers
	 */
	public static final String CLOSED_QUOTE = "`\"]";

	// private final UniqueDelegate uniqueDelegate;

	// constructors and factory methods ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	protected Dialect() {
		log.info("USE DIALECT: " + getClass().getName());

	}

	@Override
	public String toString() {
		return getClass().getName();
	}

	// native identifier generation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Resolves the native generation strategy associated to this dialect.
	 * 
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
	 * Generate the appropriate select statement to to retrieve the next value of a sequence.
	 * 
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
	 * 
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

	// miscellaneous support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * The name of the SQL function that transforms a string to lowercase
	 *
	 * @return The dialect-specific lowercase function.
	 */
	public String getLowercaseFunction() {
		return "lower";
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

}
