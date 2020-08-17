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
package org.springframework.data.mybatis.domain.sample;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import lombok.Data;
import lombok.experimental.Accessors;

@Embeddable
@Data
@Accessors(chain = true)
public class Address {

	private String country;

	private String city;

	private String streetName;

	@Column(name = "street_number")
	private String streetNo;

	public Address() {
	}

	public Address(String country, String city, String streetName, String streetNo) {
		this.country = country;
		this.city = city;
		this.streetName = streetName;
		this.streetNo = streetNo;
	}

}
