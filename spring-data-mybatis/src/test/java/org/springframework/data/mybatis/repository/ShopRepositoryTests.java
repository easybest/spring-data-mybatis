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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.StringMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mybatis.domain.sample.Address;
import org.springframework.data.mybatis.domain.sample.QShop;
import org.springframework.data.mybatis.domain.sample.Shop;
import org.springframework.data.mybatis.repository.sample.ShopRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base integration test class for {@code ShopRepository}.
 *
 * @author JARVIS SONG
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context.xml")
@Transactional
public class ShopRepositoryTests {

	@Autowired
	ShopRepository repository;

	Shop first;

	Shop second;

	Shop third;

	Shop fourth;

	Long id;

	@Before
	public void setUp() {

		this.first = new Shop("Walmart", "shop@walmart.com");
		this.first.setActive(true);
		this.first.setDuration(9);
		this.first.setIntroduce("I am the 300th shop of Walmart.");
		this.first.setAddress(new Address("USA", "NY", "Queen", "351"));
		this.first.setBrandEstablishmentTime(new Calendar.Builder().setDate(1962, 1, 1).build().getTime().getTime());
		this.first.setOpeningTime(new Calendar.Builder().setDate(2010, 10, 1).build().getTime());

		this.second = new Shop("Costco", "costco@gmail.com");
		this.second.setActive(false);
		this.second.setDuration(9);
		this.second.setIntroduce("I am the 20th shop of Costco.");
		this.second.setAddress(new Address("USA", "WA", "Issaquah", "908"));
		this.second.setBrandEstablishmentTime(new Calendar.Builder().setDate(1976, 1, 1).build().getTime().getTime());
		this.second.setOpeningTime(new Calendar.Builder().setDate(2009, 5, 15).build().getTime());

		this.third = new Shop("Carrefour", "carrefour@gmail.com");
		this.third.setActive(true);
		this.third.setDuration(12);
		this.third.setAddress(new Address("FR", "Boulogne", "Golden", "18"));
		this.third.setBrandEstablishmentTime(new Calendar.Builder().setDate(1959, 1, 1).build().getTime().getTime());
		this.third.setOpeningTime(new Calendar.Builder().setDate(2011, 6, 25).build().getTime());

		this.fourth = new Shop("Auchan", "shop@auchan.com");
		this.fourth.setActive(true);
		this.fourth.setDuration(11);
		this.fourth.setAddress(new Address("FR", "London", "Oushang", "93"));
		this.fourth.setBrandEstablishmentTime(new Calendar.Builder().setDate(1961, 1, 1).build().getTime().getTime());
		this.fourth.setOpeningTime(new Calendar.Builder().setDate(2010, 3, 15).build().getTime());
	}

	protected void flushTestShops() {

		this.first = this.repository.saveSelective(this.first);
		this.second = this.repository.save(this.second);
		// this.third = this.repository.save(this.third);
		// this.fourth = this.repository.save(this.fourth);

		this.repository.saveAll(Arrays.asList(this.third, this.fourth));

		this.id = this.first.getId();

		assertThat(this.id).isNotNull();
		assertThat(this.second.getId()).isNotNull();
		assertThat(this.third.getId()).isNotNull();
		assertThat(this.fourth.getId()).isNotNull();

		assertThat(this.repository.existsById(this.id)).isTrue();
		assertThat(this.repository.existsById(this.second.getId())).isTrue();
		assertThat(this.repository.existsById(this.third.getId())).isTrue();
		assertThat(this.repository.existsById(this.fourth.getId())).isTrue();
	}

	@Test
	public void testUpdate() {

		this.flushTestShops();

		Shop foundShop = this.repository.findById(this.id).get();
		foundShop.setName("Walmart X");
		this.repository.update(foundShop);

		assertThat(this.repository.findById(this.id)).map(Shop::getName).contains(foundShop.getName());
	}

