package org.springframework.data.mybatis.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Array of generic generator definitions.
 *
 * @author Jarvis Song
 */
@Target({ PACKAGE, TYPE })
@Retention(RUNTIME)
public @interface GenericGenerators {

	/**
	 * The aggregated generators.
	 */
	GenericGenerator[] value();
}
