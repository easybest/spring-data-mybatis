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
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mybatis.domain.sample.Customer;
import org.springframework.data.mybatis.domain.sample.Name;
import org.springframework.data.mybatis.repository.sample.CustomerRepository;
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
public class CustomerRepositoryTest {

	@Autowired
	CustomerRepository repository;

	Customer first;

	Customer second;

	Customer third;

	Customer fourth;

	Name name;

	@Before
	public void setUp() {

		this.first = new Customer("Oliver", "Gierke").setAge(25);
		this.second = new Customer("Joachim", "Arrasz").setAge(32);
		this.third = new Customer("Dave", "Matthews").setAge(25);
		this.fourth = new Customer("kevin", "raymond").setAge(30);

	}

	protected void flushTestCustomers() {

		this.first = this.repository.saveSelective(this.first);
		this.second = this.repository.save(this.second);
		this.third = this.repository.save(this.third);
		this.fourth = this.repository.save(this.fourth);

		this.name = this.first.getName();

		assertThat(this.name).isNotNull();
		assertThat(this.second.getName()).isNotNull();
		assertThat(this.third.getName()).isNotNull();
		assertThat(this.fourth.getName()).isNotNull();

		assertThat(this.repository.existsById(this.name)).isTrue();
		assertThat(this.repository.existsById(this.second.getName())).isTrue();
		assertThat(this.repository.existsById(this.third.getName())).isTrue();
		assertThat(this.repository.existsById(this.fourth.getName())).isTrue();
	}

	@Test
	public void testCreation() {

		long before = this.repository.count();

		this.flushTestCustomers();

		assertThat(this.repository.count()).isEqualTo(before + 4L);
	}

	@Test
	public void testRead() {

		this.flushTestCustomers();

		assertThat(this.repository.findById(this.name)).map(Customer::getName).contains(this.first.getName());
	}

	@Test
	public void findsAllByGivenIds() {

		this.flushTestCustomers();

		assertThat(this.repository.findAllById(Arrays.asList(this.first.getName(), this.second.getName())))
				.contains(this.first, this.second);
	}

	@Test
	public void savingEmptyCollectionIsNoOp() {
		assertThat(this.repository.saveAll(new ArrayList<>())).isEmpty();
	}

	@Test
	public void deletesById() {

		this.flushTestCustomers();

		this.repository.deleteById(this.first.getName());

		assertThat(this.repository.existsById(this.name)).isFalse();
		assertThat(this.repository.findById(this.name)).isNotPresent();
	}

	@Test
	public void returnsAllSortedCorrectly() throws Exception {

		this.flushTestCustomers();
		List<Customer> all = this.repository.findAll(Sort.by(Direction.ASC, "name.lastname"));
		assertThat(all).hasSize(4).containsExactly(this.second, this.first, this.third, this.fourth);
	}

	@Test
	public void returnsAllIgnoreCaseSortedCorrectly() throws Exception {

		this.flushTestCustomers();

		Order order = new Order(Direction.ASC, "name.firstname").ignoreCase();

		assertThat(this.repository.findAll(Sort.by(order))) //
				.hasSize(4)//
				.containsExactly(this.third, this.second, this.fourth, this.first);
	}

	@Test
	public void deleteCollectionOfEntities() {

		this.flushTestCustomers();

		long before = this.repository.count();

		this.repository.deleteAll(Arrays.asList(this.first, this.second));

		assertThat(this.repository.existsById(this.first.getName())).isFalse();
		assertThat(this.repository.existsById(this.second.getName())).isFalse();
		assertThat(this.repository.count()).isEqualTo(before - 2);
	}

	@Test
	public void batchDeleteCollectionOfEntities() {

		this.flushTestCustomers();

		long before = this.repository.count();

		this.repository.deleteInBatch(Arrays.asList(this.first, this.second));

		assertThat(this.repository.existsById(this.first.getName())).isFalse();
		assertThat(this.repository.existsById(this.second.getName())).isFalse();
		assertThat(this.repository.count()).isEqualTo(before - 2);
	}

	@Test
	public void deleteEmptyCollectionDoesNotDeleteAnything() {

		this.assertDeleteCallDoesNotDeleteAnything(new ArrayList<>());
	}

	private void assertDeleteCallDoesNotDeleteAnything(List<Customer> collection) {

		this.flushTestCustomers();
		long count = this.repository.count();

		this.repository.deleteAll(collection);
		assertThat(this.repository.count()).isEqualTo(count);
	}

	@Test
	public void executesManualQueryWithPositionLikeExpressionCorrectly() {

		this.flushTestCustomers();

		List<Customer> result = this.repository.findByNameFirstnameLike("Da");

		assertThat(result).containsOnly(this.third);
	}

	@Test
	public void executesManualQueryWithNamedLikeExpressionCorrectly() {

		this.flushTestCustomers();

		List<Customer> result = this.repository.findByNameFirstnameLikeNamed("Da");

		assertThat(result).containsOnly(this.third);
	}

	@Test
	public void executesDerivedCountQueryToLong() {

		this.flushTestCustomers();

		assertThat(this.repository.countByNameLastname("Matthews")).isEqualTo(1L);
	}

	@Test
	public void executesDerivedCountQueryToInt() {

		this.flushTestCustomers();

		assertThat(this.repository.countCustomersByNameFirstname("Dave")).isEqualTo(1);
	}

	@Test
	public void executesDerivedExistsQuery() {

		this.flushTestCustomers();

		assertThat(this.repository.existsByNameLastname("Matthews")).isEqualTo(true);
		assertThat(this.repository.existsByNameLastname("Hans Peter")).isEqualTo(false);
	}

	@Test
	public void executesManuallyDefinedQueryWithFieldProjection() {

		this.flushTestCustomers();
		List<String> lastname = this.repository.findFirstnamesByLastname("Matthews");

		assertThat(lastname).containsOnly("Dave");
	}

	@Test
	public void executesFinderWithOrderClauseOnly() {

		this.flushTestCustomers();

		assertThat(this.repository.findAllByOrderByNameLastnameAsc()).containsOnly(this.second, this.first, this.third,
				this.fourth);
	}

}
