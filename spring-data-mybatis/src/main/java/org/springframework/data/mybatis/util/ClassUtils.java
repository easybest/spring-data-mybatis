/*
 * Copyright 2012-2019 the original author or authors.
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
package org.springframework.data.mybatis.util;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public final class ClassUtils {

	/**
	 * NO param signature.
	 */
	public static final Class[] NO_PARAM_SIGNATURE = new Class[0];

	/**
	 * No params.
	 */
	public static final Object[] NO_PARAMS = new Object[0];

	private ClassUtils() {
	}

	public static Class forName(String name, Class caller) throws ClassNotFoundException {
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			if (classLoader != null) {
				return classLoader.loadClass(name);
			}
		}
		catch (Throwable ignore) {
		}
		return Class.forName(name, true, caller.getClassLoader());
	}

}