	@Test
	public void findAll() {
		this.flushTestShops();
		List<Shop> shops = this.repository.findAll();
		System.out.println(shops);
		assertThat(shops).hasSize(4).contains(this.first, this.second, this.third, this.fourth);
	}

	@Test
	public void findAllSort() {
		this.flushTestShops();
		List<Shop> shops = this.repository.findAll(Sort.by("openingTime").descending());
		assertThat(shops).hasSize(4).containsExactly(this.third, this.first, this.fourth, this.second);
		shops = this.repository.findAll(Sort.by("brandEstablishmentTime").ascending());
		assertThat(shops).hasSize(4).containsExactly(this.third, this.fourth, this.first, this.second);
	}

	@Test
	public void findAllByIds() {
		this.flushTestShops();
		List<Shop> shops = this.repository.findAllById(Arrays.asList(this.second.getId(), this.third.getId()));
		assertThat(shops).hasSize(2).contains(this.second, this.third);
	}

	@Test
	public void findAllByPage() {
		this.flushTestShops();
		Page<Shop> page = this.repository.findAll(PageRequest.of(0, 3, Sort.by("openingTime").descending()));
		assertThat(page.getSize()).isEqualTo(3);
		assertThat(page.getTotalElements()).isEqualTo(4);
		assertThat(page.getTotalPages()).isEqualTo(2);
		assertThat(page.hasNext()).isEqualTo(true);
	}

	@Test
	public void count() {
		this.flushTestShops();
		assertThat(this.repository.count()).isEqualTo(4);
	}

	@Test
	public void deleteById() {
		this.flushTestShops();
		assertThat(this.repository.count()).isEqualTo(4);
		this.repository.deleteById(this.third.getId());
		assertThat(this.repository.count()).isEqualTo(3);
	}

	@Test
	public void delete() {
		this.flushTestShops();
		assertThat(this.repository.count()).isEqualTo(4);
		this.repository.delete(this.third);
		assertThat(this.repository.count()).isEqualTo(3);
	}

	@Test
	public void deleteAllIterator() {
		this.flushTestShops();
		assertThat(this.repository.count()).isEqualTo(4);
		this.repository.deleteAll(Arrays.asList(this.third, this.fourth));
		assertThat(this.repository.count()).isEqualTo(2);
	}

	@Test
	public void deleteAll() {
		this.flushTestShops();
		assertThat(this.repository.count()).isEqualTo(4);
		this.repository.deleteAll();
		assertThat(this.repository.count()).isEqualTo(0);
	}

	@Test
	public void removeById() {
		this.flushTestShops();
		assertThat(this.repository.count()).isEqualTo(4);
		assertThat(this.repository.removeById(this.third.getId())).isEqualTo(1);
		assertThat(this.repository.count()).isEqualTo(3);
	}

	@Test
	public void removeAllIterator() {
		this.flushTestShops();
		assertThat(this.repository.count()).isEqualTo(4);
		assertThat(this.repository.removeAll(Arrays.asList(this.third, this.fourth))).isEqualTo(2);
		assertThat(this.repository.count()).isEqualTo(2);
	}

	@Test
	public void removeAll() {
		this.flushTestShops();
		assertThat(this.repository.count()).isEqualTo(4);
		assertThat(this.repository.removeAll()).isEqualTo(4);
		assertThat(this.repository.count()).isEqualTo(0);
	}

	@Test
	public void savingEmptyCollectionIsNoOp() {
		Assertions.assertThat(this.repository.saveAll(new ArrayList<>())).isEmpty();
	}

	@Test
	public void findAllByExample() {
		this.flushTestShops();
		Shop prototype = new Shop();
		prototype.setDuration(9);
		List<Shop> shops = this.repository.findAll(Example.of(prototype));
		assertThat(shops).hasSize(2).contains(this.first, this.second);
	}

	@Test
	public void findAllByExampleWithEmptyProbe() {

		this.flushTestShops();

		Shop prototype = new Shop();
		prototype.setName("Costco");

		List<Shop> shops = this.repository.findAll(
				Example.of(prototype, ExampleMatcher.matching().withIgnorePaths("name", "introduce", "emailAddress")));

		Assertions.assertThat(shops).hasSize(4);
	}

