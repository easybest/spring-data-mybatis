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
package org.springframework.data.mybatis.repository.query;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.samskivert.mustache.BasicCollector;
import com.samskivert.mustache.Mustache;

public class DefaultCollector extends BasicCollector {

	private final boolean _allowAccessCoercion;

	public DefaultCollector() {
		this(true);
	}

	public DefaultCollector(boolean allowAccessCoercion) {
		this._allowAccessCoercion = allowAccessCoercion;
	}

	@Override
	public Mustache.VariableFetcher createFetcher(Object ctx, String name) {
		Mustache.VariableFetcher fetcher = super.createFetcher(ctx, name);
		if (fetcher != null) {
			return fetcher;
		}

		// first check for a getter which provides the value
		Class<?> cclass = ctx.getClass();

		if (Map.Entry.class.isAssignableFrom(cclass)) {
			if ("key".equals(name)) {
				return ((ctx1, name1) -> ((Map.Entry) ctx1).getKey());
			}
			if ("value".equals(name)) {
				return ((ctx1, name1) -> ((Map.Entry) ctx1).getValue());
			}
		}

		final Method m = this.getMethod(cclass, name);
		if (m != null) {
			return (ctx1, name1) -> m.invoke(ctx1);
		}

		// next check for a getter which provides the value
		final Field f = this.getField(cclass, name);
		if (f != null) {
			return (ctx12, name12) -> f.get(ctx12);
		}

		// finally check for a default interface method which provides the value (this is
		// left to
		// last because it's much more expensive and hopefully something already matched
		// above)
		final Method im = this.getIfaceMethod(cclass, name);
		if (im != null) {
			return (ctx13, name13) -> im.invoke(ctx13);
		}

		return null;
	}

	@Override
	public <K, V> Map<K, V> createFetcherCache() {
		return new ConcurrentHashMap<>();
	}

	protected Method getMethod(Class<?> clazz, String name) {
		if (this._allowAccessCoercion) {
			// first check up the superclass chain
			for (Class<?> cc = clazz; cc != null && cc != Object.class; cc = cc.getSuperclass()) {
				Method m = this.getMethodOn(cc, name);
				if (m != null) {
					return m;
				}
			}
		}
		else {
			// if we only allow access to accessible methods, then we can just let the JVM
			// handle
			// searching superclasses for the method
			try {
				return clazz.getMethod(name);
			}
			catch (Exception ex) {
				// fall through
			}
		}
		return null;
	}

	protected Method getIfaceMethod(Class<?> clazz, String name) {
		// enumerate the transitive closure of all interfaces implemented by clazz
		Set<Class<?>> ifaces = new LinkedHashSet<>();
		for (Class<?> cc = clazz; cc != null && cc != Object.class; cc = cc.getSuperclass()) {
			this.addIfaces(ifaces, cc, false);
		}
		// now search those in the order that we found them
		for (Class<?> iface : ifaces) {
			Method m = this.getMethodOn(iface, name);
			if (m != null) {
				return m;
			}
		}
		return null;
	}

	private void addIfaces(Set<Class<?>> ifaces, Class<?> clazz, boolean isIface) {
		if (isIface) {
			ifaces.add(clazz);
		}
		for (Class<?> iface : clazz.getInterfaces()) {
			this.addIfaces(ifaces, iface, true);
		}
	}

	protected Method getMethodOn(Class<?> clazz, String name) {
		Method m;
		try {
			m = clazz.getDeclaredMethod(name);
			if (!m.getReturnType().equals(void.class)) {
				return this.makeAccessible(m);
			}
		}
		catch (Exception ex) {
			// fall through
		}

		String upperName = Character.toUpperCase(name.charAt(0)) + name.substring(1);
		try {
			m = clazz.getDeclaredMethod("get" + upperName);
			if (!m.getReturnType().equals(void.class)) {
				return this.makeAccessible(m);
			}
		}
		catch (Exception ex) {
			// fall through
		}

		try {
			m = clazz.getDeclaredMethod("is" + upperName);
			if (m.getReturnType().equals(boolean.class) || m.getReturnType().equals(Boolean.class)) {
				return this.makeAccessible(m);
			}
		}
		catch (Exception ex) {
			// fall through
		}

		return null;
	}

	private Method makeAccessible(Method m) {
		if (m.isAccessible()) {
			return m;
		}
		else if (!this._allowAccessCoercion) {
			return null;
		}
		m.setAccessible(true);
		return m;
	}

	protected Field getField(Class<?> clazz, String name) {
		if (!this._allowAccessCoercion) {
			try {
				return clazz.getField(name);
			}
			catch (Exception ex) {
				return null;
			}
		}

		Field f;
		try {
			f = clazz.getDeclaredField(name);
			if (!f.isAccessible()) {
				f.setAccessible(true);
			}
			return f;
		}
		catch (Exception ex) {
			// fall through
		}

		Class<?> sclass = clazz.getSuperclass();
		if (sclass != Object.class && sclass != null) {
			return this.getField(clazz.getSuperclass(), name);
		}
		return null;
	}

}
