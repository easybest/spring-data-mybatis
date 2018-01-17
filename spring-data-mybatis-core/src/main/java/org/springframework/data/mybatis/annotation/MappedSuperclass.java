package org.springframework.data.mybatis.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

/**
 * Designates a class whose mapping information is applied to the entities that inherit from it. A mapped superclass has
 * no separate table defined for it.
 * 
 * @author Jarvis Song
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface MappedSuperclass {

}
