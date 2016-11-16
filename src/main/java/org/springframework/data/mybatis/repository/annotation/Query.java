package org.springframework.data.mybatis.repository.annotation;

import org.springframework.data.annotation.QueryAnnotation;

import java.lang.annotation.*;

/**
 * Created by songjiawei on 2016/11/10.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@QueryAnnotation
@Documented
public @interface Query {

    String value() default "";

    String namespace() default "";

    String name() default "";

    Class<?> returnType() default Unspecified.class;

    Class<?> parameterType() default Unspecified.class;

    boolean basic() default true;

    class Unspecified {
    }

}


