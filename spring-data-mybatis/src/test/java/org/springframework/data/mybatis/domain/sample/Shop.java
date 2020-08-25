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

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import org.springframework.data.mybatis.annotation.JdbcType;
import org.springframework.data.mybatis.domain.Audit;

/**
 * Shop, for test case use.
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
@Entity
@Table(name = "t_shop")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@NoArgsConstructor
public class Shop extends Audit<Long, Long> {

	@Version
	private Integer version;

	private String name;

	private String introduce;

	private String emailAddress;

	private Boolean active;

	private Integer duration;

	/**
	 * Date <=> Unix timestamp.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "openingTime")
	@JdbcType("BIGINT")
	private Date openingTime;

	/**
	 * Unix timestamp <=> datetime.
	 */
	@Column(name = "brand_time")
	@JdbcType("TIMESTAMP")
	private Long brandEstablishmentTime;

	@Embedded
	private Address address;

	@OrderBy("name asc")
	@OneToMany(fetch = FetchType.EAGER)
	@JoinColumn(referencedColumnName = "id", name = "shop_id")
	private List<Goods> goods = Collections.emptyList();

	public Shop(String name, String emailAddress) {
		this.name = name;
		this.emailAddress = emailAddress;
	}

}
