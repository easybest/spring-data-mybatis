/*
 * Copyright 2019-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.easybest.mybatis.repository.query.criteria;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.invoke.SerializedLambda;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

import org.springframework.util.ClassUtils;

/**
 * .
 *
 * @author Jarvis Song
 */
@Slf4j
public class LambdaUtils {

	private static final Map<Class<?>, WeakReference<String>> cache = new ConcurrentHashMap<>();

	@SuppressWarnings("rawtypes")
	public static <T, R> String getFieldName(FieldFunction<T, R> fun) {

		Class<? extends FieldFunction> funClass = fun.getClass();

		WeakReference<String> reference = cache.computeIfAbsent(funClass, clz -> {

			try {
				Method writeReplace = clz.getDeclaredMethod("writeReplace");
				writeReplace.setAccessible(true);
				Object invoked = writeReplace.invoke(fun);
				SerializedLambda serializedLambda = (SerializedLambda) invoked;
				String methodName = serializedLambda.getImplMethodName();

				String normalImplClass = serializedLambda.getImplClass().replace('/', '.');

				Class<?> implClass = ClassUtils.forName(normalImplClass, LambdaUtils.class.getClassLoader());

				Method method = implClass.getMethod(methodName);
				BeanInfo beanInfo = Introspector.getBeanInfo(implClass);
				PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
				Optional<PropertyDescriptor> pd = Arrays.stream(descriptors)
						.filter(d -> method.equals(d.getReadMethod()) || method.equals(d.getWriteMethod())).findFirst();
				return pd.map(propertyDescriptor -> new WeakReference<>(propertyDescriptor.getName())).orElse(null);
			}
			catch (Exception ex) {
				log.error(ex.getMessage(), ex);
				return null;
			}

		});

		if (null == reference) {
			return null;
		}

		return reference.get();

	}

	private static String readMethodToPropertyName(String methodName) {

		return Introspector.decapitalize(methodName.substring(methodName.startsWith("is") ? 2 : 3));
	}

}
