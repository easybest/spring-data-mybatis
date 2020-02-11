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

import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.mybatis.domain.sample.Role;
import org.springframework.data.mybatis.domain.sample.User;
import org.springframework.data.mybatis.repository.sample.RoleRepository;
import org.springframework.data.mybatis.repository.sample.UserRepository;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Integration test for executing finders, thus testing various query lookup strategies.
 *
 * @see QueryLookupStrategy
 * @author JARVIS SONG
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:config/namespace-application-context.xml")
@Transactional
public class UserRepositoryFinderTests {

	@Autowired
	UserRepository userRepository;

	@Autowired
	RoleRepository roleRepository;

	User dave;

	User carter;

	User oliver;

	Role drummer;

	Role guitarist;

	Role singer;

	@Before
	public void setUp() {

		this.drummer = this.roleRepository.save(new Role("DRUMMER"));
		this.guitarist = this.roleRepository.save(new Role("GUITARIST"));
		this.singer = this.roleRepository.save(new Role("SINGER"));

		this.dave = this.userRepository.save(new User("Dave", "Matthews", "dave@dmband.com", this.singer));
		this.carter = this.userRepository
				.save(new User("Carter", "Beauford", "carter@dmband.com", this.singer, this.drummer));
		this.oliver = this.userRepository.save(new User("Oliver August", "Matthews", "oliver@dmband.com"));
	}

	@After
	public void clearUp() {

		this.userRepository.deleteAll();
		this.roleRepository.deleteAll();
	}

	/**
	 * Tests creation of a simple query.
	 */
	@Test
	public void testSimpleCustomCreatedFinder() {

		User user = this.userRepository.findByEmailAddressAndLastname("dave@dmband.com", "Matthews");
		Assert.assertEquals(this.dave, user);
	}

	/**
	 * Tests that the repository returns {@code null} for not found objects for finder
	 * methods that return a single domain object.
	 */
	@Test
	public void returnsNullIfNothingFound() {

		User user = this.userRepository.findByEmailAddress("foobar");
		Assert.assertEquals(null, user);
	}

	/**
	 * Tests creation of a simple query consisting of {@code AND} and {@code OR} parts.
	 */
	@Test
	public void testAndOrFinder() {

		List<User> users = this.userRepository.findByEmailAddressAndLastnameOrFirstname("dave@dmband.com", "Matthews",
				"Carter");

		Assert.assertNotNull(users);
		Assert.assertEquals(2, users.size());
		Assert.assertTrue(users.contains(this.dave));
		Assert.assertTrue(users.contains(this.carter));
	}

	@Test
	public void executesPagingMethodToPageCorrectly() {

		Page<User> page = this.userRepository.findByLastname(PageRequest.of(0, 1), "Matthews");
		Assert.assertThat(page.getNumberOfElements(), is(1));
		Assert.assertThat(page.getTotalElements(), is(2L));
		Assert.assertThat(page.getTotalPages(), is(2));
	}

	@Test
	public void executesPagingMethodToListCorrectly() {

		List<User> list = this.userRepository.findByFirstname("Carter", PageRequest.of(0, 1));
		Assert.assertThat(list.size(), is(1));
	}

	@Test
	public void executesInKeywordForPageCorrectly() {

		Page<User> page = this.userRepository.findByFirstnameIn(PageRequest.of(0, 1), "Dave", "Oliver August");

		Assert.assertThat(page.getNumberOfElements(), is(1));
		Assert.assertThat(page.getTotalElements(), is(2L));
		Assert.assertThat(page.getTotalPages(), is(2));
	}

	@Test
	public void executesNotInQueryCorrectly() throws Exception {

		List<User> result = this.userRepository.findByFirstnameNotIn(Arrays.asList("Dave", "Carter"));
		Assert.assertThat(result.size(), is(1));
		Assert.assertThat(result.get(0), is(this.oliver));
	}

	@Test
	public void findsByLastnameIgnoringCase() throws Exception {
		List<User> result = this.userRepository.findByLastnameIgnoringCase("BeAUfoRd");
		Assert.assertThat(result.size(), is(1));
		Assert.assertThat(result.get(0), is(this.carter));
	}

	@Test
	public void findsByLastnameIgnoringCaseLike() throws Exception {
		List<User> result = this.userRepository.findByLastnameIgnoringCaseLike("BeAUfo%");
		Assert.assertThat(result.size(), is(1));
		Assert.assertThat(result.get(0), is(this.carter));
	}

