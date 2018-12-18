package org.springframework.data.mybatis.id;

import java.sql.Statement;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;

public class SnowflakeKeyGenerator implements KeyGenerator {

	public static final String SELECT_KEY_SUFFIX = "!snowflake";

	private final String keyProperty;

	private final Snowflake snowflake;

	public SnowflakeKeyGenerator(String keyProperty, Snowflake snowflake) {
		this.keyProperty = keyProperty;
		this.snowflake = snowflake;
	}

	@Override
	public void processBefore(Executor executor, MappedStatement mappedStatement,
			Statement statement, Object parameter) {

		if (null == parameter) {
			return;
		}
		try {
			Configuration configuration = mappedStatement.getConfiguration();
			MetaObject metaParam = configuration.newMetaObject(parameter);
			long k = this.snowflake.nextId();
			MetaObject metaResult = configuration.newMetaObject(k);
			if (metaResult.hasGetter(this.keyProperty)) {
				this.setValue(metaParam, this.keyProperty,
						metaResult.getValue(this.keyProperty));
			}
			else {
				this.setValue(metaParam, this.keyProperty, k);
			}
		}
		catch (ExecutorException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new ExecutorException(
					"Error selecting key or setting result to parameter object. Cause: "
							+ ex.getMessage(),
					ex);
		}
	}

	@Override
	public void processAfter(Executor executor, MappedStatement mappedStatement,
			Statement statement, Object o) {

	}

	private void setValue(MetaObject metaParam, String property, Object value) {
		if (metaParam.hasSetter(property)) {
			metaParam.setValue(property, value);
		}
		else {
			throw new ExecutorException("No setter found for the keyProperty '" + property
					+ "' in " + metaParam.getOriginalObject().getClass().getName() + ".");
		}
	}

}
