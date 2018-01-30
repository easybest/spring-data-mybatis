package org.springframework.data.mybatis.repository.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Procedure {

	/**
	 * The name of the procedure in the database, defaults to {@code ""}. Short form for {@link #procedureName()}.
	 */
	String value() default "";

	/**
	 * The name of the procedure in the database, defaults to {@code ""}.
	 */
	String procedureName() default "";

	/**
	 * The name of the procedure in the EntityManager - defaults to {@code ""}.
	 */
	String name() default "";

	/**
	 * The name of the outputParameter, defaults to {@code ""}.
	 */
	String outputParameterName() default "";
}
