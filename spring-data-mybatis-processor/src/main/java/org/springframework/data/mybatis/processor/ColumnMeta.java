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
package org.springframework.data.mybatis.processor;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public class ColumnMeta {

	private String name;

	private String propertyName;

	private String upperPropertyName;

	private String type;

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPropertyName() {
		return this.propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
		if (null != propertyName && propertyName.length() > 0) {
			this.upperPropertyName = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
		}
	}

	public String getType() {
		if (null != this.type && this.type.startsWith("java.lang.")) {
			return this.type.substring(this.type.lastIndexOf(".") + 1);
		}
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUpperPropertyName() {
		return this.upperPropertyName;
	}

	public void setUpperPropertyName(String upperPropertyName) {
		this.upperPropertyName = upperPropertyName;
	}

}
