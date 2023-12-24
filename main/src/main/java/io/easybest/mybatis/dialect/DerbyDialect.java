/*
 * Copyright 2019-2023 the original author or authors.
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

package io.easybest.mybatis.dialect;

import java.lang.reflect.Method;

import jakarta.persistence.GenerationType;

import org.springframework.data.mapping.MappingException;
import org.springframework.util.ClassUtils;

/**
 * .
 *
 * @author Jarvis Song
 */
public class DerbyDialect extends SQLServer2012Dialect {

	private int driverVersionMajor;

	private int driverVersionMinor;

	private final PaginationHandler limitHandler;

	public DerbyDialect() {

		super();

		this.determineDriverVersion();
		this.limitHandler = new DerbyLimitHandler();
	}

	@Override
	public PaginationHandler getPaginationHandler() {

		return this.limitHandler;
	}

	private void determineDriverVersion() {

		try {
			final Class<?> clz = ClassUtils.forName("org.apache.derby.tools.sysinfo", this.getClass().getClassLoader());
			final Method majorVersionGetter = clz.getMethod("getMajorVersion");
			final Method minorVersionGetter = clz.getMethod("getMinorVersion");
			this.driverVersionMajor = (Integer) majorVersionGetter.invoke(null, new Object[0]);
			this.driverVersionMinor = (Integer) minorVersionGetter.invoke(null, new Object[0]);
		}
		catch (Exception ex) {
			this.driverVersionMajor = -1;
			this.driverVersionMinor = -1;
		}
	}

	public boolean supportsSequences() {
		return this.driverVersionMajor > 10 || (this.driverVersionMajor == 10 && this.driverVersionMinor >= 6);
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		if (this.supportsSequences()) {
			return "values next value for " + sequenceName;
		}
		else {
			throw new MappingException(this.getClass().getName() + " does not support sequences");
		}
	}

	@Override
	public String getNativeIdentifierGeneratorStrategy() {
		return this.supportsSequences() ? GenerationType.SEQUENCE.name().toLowerCase()
				: GenerationType.IDENTITY.name().toLowerCase();
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type) {
		return "values identity_val_local()";
	}

	private static final class DerbyLimitHandler extends AbstractPaginationHandler {

	}

}
