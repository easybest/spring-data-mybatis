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
package org.springframework.data.mybatis.repository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mybatis.domain.sample.Category;
import org.springframework.data.mybatis.domain.sample.Goods;
import org.springframework.data.mybatis.repository.sample.CategoryRepository;
import org.springframework.data.mybatis.repository.sample.GoodsRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * .
 *
 * @author JARVIS SONG
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context.xml")
@Transactional
public class GoodsRepositoryTest {

	@Autowired
	GoodsRepository repository;

	@Autowired
	CategoryRepository categoryRepository;

	Category food;

	Category drinks;

	Goods first;

	Goods second;

	Goods third;

	Goods fourth;

	@Before
	public void setUp() {
		this.food = new Category("food");
		this.drinks = new Category("drinks");

		this.first = new Goods("biscuits").setBrand("Oreo").setInventory(980).setCategory(this.food);
		this.second = new Goods("chips").setBrand("Lay's").setInventory(301).setCategory(this.food);
		this.third = new Goods("coke").setBrand("Coca-Cola").setInventory(890).setCategory(this.drinks);
		this.fourth = new Goods("soda water").setBrand("Watson").setInventory(120).setCategory(this.drinks);
	}

	protected void flushTestGoods() {
		this.food = this.categoryRepository.save(this.food);
		this.drinks = this.categoryRepository.save(this.drinks);

		assertThat(this.food.getId()).isNotNull();
		assertThat(this.drinks.getId()).isNotNull();

		this.first = this.repository.save(this.first);
		this.second = this.repository.save(this.second);
		this.third = this.repository.save(this.third);
		this.fourth = this.repository.saveSelective(this.fourth);
	}

	@Test
	public void testReadAll() {
		this.flushTestGoods();
		assertThat(this.repository.count()).isEqualTo(4L);
		assertThat(this.repository.findAll()).contains(this.first, this.second, this.third, this.fourth);

		assertThat(this.first.getCategory()).isNotNull();
		assertThat(this.first.getCategory()).isEqualTo(this.food);

	}

}
