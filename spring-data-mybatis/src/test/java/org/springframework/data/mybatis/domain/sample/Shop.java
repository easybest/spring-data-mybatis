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

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

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

	private String name;

	private String introduce;

	private String emailAddress;

	private Boolean active;

	private Integer duration;

	/**
	 * Date <=> Unix timestamp.
	 */
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

	@ManyToMany
	@JoinTable(name = "t_shop_goods", joinColumns = @JoinColumn(name = "shop_id", referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "goods_id", referencedColumnName = "id"))
	private List<Goods> goods;

	public Shop(String name, String emailAddress) {
		this.name = name;
		this.emailAddress = emailAddress;
	}

}