	@Test
	public void findAllByExampleWithExcludedAttributes() {

		this.flushTestShops();

		Shop prototype = new Shop();
		prototype.setDuration(9);
		prototype.setActive(true);
		Example<Shop> example = Example.of(prototype, ExampleMatcher.matching().withIgnorePaths("active"));
		List<Shop> shops = this.repository.findAll(example);

		Assertions.assertThat(shops).containsOnly(this.first, this.second);
	}

	@Test(expected = IllegalArgumentException.class)
	public void findAllByNullExample() {
		this.repository.findAll((Example<Shop>) null);
	}

	@Test
	public void findAllByExampleWithEmbedded() {
		this.flushTestShops();
		Shop prototype = new Shop();
		prototype.setAddress(new Address("USA", null, null, null));
		List<Shop> shops = this.repository.findAll(Example.of(prototype));
		assertThat(shops).hasSize(2);
	}

	@Test
	public void findAllByExampleWithSort() {
		this.flushTestShops();
		Shop prototype = new Shop();
		prototype.setDuration(9);
		List<Shop> shops = this.repository.findAll(Example.of(prototype),
				Sort.by("brandEstablishmentTime").descending());
		assertThat(shops).hasSize(2).containsExactly(this.second, this.first);
		shops = this.repository.findAll(Example.of(prototype), Sort.by("address.city").descending());
		assertThat(shops).hasSize(2).containsExactly(this.second, this.first);
	}

	@Test
	public void findAllByExampleWithSpecifyMatcher() {
		this.flushTestShops();

		Shop prototype = new Shop();
		prototype.setName("c");
		prototype.setDuration(9);
		prototype.setActive(false);

		Example<Shop> example = Example.of(prototype, ExampleMatcher.matching().withIgnorePaths("active")
				.withMatcher("name", matcher -> matcher.startsWith().ignoreCase()));
		List<Shop> shops = this.repository.findAll(example);

		Assertions.assertThat(shops).containsOnly(this.second);
	}

	@Test
	public void findAllByExampleWithStartingStringMatcher() {

		this.flushTestShops();

		Shop prototype = new Shop();
		prototype.setName("Au");

		Example<Shop> example = Example.of(prototype,
				ExampleMatcher.matching().withStringMatcher(StringMatcher.STARTING));
		List<Shop> shops = this.repository.findAll(example);

		Assertions.assertThat(shops).containsOnly(this.fourth);
	}

	@Test
	public void findAllByExampleWithEndingStringMatcher() {

		this.flushTestShops();

		Shop prototype = new Shop();
		prototype.setName("co");

		Example<Shop> example = Example.of(prototype,
				ExampleMatcher.matching().withStringMatcher(StringMatcher.ENDING));
		List<Shop> shops = this.repository.findAll(example);

		Assertions.assertThat(shops).containsOnly(this.second);
	}

	public void findAllByExampleWithRegexStringMatcher() {

		this.flushTestShops();

		Shop prototype = new Shop();
		prototype.setName("^Cos");

		Example<Shop> example = Example.of(prototype, ExampleMatcher.matching().withStringMatcher(StringMatcher.REGEX));
		assertThat(this.repository.findAll(example)).hasSize(1).containsExactly(this.second);
	}

	@Test
	public void findAllByExampleWithIgnoreCase() {

		this.flushTestShops();

		Shop prototype = new Shop();
		prototype.setName("cOStCo");

		Example<Shop> example = Example.of(prototype, ExampleMatcher.matching().withIgnoreCase());

		List<Shop> shops = this.repository.findAll(example);

		Assertions.assertThat(shops).containsOnly(this.second);
	}

