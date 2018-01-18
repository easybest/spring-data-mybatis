package org.springframework.data.mybatis.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Jarvis Song
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface Entity {

	String name() default "";

	/**
	 * mapping table's name.
	 * 
	 * @return table's name
	 */
	String table() default "";

	String schema() default "";

}
