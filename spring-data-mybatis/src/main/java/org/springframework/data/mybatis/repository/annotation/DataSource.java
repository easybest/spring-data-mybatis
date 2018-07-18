package org.springframework.data.mybatis.repository.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to declare which datasource to use.
 * 
 * @author Jarvis Song
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Documented
public @interface DataSource {
	/**
	 * determine which datasource to use.
	 */
	String value() default "";

}
