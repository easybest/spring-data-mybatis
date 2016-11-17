/*
 *
 *   Copyright 2016 the original author or authors.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.springframework.data.mybatis.repository.util;

import javax.persistence.Transient;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

/**
 * entity utils.
 *
 * @author Jarvis Song
 */
public abstract class EntityUtils {

    public static List<Field> getAllFields(Class<?> clazz) {
        List<Field> result = new LinkedList<Field>();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {

			/* filter static property */
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

			/* filter transient property */
            if (Modifier.isTransient(field.getModifiers())) {
                continue;
            }

			/* filter transient property */
            if (null != field.getAnnotation(Transient.class) || null != field.getAnnotation(org.springframework.data.annotation.Transient.class)) {
                continue;
            }

            result.add(field);

        }

		/* process super class */
        Class<?> superClass = clazz.getSuperclass();
        if (superClass.equals(Object.class)) {
            return result;
        }
        result.addAll(getAllFields(superClass));
        return result;
    }

}
