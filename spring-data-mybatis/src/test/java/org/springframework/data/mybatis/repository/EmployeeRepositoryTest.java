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

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mybatis.domain.sample.Employee;
import org.springframework.data.mybatis.domain.sample.Employee.Constellation;
import org.springframework.data.mybatis.domain.sample.Employee.Gender;
import org.springframework.data.mybatis.domain.sample.Name;
import org.springframework.data.mybatis.repository.sample.EmployeeRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * .
 *
 * @author JARVIS SONG
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:config/namespace-application-context.xml")
@Transactional
public class EmployeeRepositoryTest {

	@Autowired
	EmployeeRepository repository;

	Employee first;

	Employee second;

	Employee third;

	Employee fourth;

	Name name;

	@Before
	public void setUp() {

		this.first = new Employee("Oliver", "Gierke");
		this.first.setAge(28);
		this.first.setGender(Gender.MALE);
		this.first.setConstellation(Constellation.Aquarius);
		this.first.setEmailAddress("gierke@synyx.de");
		this.second = new Employee("Joachim", "Arrasz");
		this.second.setAge(35);
		this.second.setGender(Gender.FEMALE);
		this.second.setConstellation(Constellation.Cancer);
		this.second.setEmailAddress("arrasz@synyx.de");
		this.third = new Employee("Dave", "Matthews");
		this.third.setAge(43);
		this.third.setGender(Gender.MALE);
		this.third.setConstellation(Constellation.Cancer);
		this.third.setEmailAddress("no@email.com");
		this.fourth = new Employee("kevin", "raymond");
		this.fourth.setAge(31);
		this.fourth.setGender(Gender.FEMALE);
		this.fourth.setConstellation(Constellation.Libra);
		this.fourth.setEmailAddress("no@email.com");

	}

	protected void flushTestCustomers() {

		this.first = this.repository.saveSelective(this.first);
		this.second = this.repository.save(this.second);
		this.third = this.repository.save(this.third);
		this.fourth = this.repository.save(this.fourth);

		this.name = this.first.getName();

		Assertions.assertThat(this.name).isNotNull();
		Assertions.assertThat(this.second.getName()).isNotNull();
		Assertions.assertThat(this.third.getName()).isNotNull();
		Assertions.assertThat(this.fourth.getName()).isNotNull();

		Assertions.assertThat(this.repository.existsById(this.name)).isTrue();
		Assertions.assertThat(this.repository.existsById(this.second.getName())).isTrue();
		Assertions.assertThat(this.repository.existsById(this.third.getName())).isTrue();
		Assertions.assertThat(this.repository.existsById(this.fourth.getName())).isTrue();
	}

	@Test
	public void testCreation() {

		long before = this.repository.count();

		this.flushTestCustomers();

		Assertions.assertThat(this.repository.count()).isEqualTo(before + 4L);
	}

	@Test
	public void testRead() {

		this.flushTestCustomers();

		Assertions.assertThat(this.repository.findById(this.name)).map(Employee::getName)
				.contains(this.first.getName());
	}

	@Test
	public void findsAllByGivenIds() {

		this.flushTestCustomers();

		Assertions.assertThat(this.repository.findAllById(Arrays.asList(this.first.getName(), this.second.getName())))
				.contains(this.first, this.second);
	}

	@Test
	public void savingEmptyCollectionIsNoOp() {
		Assertions.assertThat(this.repository.saveAll(new ArrayList<>())).isEmpty();
	}

	@Test
	public void deletesById() {

		this.flushTestCustomers();

		this.repository.deleteById(this.first.getName());

		Assertions.assertThat(this.repository.existsById(this.name)).isFalse();
		Assertions.assertThat(this.repository.findById(this.name)).isNotPresent();
	}

	@Test
	public void returnsAllSortedCorrectly() {

		this.flushTestCustomers();
		List<Employee> all = this.repository.findAll(Sort.by(Direction.ASC, "name.lastname"));
		Assertions.assertThat(all).hasSize(4).containsExactly(this.second, this.first, this.third, this.fourth);
	}

	@Test
	public void returnsAllIgnoreCaseSortedCorrectly() {

		this.flushTestCustomers();

		Order order = new Order(Direction.ASC, "name.firstname").ignoreCase();

		Assertions.assertThat(this.repository.findAll(Sort.by(order))) //
				.hasSize(4)//
				.containsExactly(this.third, this.second, this.fourth, this.first);
	}

	@Test
	public void deleteCollectionOfEntities() {

		this.flushTestCustomers();

		long before = this.repository.count();

		this.repository.deleteAll(Arrays.asList(this.first, this.second));

		Assertions.assertThat(this.repository.existsById(this.first.getName())).isFalse();
		Assertions.assertThat(this.repository.existsById(this.second.getName())).isFalse();
		Assertions.assertThat(this.repository.count()).isEqualTo(before - 2);
	}

	@Test
	public void batchDeleteCollectionOfEntities() {

		this.flushTestCustomers();

		long before = this.repository.count();

		this.repository.deleteInBatch(Arrays.asList(this.first, this.second));

		Assertions.assertThat(this.repository.existsById(this.first.getName())).isFalse();
		Assertions.assertThat(this.repository.existsById(this.second.getName())).isFalse();
		Assertions.assertThat(this.repository.count()).isEqualTo(before - 2);
	}

	@Test
	public void deleteEmptyCollectionDoesNotDeleteAnything() {

		this.assertDeleteCallDoesNotDeleteAnything(new ArrayList<>());
	}

	private void assertDeleteCallDoesNotDeleteAnything(List<Employee> collection) {

		this.flushTestCustomers();
		long count = this.repository.count();

		this.repository.deleteAll(collection);
		Assertions.assertThat(this.repository.count()).isEqualTo(count);
	}

}
