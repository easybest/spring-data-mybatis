package org.springframework.data.mybatis.annotation;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Generator annotation describing any kind of Hibernate generator in a generic (de-typed) manner.
 *
 * @author Jarvis Song
 */
@Target({ PACKAGE, TYPE, METHOD, FIELD })
@Retention(RUNTIME)
@Repeatable(GenericGenerators.class)
public @interface GenericGenerator {

	/**
	 * unique generator name.
	 */
	String name();

	/**
	 * Generator strategy either a predefined Hibernate strategy or a fully qualified class name.
	 */
	String strategy();

	/**
	 * Optional generator parameters.
	 */
	Parameter[] parameters() default {};
}