	@Test
	public void findByLastnameAndFirstnameAllIgnoringCase() throws Exception {
		List<User> result = this.userRepository.findByLastnameAndFirstnameAllIgnoringCase("MaTTheWs", "DaVe");
		Assert.assertThat(result.size(), is(1));
		Assert.assertThat(result.get(0), is(this.dave));
	}

	@Test
	public void respectsPageableOrderOnQueryGenerateFromMethodName() throws Exception {
		Page<User> ascending = this.userRepository.findByLastnameIgnoringCase(
				PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "firstname")), "Matthews");
		Page<User> descending = this.userRepository.findByLastnameIgnoringCase(
				PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "firstname")), "Matthews");
		Assert.assertThat(ascending.getTotalElements(), is(2L));
		Assert.assertThat(descending.getTotalElements(), is(2L));
		Assert.assertThat(ascending.getContent().get(0).getFirstname(),
				is(not(equalTo(descending.getContent().get(0).getFirstname()))));
		Assert.assertThat(ascending.getContent().get(0).getFirstname(),
				is(equalTo(descending.getContent().get(1).getFirstname())));
		Assert.assertThat(ascending.getContent().get(1).getFirstname(),
				is(equalTo(descending.getContent().get(0).getFirstname())));
	}

	@Test
	public void executesQueryToSlice() {

		Slice<User> slice = this.userRepository.findSliceByLastname("Matthews",
				PageRequest.of(0, 1, Sort.Direction.ASC, "firstname"));

		Assert.assertThat(slice.getContent(), hasItem(this.dave));
		Assert.assertThat(slice.hasNext(), is(true));
	}

	@Test
	public void executesQueryToSliceWithUnpaged() {

		Slice<User> slice = this.userRepository.findSliceByLastname("Matthews", Pageable.unpaged());

		Assert.assertThat(slice, containsInAnyOrder(this.dave, this.oliver));
		Assert.assertThat(slice.getNumberOfElements(), is(2));
		Assert.assertThat(slice.hasNext(), is(false));
	}

	@Test
	public void executesMethodWithNotContainingOnStringCorrectly() {
		Assert.assertThat(this.userRepository.findByLastnameNotContaining("u"),
				containsInAnyOrder(this.dave, this.oliver));
	}

	@Test
	public void parametersForContainsGetProperlyEscaped() {
		List<User> users = this.userRepository.findByFirstnameContaining("liv\\%");
		Assert.assertThat(users, iterableWithSize(0));
	}

	@Test
	public void escapingInLikeSpels() {

		User extra = new User("extra", "Matt_ew", "extra");
		this.userRepository.save(extra);

		Assert.assertThat(this.userRepository.findContainingEscaped("att_"), Matchers.contains(extra));
	}

	@Test
	public void escapingInLikeSpelsInThePresenceOfEscapeCharacters() {

		User withEscapeCharacter = this.userRepository.save(new User("extra", "Matt\\xew", "extra1"));
		this.userRepository.save(new User("extra", "Matt\\_ew", "extra2"));

		Assert.assertThat(this.userRepository.findContainingEscaped("att\\x"), contains(withEscapeCharacter));
	}

	@Test
	public void escapingInLikeSpelsInThePresenceOfEscapedWildcards() {

		this.userRepository.save(new User("extra", "Matt\\xew", "extra1"));
		User withEscapedWildcard = this.userRepository.save(new User("extra", "Matt\\_ew", "extra2"));

		Assert.assertThat(this.userRepository.findContainingEscaped("att\\_"), contains(withEscapedWildcard));
	}

	@Test
	public void translatesContainsToMemberOf() {

		List<User> singers = this.userRepository.findByRolesContaining(this.singer);

		Assert.assertThat(singers, hasSize(2));
		Assert.assertThat(singers, hasItems(this.dave, this.carter));
		Assert.assertThat(this.userRepository.findByRolesContaining(this.drummer), contains(this.carter));
	}

	@Test
	public void translatesNotContainsToNotMemberOf() {
		Assert.assertThat(this.userRepository.findByRolesNotContaining(this.drummer), hasItems(this.dave, this.oliver));
	}

	@Test
	public void executesQueryWithProjectionContainingReferenceToPluralAttribute() {
		Assert.assertThat(this.userRepository.findRolesAndFirstnameBy(), is(notNullValue()));
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void rejectsStreamExecutionIfNoSurroundingTransactionActive() {
		this.userRepository.findAllByCustomQueryAndStream();
	}

	@Test
	public void executesNamedQueryWithConstructorExpression() {
		this.userRepository.findByNamedQueryWithConstructorExpression();
	}

}
