package org.springframework.data.mybatis.repository.annotation;

import org.springframework.data.annotation.QueryAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to declare finder queries directly on repository methods.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@QueryAnnotation
@Documented
public @interface Query {
	/**
	 * Defines the MyBatis SQL query to be executed when the annotated method is called.
	 */
	String value() default "";

	String countQuery() default "";

	String namespace() default "";

	String statement() default "";

	String countStatement() default "";

	boolean withAssociations() default true;
}
