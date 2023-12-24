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

package io.easybest.mybatis.mapping.precompile;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;

/**
 * .
 *
 * @author Jarvis Song
 */
@Getter
public class MethodInvocation implements Segment {

	private Class<?> staticClass;

	private String methodName;

	private List<String> parameters;

	public static MethodInvocation of(Class<?> staticClass, String methodName, String... parameters) {

		return MethodInvocation.builder().staticClass(staticClass).methodName(methodName)
				.parameters(Arrays.asList(parameters)).build();
	}

	@Override
	public String toString() {
		return "@" + this.staticClass.getName() + "@" + this.methodName + "("
				+ (null == this.parameters ? "" : String.join(",", this.parameters)) + ")";
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private Class<?> staticClass;

		private String methodName;

		private List<String> parameters;

		public MethodInvocation build() {

			MethodInvocation instance = new MethodInvocation();
			instance.staticClass = this.staticClass;
			instance.methodName = this.methodName;
			instance.parameters = this.parameters;

			return instance;
		}

		public Builder staticClass(final Class<?> staticClass) {
			this.staticClass = staticClass;
			return this;
		}

		public Builder methodName(final String methodName) {
			this.methodName = methodName;
			return this;
		}

		public Builder parameters(final List<String> parameters) {
			this.parameters = parameters;
			return this;
		}

	}

}
