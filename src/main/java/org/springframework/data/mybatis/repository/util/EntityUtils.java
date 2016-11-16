package org.springframework.data.mybatis.repository.util;

import javax.persistence.Transient;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by songjiawei on 2016/11/13.
 */
public abstract class EntityUtils {

    public static List<Field> getAllFields(Class<?> clazz) {
        List<Field> result = new LinkedList<Field>();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {

			/* 过滤静态属性 */
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

			/* 过滤 transient关键字修饰的属性 */
            if (Modifier.isTransient(field.getModifiers())) {
                continue;
            }

			/* 过滤注解非表字段属性 */
            if (null != field.getAnnotation(Transient.class) || null != field.getAnnotation(org.springframework.data.annotation.Transient.class)) {
                continue;
            }

            result.add(field);

        }

		/* 处理父类字段 */
        Class<?> superClass = clazz.getSuperclass();
        if (superClass.equals(Object.class)) {
            return result;
        }
        result.addAll(getAllFields(superClass));
        return result;
    }

}
