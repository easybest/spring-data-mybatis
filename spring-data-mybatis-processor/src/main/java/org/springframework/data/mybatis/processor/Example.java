package org.springframework.data.mybatis.processor;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Example {

    /**
     * mapper namespace
     *
     * @return
     */
    String value();

}
