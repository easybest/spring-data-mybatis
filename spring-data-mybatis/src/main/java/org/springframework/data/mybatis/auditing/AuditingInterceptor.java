package org.springframework.data.mybatis.auditing;

import static org.apache.ibatis.mapping.SqlCommandType.INSERT;
import static org.apache.ibatis.mapping.SqlCommandType.UPDATE;

import java.util.Properties;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;

@Intercepts({ @Signature(type = Executor.class, method = "update", args = {
		MappedStatement.class, Object.class }) })
public class AuditingInterceptor implements Interceptor {

	private final AuditingEntityListener listener;

	public AuditingInterceptor(AuditingEntityListener listener) {
		this.listener = listener;
	}

	@Override
	public Object intercept(Invocation invocation) throws Throwable {

		MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
		SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
		Object target = invocation.getArgs()[1];
		if (INSERT == sqlCommandType) {
			listener.touchForCreate(target);
		}
		else if (UPDATE == sqlCommandType) {
			listener.touchForUpdate(target);
		}
		return invocation.proceed();
	}

	@Override
	public Object plugin(Object target) {
		if (target instanceof Executor) {
			return Plugin.wrap(target, this);
		}
		return target;
	}

	@Override
	public void setProperties(Properties properties) {

	}

}