	@Test
	public void findAllByExampleWithStringMatcherAndIgnoreCase() {

		this.flushTestShops();

		Shop prototype = new Shop();
		prototype.setName("cOS");

		Example<Shop> example = Example.of(prototype,
				ExampleMatcher.matching().withStringMatcher(StringMatcher.STARTING).withIgnoreCase());

		List<Shop> shops = this.repository.findAll(example);

		Assertions.assertThat(shops).containsOnly(this.second);
	}

	@Test
	public void getById() {
		this.flushTestShops();
		Shop shop = this.repository.getById(this.second.getId());
		assertThat(shop).isNotNull().isEqualTo(this.second);
	}

	@Test
	public void saveAll() {
		this.repository.save(this.third);
		assertThat(this.third.getIntroduce()).isNull();
		this.third.setIntroduce("I am the 209th shop of Carrefour.");
		assertThat(this.repository.saveAll(Arrays.asList(this.first, this.third, this.fourth))).hasSize(3)
				.contains(this.first, this.third, this.fourth).allMatch(shop -> null != shop.getId());
		assertThat(this.third.getIntroduce()).isNotNull();
	}

	@Test
	public void saveSelectiveAll() {
		this.repository.save(this.third);
		assertThat(this.third.getIntroduce()).isNull();
		this.third.setIntroduce("I am the 209th shop of Carrefour.");
		assertThat(this.repository.saveSelectiveAll(Arrays.asList(this.first, this.third, this.fourth))).hasSize(3)
				.contains(this.first, this.third, this.fourth).allMatch(shop -> null != shop.getId());
		assertThat(this.third.getIntroduce()).isNotNull();
	}

	@Test
	public void executesManualQueryWithPositionLikeExpressionCorrectly() {

		this.flushTestShops();

		List<Shop> result = this.repository.findByNameLike("Cos");

		assertThat(result).containsOnly(this.second);
	}

	@Test
	public void shouldSupportModifyingQueryWithVarArgs() {

		this.flushTestShops();

		this.repository.updateActiveState(false, this.first.getId(), this.second.getId(), this.third.getId(),
				this.fourth.getId());

		long expectedCount = this.repository.count();
		assertThat(this.repository.findByActiveFalse().size()).isEqualTo((int) expectedCount);
		assertThat(this.repository.findByActiveTrue().size()).isEqualTo(0);
	}

	@Test
	public void sortByEmbeddedProperty() {

		this.third.setAddress(new Address("Germany", "Saarbrücken", "HaveItYourWay", "123"));
		this.flushTestShops();

		Page<Shop> page = this.repository
				.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "address.streetName")));

		assertThat(page.getContent()).hasSize(4);
		assertThat(page.getContent().get(3)).isEqualTo(this.first);
	}

	@Test
	public void invokesQueryWithVarargsParametersCorrectly() {

		this.flushTestShops();
		Collection<Shop> result = this.repository.findByIdIn(this.first.getId(), this.second.getId());
		assertThat(result).containsOnly(this.first, this.second);
	}

	@Test
	public void invokesQueryCustomCorrectly() {

		this.flushTestShops();
		List<Shop> result = this.repository.findByCustom(this.first.getId(), this.second.getId());
		assertThat(result).containsOnly(this.first, this.second);
	}

	@Test
	public void customFindByQueryWithPositionalVarargsParameters() {

		this.flushTestShops();

		Collection<Shop> result = this.repository.findByIdsCustomWithPositionalVarArgs(this.first.getId(),
				this.second.getId());

		assertThat(result).containsOnly(this.first, this.second);
	}

	@Test
	public void customFindByQueryWithNamedVarargsParameters() {

		this.flushTestShops();

		Collection<Shop> result = this.repository.findByIdsCustomWithNamedVarArgs(this.first.getId(),
				this.second.getId());

		assertThat(result).containsOnly(this.first, this.second);
	}

	@Test
	public void testQuerydslFind() {
		this.flushTestShops();

		Iterable<Shop> shops = this.repository.findAll(QShop.shop.name.contains("re"));
		assertThat(shops).hasSize(1).containsOnly(this.third);

	}

}
