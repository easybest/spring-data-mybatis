package org.springframework.data.mybatis.dialect.function;

/**
 * @author Jarvis Song
 */
public interface SQLFunction {

	boolean hasArguments();

	boolean hasParenthesesIfNoArguments();

}
