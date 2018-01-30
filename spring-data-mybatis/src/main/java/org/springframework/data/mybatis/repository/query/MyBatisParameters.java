package org.springframework.data.mybatis.repository.query;

import org.springframework.core.MethodParameter;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author Jarvis Song
 */
public class MyBatisParameters extends Parameters<MyBatisParameters, MyBatisParameters.MyBatisParameter> {

	public MyBatisParameters(Method method) {
		super(method);
	}

	protected MyBatisParameters(List<MyBatisParameter> originals) {
		super(originals);
	}

	@Override
	protected MyBatisParameter createParameter(MethodParameter parameter) {
		return new MyBatisParameter(parameter);
	}

	@Override
	protected MyBatisParameters createFrom(List<MyBatisParameter> parameters) {
		return new MyBatisParameters(parameters);
	}

	public static class MyBatisParameter extends Parameter {

		/**
		 * Creates a new {@link Parameter} for the given {@link MethodParameter}.
		 *
		 * @param parameter must not be {@literal null}.
		 */
		protected MyBatisParameter(MethodParameter parameter) {
			super(parameter);
		}
	}

}
