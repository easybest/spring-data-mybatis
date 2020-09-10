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

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mybatis.domain.sample.Address;
import org.springframework.data.mybatis.domain.sample.Person;
import org.springframework.data.mybatis.domain.sample.Role;
import org.springframework.data.mybatis.domain.sample.User;
import org.springframework.data.mybatis.repository.sample.PersonRepository;
import org.springframework.data.mybatis.repository.sample.RoleRepository;
import org.springframework.data.mybatis.repository.sample.UserRepository;
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
@ContextConfiguration(locations = "classpath:config/namespace-application-context.xml")
@Transactional
public class UserRepositoryTest {

	@Autowired
	private UserRepository repository;

	@Autowired
	private PersonRepository personRepository;

	@Autowired
	private RoleRepository roleRepository;

	User first;

	User second;

	User third;

	User fourth;

	Role apple;

	Role orange;

	Role banana;

	Role grape;

	Person pig;

	Person dog;

	Person cat;

	@Before
	public void setUp() {
		this.apple = new Role("apple");
		this.orange = new Role("orange");
		this.banana = new Role("banana");
		this.grape = new Role("grape");

		this.pig = new Person("Pig", "Smart", new Address("USA", "NY", "Queen", "351"));
		this.dog = new Person("Dog", "Steve", new Address("FK", "PA", "King", "510"));
		this.cat = new Person("Cat", "George", new Address("JP", "TK", "Prince", "69"));

		this.first = new User("smart", "smart@gmail.com");
		this.second = new User("steve", "steve@hotmail.com");
		this.third = new User("george", "george@live.com");
		this.fourth = new User("george1", "george@gmail.com");

	}

	protected void flushUsers() {
		this.roleRepository.saveSelectiveAll(this.apple, this.orange, this.banana, this.grape);
		this.personRepository.saveAll(this.pig, this.dog, this.cat);

		this.first.setPerson(this.pig);
		this.second.setPerson(this.dog);
		this.third.setPerson(this.dog);
		this.fourth.setPerson(this.cat);

		this.first.addRoles(this.apple, this.orange, this.banana);
		this.second.addRoles(this.orange, this.banana);
		this.third.addRoles(this.banana, this.grape);
		this.fourth.addRoles(this.grape);

		this.repository.saveSelectiveAll(this.first, this.second, this.third, this.fourth);
	}

	@Test
	public void testCascadeFind() {
		this.flushUsers();
		List<User> users = this.repository.findAll();
		assertThat(users).hasSize(4).contains(this.first, this.second, this.third, this.fourth);
	}

}
