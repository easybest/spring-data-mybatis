package org.springframework.data.mybatis.dialect.function;

/**
 * @author Jarvis Song
 */
public interface SQLFunction {

	/**
	 * Does this function have any arguments?
	 *
	 * @return True if the function expects to have parameters; false otherwise.
	 */
	public boolean hasArguments();

	/**
	 * If there are no arguments, are parentheses required?
	 *
	 * @return True if a no-arg call of this function requires parentheses.
	 */
	public boolean hasParenthesesIfNoArguments();

}
