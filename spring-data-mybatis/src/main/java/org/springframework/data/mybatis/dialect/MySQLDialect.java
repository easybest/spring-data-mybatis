package org.springframework.data.mybatis.dialect;

import org.springframework.data.mybatis.dialect.identity.IdentityColumnSupport;
import org.springframework.data.mybatis.dialect.identity.impl.MySQLIdentityColumnSupport;
import org.springframework.data.mybatis.dialect.pagination.AbstractLimitHandler;
import org.springframework.data.mybatis.dialect.pagination.LimitHandler;
import org.springframework.data.mybatis.dialect.pagination.LimitHelper;

import java.sql.Types;

/**
 * @author Jarvis Song
 */
public class MySQLDialect extends Dialect {

	private MySQLStorageEngine storageEngine;

	private static final LimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public String processSql(String sql, RowSelection selection) {
			if (LimitHelper.hasMaxRows(selection)) {
				return sql + " limit " + (LimitHelper.hasFirstRow(selection) ? (LimitHelper.getFirstRow(selection) + ",") : "")
						+ selection.getMaxRows();
			}

			// return sql + (hasOffset ? " limit #{pageSize}, #{offset}" : " limit #{pageSize}");
			return sql + " limit #{offset}, #{pageSize}";
		}

		@Override
		public boolean supportsLimit() {
			return true;
		}
	};

	public MySQLDialect() {
		super();
		// FIXME judge mysql storage engine.
		this.storageEngine = getDefaultMySQLStorageEngine();

		registerColumnType(Types.BIT, "bit");
		registerColumnType(Types.BIGINT, "bigint");
		registerColumnType(Types.SMALLINT, "smallint");
		registerColumnType(Types.TINYINT, "tinyint");
		registerColumnType(Types.INTEGER, "integer");
		registerColumnType(Types.CHAR, "char(1)");
		registerColumnType(Types.FLOAT, "float");
		registerColumnType(Types.DOUBLE, "double precision");
		registerColumnType(Types.BOOLEAN, "bit"); // HHH-6935
		registerColumnType(Types.DATE, "date");
		registerColumnType(Types.TIME, "time");
		registerColumnType(Types.TIMESTAMP, "datetime");
		registerColumnType(Types.VARBINARY, "longblob");
		registerColumnType(Types.VARBINARY, 16777215, "mediumblob");
		registerColumnType(Types.VARBINARY, 65535, "blob");
		registerColumnType(Types.VARBINARY, 255, "tinyblob");
		registerColumnType(Types.BINARY, "binary($l)");
		registerColumnType(Types.LONGVARBINARY, "longblob");
		registerColumnType(Types.LONGVARBINARY, 16777215, "mediumblob");
		registerColumnType(Types.NUMERIC, "decimal($p,$s)");
		registerColumnType(Types.BLOB, "longblob");
		// registerColumnType( Types.BLOB, 16777215, "mediumblob" );
		// registerColumnType( Types.BLOB, 65535, "blob" );
		registerColumnType(Types.CLOB, "longtext");
		registerColumnType(Types.NCLOB, "longtext");
		// registerColumnType( Types.CLOB, 16777215, "mediumtext" );
		// registerColumnType( Types.CLOB, 65535, "text" );
		registerVarcharTypes();

	}

	protected void registerVarcharTypes() {
		registerColumnType(Types.VARCHAR, "longtext");
		// registerColumnType( Types.VARCHAR, 16777215, "mediumtext" );
		// registerColumnType( Types.VARCHAR, 65535, "text" );
		registerColumnType(Types.VARCHAR, 255, "varchar($l)");
		registerColumnType(Types.LONGVARCHAR, "longtext");
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}

	@Override
	public char closeQuote() {
		return '`';
	}

	@Override
	public char openQuote() {
		return '`';
	}

	@Override
	public String getCastTypeName(int code) {
		switch (code) {
			case Types.BOOLEAN:
				return "char";
			case Types.INTEGER:
			case Types.BIGINT:
			case Types.SMALLINT:
				return smallIntegerCastTarget();
			case Types.FLOAT:
			case Types.REAL: {
				return floatingPointNumberCastTarget();
			}
			case Types.NUMERIC:
				return fixedPointNumberCastTarget();
			case Types.VARCHAR:
				return "char";
			case Types.VARBINARY:
				return "binary";
			default:
				return super.getCastTypeName(code);
		}
	}

	/**
	 * Determine the cast target for {@link Types#INTEGER}, {@link Types#BIGINT} and {@link Types#SMALLINT}
	 *
	 * @return The proper cast target type.
	 */
	protected String smallIntegerCastTarget() {
		return "signed";
	}

	/**
	 * Determine the cast target for {@link Types#FLOAT} and {@link Types#REAL} (DOUBLE)
	 *
	 * @return The proper cast target type.
	 */
	protected String floatingPointNumberCastTarget() {
		// MySQL does not allow casting to DOUBLE nor FLOAT, so we have to cast these as DECIMAL.
		// MariaDB does allow casting to DOUBLE, although not FLOAT.
		return fixedPointNumberCastTarget();
	}

	/**
	 * Determine the cast target for {@link Types#NUMERIC}
	 *
	 * @return The proper cast target type.
	 */
	protected String fixedPointNumberCastTarget() {
		// NOTE : the precision/scale are somewhat arbitrary choices, but MySQL/MariaDB
		// effectively require *some* values
		return "decimal(" + DEFAULT_PRECISION + "," + DEFAULT_SCALE + ")";
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select now()";
	}

	// locking support

	@Override
	public String getForUpdateString() {
		return " for update";
	}

	@Override
	public String getWriteLockString(int timeout) {
		return " for update";
	}

	@Override
	public String getReadLockString(int timeout) {
		return " lock in share mode";
	}

	@Override
	public boolean supportsLockTimeouts() {
		// yes, we do handle "lock timeout" conditions in the exception conversion delegate,
		// but that's a hardcoded lock timeout period across the whole entire MySQL database.
		// MySQL does not support specifying lock timeouts as part of the SQL statement, which is really
		// what this meta method is asking.
		return false;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new MySQLIdentityColumnSupport();
	}

	protected String getEngineKeyword() {
		return "type";
	}

	protected MySQLStorageEngine getDefaultMySQLStorageEngine() {
		return InnoDBStorageEngine.INSTANCE;
	}

}
