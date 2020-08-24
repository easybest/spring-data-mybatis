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

import java.io.Serializable;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Version;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * .
 *
 * @author JARVIS SONG
 */
@Entity
@Table(name = "t_customer")
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class Customer implements Serializable {

	@EmbeddedId
	private Name name;

	private String emailAddress;

	private Integer age;

	@Enumerated(EnumType.ORDINAL)
	private Gender gender;

	@Enumerated(EnumType.STRING)
	private Constellation constellation;

	@Lob
	private byte[] binaryData;

	@Version
	private Long version;

	public Customer(String firstname, String lastname) {
		this.name = new Name(firstname, lastname);
	}

	public Customer(String firstname, String lastname, String emailAddress) {
		this.name = new Name(firstname, lastname);
		this.emailAddress = emailAddress;
	}

	public enum Gender {

		MALE, FEMALE

	}

	public enum Constellation {

		Aries, Taurus, Gemini, Cancer, Leo, Virgo, Libra, Scorpio, Sagittarius, Capricorn, Aquarius, Pisces

	}

}
