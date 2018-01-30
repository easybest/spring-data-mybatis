package org.springframework.data.mybatis.dialect;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Dialect Factory Bean.
 * 
 * @author Jarvis Song
 */
@Slf4j
public class DialectFactoryBean implements FactoryBean<Dialect>, InitializingBean {

	public static final int NO_VERSION = -9999;

	private Dialect dialect;
	private final SqlSessionTemplate sqlSessionTemplate;

	public DialectFactoryBean(SqlSessionTemplate sqlSessionTemplate) {
		this.sqlSessionTemplate = sqlSessionTemplate;
	}

	@Override
	public Dialect getObject() throws Exception {
		if (null == dialect) {
			afterPropertiesSet();
		}
		return dialect;
	}

	@Override
	public Class<?> getObjectType() {
		return null == dialect ? Dialect.class : dialect.getClass();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(sqlSessionTemplate != null, "SqlSessionTemplate must not be null!");
		if (null != this.dialect) {
			return;
		}

		DataSource dataSource = sqlSessionTemplate.getConfiguration().getEnvironment().getDataSource();
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			DatabaseMetaData metaData = conn.getMetaData();
			this.dialect = getDialect(metaData);
			log.info("Using dialect: " + this.dialect);
		} finally {
			if (null != conn) {
				conn.close();
			}
		}

	}

	private Dialect getDialect(DatabaseMetaData metaData) throws SQLException {
		final String databaseName = metaData.getDatabaseProductName();
		final int majorVersion = interpretVersion(metaData.getDatabaseMajorVersion());
		final int minorVersion = interpretVersion(metaData.getDatabaseMinorVersion());

		if ("H2".equalsIgnoreCase(databaseName)) {
			return new H2Dialect();
		}
		if ("MySQL".equalsIgnoreCase(databaseName)) {
			return new MySQLDialect();
		}
		// if (databaseName.startsWith("Microsoft SQL Server")) {
		// return new SqlServer2012Dialect();
		// }
		// if ("Oracle".equalsIgnoreCase(databaseName)) {
		// return new OracleDialect();
		// }
		// if ("PostgreSQL".equalsIgnoreCase(databaseName)) {
		// return new PostgreSQLDialect();
		// }
		// return new AnsiSqlDialect();
		return null;
	}

	private static int interpretVersion(int result) {
		return result < 0 ? NO_VERSION : result;
	}

	public void setDialect(Dialect dialect) {
		this.dialect = dialect;
	}
}
