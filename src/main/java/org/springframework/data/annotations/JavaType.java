package org.springframework.data.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * JavaType.
 *
 * @author jarvis@caomeitu.com
 * @since 15/9/30
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface JavaType {
    /**
     * value.
     */
    Class<?> value();

}
