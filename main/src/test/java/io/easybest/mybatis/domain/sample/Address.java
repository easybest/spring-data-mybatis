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

package io.easybest.mybatis.domain.sample;

import jakarta.persistence.Embeddable;

/**
 * .
 *
 * @author Thomas Darimont
 */
@Embeddable
public class Address {

	private String country;

	private String city;

	private String streetName;

	private String streetNo;

	public Address() {
	}

	public Address(String country, String city, String streetName, String streetNo) {
		this.country = country;
		this.city = city;
		this.streetName = streetName;
		this.streetNo = streetNo;
	}

	public String getCountry() {
		return this.country;
	}

	public String getCity() {
		return this.city;
	}

	public String getStreetName() {
		return this.streetName;
	}

	public String getStreetNo() {
		return this.streetNo;
	}

}
