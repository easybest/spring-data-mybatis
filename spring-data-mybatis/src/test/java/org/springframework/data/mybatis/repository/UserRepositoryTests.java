/*
 * Copyright 2008-2019 the original author or authors.
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.GenericPropertyMatcher;
import org.springframework.data.domain.ExampleMatcher.StringMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mybatis.domain.sample.Address;
import org.springframework.data.mybatis.domain.sample.Role;
import org.springframework.data.mybatis.domain.sample.User;
import org.springframework.data.mybatis.repository.sample.RoleRepository;
import org.springframework.data.mybatis.repository.sample.UserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base integration test class for {@code UserRepository}. Loads a basic (non-namespace)
 * Spring configuration file as well as Hibernate configuration to execute tests.
 * <p>
 * To test further persistence providers subclass this class and provide a custom provider
 * configuration.
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context.xml")
@Transactional
public class UserRepositoryTests {

	@Autowired
	UserRepository repository;

	@Autowired
	RoleRepository roleRepository;

	User firstUser;

	User secondUser;

	User thirdUser;

	User fourthUser;

	Integer id;

	Role adminRole;

	@Before
	public void setUp() throws Exception {

		this.firstUser = new User("Oliver", "Gierke", "gierke@synyx.de");
		this.firstUser.setAge(28);
		this.secondUser = new User("Joachim", "Arrasz", "arrasz@synyx.de");
		this.secondUser.setAge(35);
		Thread.sleep(10);
		this.thirdUser = new User("Dave", "Matthews", "no@email.com");
		this.thirdUser.setAge(43);
		this.fourthUser = new User("kevin", "raymond", "no@gmail.com");
		this.fourthUser.setAge(31);
		this.adminRole = new Role("admin");

	}

	@Test
	public void testRead() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.findById(this.id)).map(User::getFirstname)
				.contains(this.firstUser.getFirstname());
	}

	@Test
	public void findsAllByGivenIds() {

		this.flushTestUsers();

		Assertions
				.assertThat(this.repository.findAllById(Arrays.asList(this.firstUser.getId(), this.secondUser.getId())))
				.contains(this.firstUser, this.secondUser);
	}

	@Test
	public void testReadByIdReturnsNullForNotFoundEntities() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.findById(this.id * 27)).isNotPresent();
	}

	@Test
	public void savesCollectionCorrectly() {

		Assertions.assertThat(this.repository.saveAll(Arrays.asList(this.firstUser, this.secondUser, this.thirdUser)))
				.hasSize(3).contains(this.firstUser, this.secondUser, this.thirdUser);
	}

	@Test
	public void savingEmptyCollectionIsNoOp() {
		Assertions.assertThat(this.repository.saveAll(new ArrayList<>())).isEmpty();
	}

	@Test
	public void testUpdate() {

		this.flushTestUsers();

		User foundPerson = this.repository.findById(this.id).get();
		foundPerson.setLastname("Schlicht");

		Assertions.assertThat(this.repository.findById(this.id)).map(User::getFirstname)
				.contains(foundPerson.getFirstname());
	}

	@Test
	public void existReturnsWhetherAnEntityCanBeLoaded() {

		this.flushTestUsers();
		Assertions.assertThat(this.repository.existsById(this.id)).isTrue();
		Assertions.assertThat(this.repository.existsById(this.id * 27)).isFalse();
	}

	@Test
	public void deletesAUserById() {

		this.flushTestUsers();

		this.repository.deleteById(this.firstUser.getId());

		Assertions.assertThat(this.repository.existsById(this.id)).isFalse();
		Assertions.assertThat(this.repository.findById(this.id)).isNotPresent();
	}

	@Test
	public void testDelete() {

		this.flushTestUsers();

		this.repository.delete(this.firstUser);

		Assertions.assertThat(this.repository.existsById(this.id)).isFalse();
		Assertions.assertThat(this.repository.findById(this.id)).isNotPresent();
	}

	@Test
	public void returnsAllSortedCorrectly() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.findAll(Sort.by(Direction.ASC, "lastname"))).hasSize(4)
				.containsExactly(this.secondUser, this.firstUser, this.thirdUser, this.fourthUser);
	}

	@Test
	public void returnsAllIgnoreCaseSortedCorrectly() {

		this.flushTestUsers();

		Order order = new Order(Direction.ASC, "firstname").ignoreCase();

		Assertions.assertThat(this.repository.findAll(Sort.by(order))) //
				.hasSize(4)//
				.containsExactly(this.thirdUser, this.secondUser, this.fourthUser, this.firstUser);
	}

	@Test
	public void deleteColletionOfEntities() {

		this.flushTestUsers();

		long before = this.repository.count();

		this.repository.deleteAll(Arrays.asList(this.firstUser, this.secondUser));

		Assertions.assertThat(this.repository.existsById(this.firstUser.getId())).isFalse();
		Assertions.assertThat(this.repository.existsById(this.secondUser.getId())).isFalse();
		Assertions.assertThat(this.repository.count()).isEqualTo(before - 2);
	}

	@Test
	public void batchDeleteColletionOfEntities() {

		this.flushTestUsers();

		long before = this.repository.count();

		this.repository.deleteInBatch(Arrays.asList(this.firstUser, this.secondUser));

		Assertions.assertThat(this.repository.existsById(this.firstUser.getId())).isFalse();
		Assertions.assertThat(this.repository.existsById(this.secondUser.getId())).isFalse();
		Assertions.assertThat(this.repository.count()).isEqualTo(before - 2);
	}

	@Test
	public void deleteEmptyCollectionDoesNotDeleteAnything() {

		this.assertDeleteCallDoesNotDeleteAnything(new ArrayList<>());
	}

	@Test
	public void executesManipulatingQuery() {

		this.flushTestUsers();
		this.repository.renameAllUsersTo("newLastname");

		long expected = this.repository.count();
		Assertions.assertThat(this.repository.findByLastname("newLastname").size())
				.isEqualTo(Long.valueOf(expected).intValue());
	}

	@Test
	public void testFinderInvocationWithNullParameter() {

		this.flushTestUsers();

		this.repository.findByLastname(null);
	}

	@Test
	public void testFindByLastname() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.findByLastname("Gierke")).containsOnly(this.firstUser);
	}

	/**
	 * Tests, that searching by the email address of the reference user returns exactly
	 * that instance.
	 */
	@Test
	public void testFindByEmailAddress() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.findByEmailAddress("gierke@synyx.de")).isEqualTo(this.firstUser);
	}

	/**
	 * Tests reading all users.
	 */
	@Test
	public void testReadAll() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.count()).isEqualTo(4L);
		Assertions.assertThat(this.repository.findAll()).contains(this.firstUser, this.secondUser, this.thirdUser,
				this.fourthUser);
	}

	/**
	 * Tests that all users get deleted by triggering {@link UserRepository#deleteAll()}.
	 */
	@Test
	public void deleteAll() {

		this.flushTestUsers();

		this.repository.deleteAll();

		Assertions.assertThat(this.repository.count()).isZero();
	}

	@Test
	public void deleteAllInBatch() {

		this.flushTestUsers();

		this.repository.deleteAllInBatch();

		Assertions.assertThat(this.repository.count()).isZero();
	}

	/**
	 * Tests cascading persistence.
	 */
	@Test
	public void testCascadesPersisting() {

		// Create link prior to persisting
		this.firstUser.addColleague(this.secondUser);

		// Persist
		this.flushTestUsers();

		// Fetches first user from database
		User firstReferenceUser = this.repository.findById(this.firstUser.getId()).get();
		Assertions.assertThat(firstReferenceUser).isEqualTo(this.firstUser);

		// Fetch colleagues and assert link
		Set<User> colleagues = firstReferenceUser.getColleagues();
		Assertions.assertThat(colleagues).containsOnly(this.secondUser);
	}

	/**
	 * Tests, that persisting a relationsship without cascade attributes throws a
	 * {@code DataAccessException}.
	 */
	@Test(expected = DataAccessException.class)
	public void testPreventsCascadingRolePersisting() {

		this.firstUser.addRole(new Role("USER"));

		this.flushTestUsers();
	}

	/**
	 * Tests cascading on {@literal merge} operation.
	 */
	@Test
	public void testMergingCascadesCollegueas() {

		this.firstUser.addColleague(this.secondUser);
		this.flushTestUsers();

		this.firstUser.addColleague(new User("Florian", "Hopf", "hopf@synyx.de"));
		this.firstUser = this.repository.save(this.firstUser);

		User reference = this.repository.findById(this.firstUser.getId()).get();
		Set<User> colleagues = reference.getColleagues();

		Assertions.assertThat(colleagues).hasSize(2);
	}

	@Test
	public void testCountsCorrectly() {

		long count = this.repository.count();

		User user = new User();
		user.setEmailAddress("gierke@synyx.de");
		this.repository.save(user);

		Assertions.assertThat(this.repository.count()).isEqualTo(count + 1);
	}

	@Test
	public void testInvocationOfCustomImplementation() {

		this.repository.someCustomMethod(new User());
	}

	@Test
	public void testOverwritingFinder() {

		this.repository.findByOverrridingMethod();
	}

	@Test
	public void testUsesQueryAnnotation() {

		Assertions.assertThat(this.repository.findByAnnotatedQuery("gierke@synyx.de")).isNull();
	}

	@Test
	public void testExecutionOfProjectingMethod() {

		this.flushTestUsers();
		Assertions.assertThat(this.repository.countWithFirstname("Oliver")).isEqualTo(1L);
	}

	@Test
	public void executesMethodWithAnnotatedNamedParametersCorrectly() {

		this.firstUser = this.repository.save(this.firstUser);
		this.secondUser = this.repository.save(this.secondUser);

		Assertions.assertThat(this.repository.findByLastnameOrFirstname("Oliver", "Arrasz")).contains(this.firstUser,
				this.secondUser);
	}

	@Test
	public void executesMethodWithNamedParametersCorrectlyOnMethodsWithQueryCreation() {

		this.firstUser = this.repository.save(this.firstUser);
		this.secondUser = this.repository.save(this.secondUser);

		Assertions.assertThat(this.repository.findByFirstnameOrLastname("Oliver", "Arrasz"))
				.containsOnly(this.firstUser, this.secondUser);
	}

	@Test
	public void executesLikeAndOrderByCorrectly() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.findByLastnameLikeOrderByFirstnameDesc("%r%")).hasSize(3)
				.containsExactly(this.fourthUser, this.firstUser, this.secondUser);
	}

	@Test
	public void executesNotLikeCorrectly() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.findByLastnameNotLike("%er%")).containsOnly(this.secondUser,
				this.thirdUser, this.fourthUser);
	}

	@Test
	public void executesSimpleNotCorrectly() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.findByLastnameNot("Gierke")).containsOnly(this.secondUser, this.thirdUser,
				this.fourthUser);
	}

	@Test
	public void returnsSameListIfNoSortIsGiven() {

		this.flushTestUsers();
		assertSameElements(this.repository.findAll(Sort.unsorted()), this.repository.findAll());
	}

	@Test
	public void returnsAllAsPageIfNoPageableIsGiven() {

		this.flushTestUsers();
		Assertions.assertThat(this.repository.findAll(Pageable.unpaged()))
				.isEqualTo(new PageImpl<>(this.repository.findAll()));
	}

	@Test
	public void executesQueryMethodWithDeepTraversalCorrectly() {

		this.flushTestUsers();

		this.firstUser.setManager(this.secondUser);
		this.thirdUser.setManager(this.firstUser);
		this.repository.saveAll(Arrays.asList(this.firstUser, this.thirdUser));

		Assertions.assertThat(this.repository.findByManagerLastname("Arrasz")).containsOnly(this.firstUser);
		Assertions.assertThat(this.repository.findByManagerLastname("Gierke")).containsOnly(this.thirdUser);
	}

	@Test
	public void executesFindByColleaguesLastnameCorrectly() {

		this.flushTestUsers();

		this.firstUser.addColleague(this.secondUser);
		this.thirdUser.addColleague(this.firstUser);
		this.repository.saveAll(Arrays.asList(this.firstUser, this.thirdUser));

		Assertions.assertThat(this.repository.findByColleaguesLastname(this.secondUser.getLastname()))
				.containsOnly(this.firstUser);

		Assertions.assertThat(this.repository.findByColleaguesLastname("Gierke")).containsOnly(this.thirdUser,
				this.secondUser);
	}

	@Test
	public void executesFindByNotNullLastnameCorrectly() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.findByLastnameNotNull()).containsOnly(this.firstUser, this.secondUser,
				this.thirdUser, this.fourthUser);
	}

	@Test
	public void executesFindByNullLastnameCorrectly() {

		this.flushTestUsers();
		User forthUser = this.repository.save(new User("Foo", null, "email@address.com"));

		Assertions.assertThat(this.repository.findByLastnameNull()).containsOnly(forthUser);
	}

	@Test
	public void findsSortedByLastname() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.findByEmailAddressLike("%@%", Sort.by(Direction.ASC, "lastname")))
				.containsExactly(this.secondUser, this.firstUser, this.thirdUser, this.fourthUser);
	}

	@Test
	public void findsUsersBySpringDataNamedQuery() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.findBySpringDataNamedQuery("Gierke")).containsOnly(this.firstUser);
	}

	@Test
	public void readsPageWithGroupByClauseCorrectly() {

		this.flushTestUsers();

		Page<String> result = this.repository.findByLastnameGrouped(PageRequest.of(0, 10));
		Assertions.assertThat(result.getTotalPages()).isEqualTo(1);
	}

	@Test
	public void executesLessThatOrEqualQueriesCorrectly() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.findByAgeLessThanEqual(35)).containsOnly(this.firstUser, this.secondUser,
				this.fourthUser);
	}

	@Test
	public void executesGreaterThatOrEqualQueriesCorrectly() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.findByAgeGreaterThanEqual(35)).containsOnly(this.secondUser,
				this.thirdUser);
	}

	@Test
	public void executesNativeQueryCorrectly() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.findNativeByLastname("Matthews")).containsOnly(this.thirdUser);
	}

	@Test
	public void executesFinderWithTrueKeywordCorrectly() {

		this.flushTestUsers();
		this.firstUser.setActive(false);
		this.repository.save(this.firstUser);

		Assertions.assertThat(this.repository.findByActiveTrue()).containsOnly(this.secondUser, this.thirdUser,
				this.fourthUser);
	}

	@Test
	public void executesFinderWithFalseKeywordCorrectly() {

		this.flushTestUsers();
		this.firstUser.setActive(false);
		this.repository.save(this.firstUser);

		Assertions.assertThat(this.repository.findByActiveFalse()).containsOnly(this.firstUser);
	}

	/**
	 * Ignored until the query declaration is supported by OpenJPA.
	 */
	@Test
	public void executesAnnotatedCollectionMethodCorrectly() {

		this.flushTestUsers();
		this.firstUser.addColleague(this.thirdUser);
		this.repository.save(this.firstUser);

		List<User> result = this.repository.findColleaguesFor(this.firstUser);
		Assertions.assertThat(result).containsOnly(this.thirdUser);
	}

	@Test
	public void executesFinderWithAfterKeywordCorrectly() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.findByCreatedAtAfter(this.secondUser.getCreatedAt()))
				.containsOnly(this.thirdUser, this.fourthUser);
	}

	@Test
	public void executesFinderWithBeforeKeywordCorrectly() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.findByCreatedAtBefore(this.thirdUser.getCreatedAt()))
				.containsOnly(this.firstUser, this.secondUser);
	}

	@Test
	public void executesFinderWithStartingWithCorrectly() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.findByFirstnameStartingWith("Oli")).containsOnly(this.firstUser);
	}

	@Test
	public void executesFinderWithEndingWithCorrectly() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.findByFirstnameEndingWith("er")).containsOnly(this.firstUser);
	}

	@Test
	public void executesFinderWithContainingCorrectly() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.findByFirstnameContaining("a")).containsOnly(this.secondUser,
				this.thirdUser);
	}

	@Test
	public void allowsExecutingPageableMethodWithUnpagedArgument() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.findByFirstname("Oliver", null)).containsOnly(this.firstUser);

		Page<User> page = this.repository.findByFirstnameIn(Pageable.unpaged(), "Oliver");
		Assertions.assertThat(page.getNumberOfElements()).isEqualTo(1);
		Assertions.assertThat(page.getContent()).contains(this.firstUser);

		page = this.repository.findAll(Pageable.unpaged());
		Assertions.assertThat(page.getNumberOfElements()).isEqualTo(4);
		Assertions.assertThat(page.getContent()).contains(this.firstUser, this.secondUser, this.thirdUser,
				this.fourthUser);
	}

	@Test
	public void executesNativeQueryForNonEntitiesCorrectly() {

		this.flushTestUsers();

		List<Integer> result = this.repository.findOnesByNativeQuery();

		Assertions.assertThat(result.size()).isEqualTo(4);
		Assertions.assertThat(result).contains(1);
	}

	@Test
	public void handlesIterableOfIdsCorrectly() {

		this.flushTestUsers();

		Set<Integer> set = new HashSet<>();
		set.add(this.firstUser.getId());
		set.add(this.secondUser.getId());

		Assertions.assertThat(this.repository.findAllById(set)).containsOnly(this.firstUser, this.secondUser);
	}

	protected void flushTestUsers() {

		this.roleRepository.save(this.adminRole);

		this.firstUser = this.repository.saveSelective(this.firstUser);
		this.secondUser = this.repository.save(this.secondUser);
		this.thirdUser = this.repository.save(this.thirdUser);
		this.fourthUser = this.repository.save(this.fourthUser);

		this.id = this.firstUser.getId();

		Assertions.assertThat(this.id).isNotNull();
		Assertions.assertThat(this.secondUser.getId()).isNotNull();
		Assertions.assertThat(this.thirdUser.getId()).isNotNull();
		Assertions.assertThat(this.fourthUser.getId()).isNotNull();

		Assertions.assertThat(this.repository.existsById(this.id)).isTrue();
		Assertions.assertThat(this.repository.existsById(this.secondUser.getId())).isTrue();
		Assertions.assertThat(this.repository.existsById(this.thirdUser.getId())).isTrue();
		Assertions.assertThat(this.repository.existsById(this.fourthUser.getId())).isTrue();
	}

	private static <T> void assertSameElements(Collection<T> first, Collection<T> second) {

		for (T element : first) {
			Assertions.assertThat(element).isIn(second);
		}

		for (T element : second) {
			Assertions.assertThat(element).isIn(first);
		}
	}

	private void assertDeleteCallDoesNotDeleteAnything(List<User> collection) {

		this.flushTestUsers();
		long count = this.repository.count();

		this.repository.deleteAll(collection);
		Assertions.assertThat(this.repository.count()).isEqualTo(count);
	}

	@Test
	public void ordersByReferencedEntityCorrectly() {

		this.flushTestUsers();
		this.firstUser.setManager(this.thirdUser);
		this.repository.save(this.firstUser);

		Page<User> all = this.repository.findAll(PageRequest.of(0, 10, Sort.by("manager.id")));

		Assertions.assertThat(all.getContent().isEmpty()).isFalse();
	}

	@Test
	public void bindsSortingToOuterJoinCorrectly() {

		this.flushTestUsers();

		// Managers not set, make sure adding the sort does not rule out those Users
		Page<User> result = this.repository.findAllPaged(PageRequest.of(0, 10, Sort.by("manager.lastname")));
		Assertions.assertThat(result.getContent()).hasSize((int) this.repository.count());
	}

	@Test
	public void shouldGenerateLeftOuterJoinInfindAllWithPaginationAndSortOnNestedPropertyPath() {

		this.firstUser.setManager(null);
		this.secondUser.setManager(null);
		this.thirdUser.setManager(this.firstUser); // manager Oliver
		this.fourthUser.setManager(this.secondUser); // manager Joachim

		this.flushTestUsers();

		Page<User> pages = this.repository
				.findAll(PageRequest.of(0, 4, Sort.by(Sort.Direction.ASC, "manager.firstname")));
		Assertions.assertThat(pages.getSize()).isEqualTo(4);
		Assertions.assertThat(pages.getContent().get(0).getManager()).isNull();
		Assertions.assertThat(pages.getContent().get(1).getManager()).isNull();
		Assertions.assertThat(pages.getContent().get(2).getManager().getFirstname()).isEqualTo("Joachim");
		Assertions.assertThat(pages.getContent().get(3).getManager().getFirstname()).isEqualTo("Oliver");
		Assertions.assertThat(pages.getTotalElements()).isEqualTo(4L);
	}

	@Test
	public void executesManualQueryWithPositionLikeExpressionCorrectly() {

		this.flushTestUsers();

		List<User> result = this.repository.findByFirstnameLike("Da");

		Assertions.assertThat(result).containsOnly(this.thirdUser);
	}

	@Test
	public void executesManualQueryWithNamedLikeExpressionCorrectly() {

		this.flushTestUsers();

		List<User> result = this.repository.findByFirstnameLikeNamed("Da");

		Assertions.assertThat(result).containsOnly(this.thirdUser);
	}

	@Test
	public void executesDerivedCountQueryToLong() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.countByLastname("Matthews")).isEqualTo(1L);
	}

	@Test
	public void executesDerivedCountQueryToInt() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.countUsersByFirstname("Dave")).isEqualTo(1);
	}

	@Test
	public void executesDerivedExistsQuery() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.existsByLastname("Matthews")).isEqualTo(true);
		Assertions.assertThat(this.repository.existsByLastname("Hans Peter")).isEqualTo(false);
	}

	@Test
	public void findAllReturnsEmptyIterableIfNoIdsGiven() {

		Assertions.assertThat(this.repository.findAllById(Collections.emptySet())).isEmpty();
	}

	@Test
	public void executesManuallyDefinedQueryWithFieldProjection() {

		this.flushTestUsers();
		List<String> lastname = this.repository.findFirstnamesByLastname("Matthews");

		Assertions.assertThat(lastname).containsOnly("Dave");
	}

	@Test
	public void looksUpEntityReference() {

		this.flushTestUsers();

		User result = this.repository.getById(this.firstUser.getId());
		Assertions.assertThat(result).isEqualTo(this.firstUser);
	}

	@Test
	public void invokesQueryWithVarargsParametersCorrectly() {

		this.flushTestUsers();

		Collection<User> result = this.repository.findByIdIn(this.firstUser.getId(), this.secondUser.getId());

		Assertions.assertThat(result).containsOnly(this.firstUser, this.secondUser);
	}

	@Test
	public void shouldSupportModifyingQueryWithVarArgs() {

		this.flushTestUsers();

		this.repository.updateUserActiveState(false, this.firstUser.getId(), this.secondUser.getId(),
				this.thirdUser.getId(), this.fourthUser.getId());

		long expectedCount = this.repository.count();
		Assertions.assertThat(this.repository.findByActiveFalse().size()).isEqualTo((int) expectedCount);
		Assertions.assertThat(this.repository.findByActiveTrue().size()).isEqualTo(0);
	}

	@Test
	public void executesFinderWithOrderClauseOnly() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.findAllByOrderByLastnameAsc()).containsOnly(this.secondUser,
				this.firstUser, this.thirdUser, this.fourthUser);
	}

	@Test
	public void sortByAssociationPropertyShouldUseLeftOuterJoin() {

		this.secondUser.getColleagues().add(this.firstUser);
		this.fourthUser.getColleagues().add(this.thirdUser);
		this.flushTestUsers();

		List<User> result = this.repository.findAll(Sort.by(Sort.Direction.ASC, "colleagues.id"));

		Assertions.assertThat(result).hasSize(4);
	}

	@Test
	public void sortByAssociationPropertyInPageableShouldUseLeftOuterJoin() {

		this.secondUser.getColleagues().add(this.firstUser);
		this.fourthUser.getColleagues().add(this.thirdUser);
		this.flushTestUsers();

		Page<User> page = this.repository.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "colleagues.id")));

		Assertions.assertThat(page.getContent()).hasSize(4);
	}

	@Test
	public void sortByEmbeddedProperty() {

		this.thirdUser.setAddress(new Address("Germany", "Saarbrücken", "HaveItYourWay", "123"));
		this.flushTestUsers();

		Page<User> page = this.repository
				.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "address.streetName")));

		Assertions.assertThat(page.getContent()).hasSize(4);
		Assertions.assertThat(page.getContent().get(3)).isEqualTo(this.thirdUser);
	}

	@Test
	public void findsUserByBinaryDataReference() {

		byte[] data = "Woho!!".getBytes(StandardCharsets.UTF_8);
		this.firstUser.setBinaryData(data);

		this.flushTestUsers();

		List<User> result = this.repository.findByBinaryData(data);
		Assertions.assertThat(result).containsOnly(this.firstUser);
		Assertions.assertThat(result.get(0).getBinaryData()).isEqualTo(data);
	}

	@Test
	public void customFindByQueryWithPositionalVarargsParameters() {

		this.flushTestUsers();

		Collection<User> result = this.repository.findByIdsCustomWithPositionalVarArgs(this.firstUser.getId(),
				this.secondUser.getId());

		Assertions.assertThat(result).containsOnly(this.firstUser, this.secondUser);
	}

	@Test
	public void customFindByQueryWithNamedVarargsParameters() {

		this.flushTestUsers();

		Collection<User> result = this.repository.findByIdsCustomWithNamedVarArgs(this.firstUser.getId(),
				this.secondUser.getId());

		Assertions.assertThat(result).containsOnly(this.firstUser, this.secondUser);
	}

	@Test
	public void sortByNestedAssociationPropertyWithSortInPageable() {

		this.firstUser.setManager(this.thirdUser);
		this.thirdUser.setManager(this.fourthUser);

		this.flushTestUsers();

		Page<User> page = this.repository.findAll(PageRequest.of(0, 10, //
				Sort.by(Sort.Direction.ASC, "manager.manager.firstname")));

		Assertions.assertThat(page.getContent()).hasSize(4);
		Assertions.assertThat(page.getContent().get(3)).isEqualTo(this.firstUser);
	}

	@Test
	public void sortByNestedAssociationPropertyWithSortOrderIgnoreCaseInPageable() {

		this.firstUser.setManager(this.thirdUser);
		this.thirdUser.setManager(this.fourthUser);

		this.flushTestUsers();

		Page<User> page = this.repository.findAll(PageRequest.of(0, 10, //
				Sort.by(new Sort.Order(Direction.ASC, "manager.manager.firstname").ignoreCase())));

		Assertions.assertThat(page.getContent()).hasSize(4);
		Assertions.assertThat(page.getContent().get(3)).isEqualTo(this.firstUser);
	}

	@Test
	public void findByElementCollectionAttribute() {

		this.firstUser.getAttributes().add("cool");
		this.secondUser.getAttributes().add("hip");
		this.thirdUser.getAttributes().add("rockstar");

		this.flushTestUsers();

		List<User> result = this.repository.findByAttributesIn(new HashSet<>(Arrays.asList("cool", "hip")));

		Assertions.assertThat(result).containsOnly(this.firstUser, this.secondUser);
	}

	@Test
	public void deleteByShouldReturnListOfDeletedElementsWhenRetunTypeIsCollectionLike() {

		this.flushTestUsers();

		List<User> result = this.repository.deleteByLastname(this.firstUser.getLastname());
		Assertions.assertThat(result).containsOnly(this.firstUser);
	}

	@Test
	public void deleteByShouldRemoveElementsMatchingDerivedQuery() {

		this.flushTestUsers();

		this.repository.deleteByLastname(this.firstUser.getLastname());
		Assertions.assertThat(this.repository.countByLastname(this.firstUser.getLastname())).isEqualTo(0L);
	}

	@Test
	public void deleteByShouldReturnNumberOfEntitiesRemovedIfReturnTypeIsLong() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.removeByLastname(this.firstUser.getLastname())).isEqualTo(1L);
	}

	@Test
	public void deleteByShouldReturnZeroInCaseNoEntityHasBeenRemovedAndReturnTypeIsNumber() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.removeByLastname("bubu")).isEqualTo(0L);
	}

	@Test
	public void deleteByShouldReturnEmptyListInCaseNoEntityHasBeenRemovedAndReturnTypeIsCollectionLike() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.deleteByLastname("dorfuaeB")).isEmpty();
	}

	@Test
	@Ignore
	public void findBinaryDataByIdJpaQl() {

		byte[] data = "Woho!!".getBytes(StandardCharsets.UTF_8);
		this.firstUser.setBinaryData(data);

		this.flushTestUsers();

		byte[] result = this.repository.findBinaryDataByIdNative(this.firstUser.getId());

		Assertions.assertThat(result.length).isEqualTo(data.length);
		Assertions.assertThat(result).isEqualTo(data);
	}

	@Test
	public void findBinaryDataByIdNative() {

		byte[] data = "Woho!!".getBytes(StandardCharsets.UTF_8);
		this.firstUser.setBinaryData(data);

		this.flushTestUsers();

		byte[] result = this.repository.findBinaryDataByIdNative(this.firstUser.getId());

		Assertions.assertThat(result).isEqualTo(data);
		Assertions.assertThat(result.length).isEqualTo(data.length);
	}

	@Test
	public void findPaginatedExplicitQueryWithCountQueryProjection() {

		this.firstUser.setFirstname(null);

		this.flushTestUsers();

		Page<User> result = this.repository.findAllByFirstnameLike("", PageRequest.of(0, 10));

		Assertions.assertThat(result.getContent().size()).isEqualTo(3);
	}

	@Test
	public void findPaginatedNamedQueryWithCountQueryProjection() {

		this.flushTestUsers();

		Page<User> result = this.repository.findByNamedQueryAndCountProjection("Gierke", PageRequest.of(0, 10));

		Assertions.assertThat(result.getContent().size()).isEqualTo(1);
	}

	@Test
	public void findOldestUser() {

		this.flushTestUsers();

		User oldest = this.thirdUser;

		Assertions.assertThat(this.repository.findFirstByOrderByAgeDesc()).isEqualTo(oldest);
		Assertions.assertThat(this.repository.findFirst1ByOrderByAgeDesc()).isEqualTo(oldest);
	}

	@Test
	public void findYoungestUser() {

		this.flushTestUsers();

		User youngest = this.firstUser;

		Assertions.assertThat(this.repository.findTopByOrderByAgeAsc()).isEqualTo(youngest);
		Assertions.assertThat(this.repository.findTop1ByOrderByAgeAsc()).isEqualTo(youngest);
	}

	@Test
	public void find2OldestUsers() {

		this.flushTestUsers();

		User oldest1 = this.thirdUser;
		User oldest2 = this.secondUser;

		Assertions.assertThat(this.repository.findFirst2ByOrderByAgeDesc()).contains(oldest1, oldest2);
		Assertions.assertThat(this.repository.findTop2ByOrderByAgeDesc()).contains(oldest1, oldest2);
	}

	@Test
	public void find2YoungestUsers() {

		this.flushTestUsers();

		User youngest1 = this.firstUser;
		User youngest2 = this.fourthUser;

		Assertions.assertThat(this.repository.findFirst2UsersBy(Sort.by(Direction.ASC, "age"))).contains(youngest1,
				youngest2);
		Assertions.assertThat(this.repository.findTop2UsersBy(Sort.by(Direction.ASC, "age"))).contains(youngest1,
				youngest2);
	}

	@Test
	public void find3YoungestUsersPageableWithPageSize2() {

		this.flushTestUsers();

		User youngest1 = this.firstUser;
		User youngest2 = this.fourthUser;
		User youngest3 = this.secondUser;

		Page<User> firstPage = this.repository.findFirst3UsersBy(PageRequest.of(0, 2, Direction.ASC, "age"));
		Assertions.assertThat(firstPage.getContent()).contains(youngest1, youngest2);

		Page<User> secondPage = this.repository.findFirst3UsersBy(PageRequest.of(1, 2, Direction.ASC, "age"));
		Assertions.assertThat(secondPage.getContent()).contains(youngest3);
	}

	@Test
	public void find2YoungestUsersPageableWithPageSize3() {

		this.flushTestUsers();

		User youngest1 = this.firstUser;
		User youngest2 = this.fourthUser;
		User youngest3 = this.secondUser;

		Page<User> firstPage = this.repository.findFirst2UsersBy(PageRequest.of(0, 3, Direction.ASC, "age"));
		Assertions.assertThat(firstPage.getContent()).contains(youngest1, youngest2);

		Page<User> secondPage = this.repository.findFirst2UsersBy(PageRequest.of(1, 3, Direction.ASC, "age"));
		Assertions.assertThat(secondPage.getContent()).contains(youngest3);
	}

	@Test
	public void find3YoungestUsersPageableWithPageSize2Sliced() {

		this.flushTestUsers();

		User youngest1 = this.firstUser;
		User youngest2 = this.fourthUser;
		User youngest3 = this.secondUser;

		Slice<User> firstPage = this.repository.findTop3UsersBy(PageRequest.of(0, 2, Direction.ASC, "age"));
		Assertions.assertThat(firstPage.getContent()).contains(youngest1, youngest2);

		Slice<User> secondPage = this.repository.findTop3UsersBy(PageRequest.of(1, 2, Direction.ASC, "age"));
		Assertions.assertThat(secondPage.getContent()).contains(youngest3);
	}

	@Test
	public void find2YoungestUsersPageableWithPageSize3Sliced() {

		this.flushTestUsers();

		User youngest1 = this.firstUser;
		User youngest2 = this.fourthUser;
		User youngest3 = this.secondUser;

		Slice<User> firstPage = this.repository.findTop2UsersBy(PageRequest.of(0, 3, Direction.ASC, "age"));
		Assertions.assertThat(firstPage.getContent()).contains(youngest1, youngest2);

		Slice<User> secondPage = this.repository.findTop2UsersBy(PageRequest.of(1, 3, Direction.ASC, "age"));
		Assertions.assertThat(secondPage.getContent()).contains(youngest3);
	}

	@Test
	public void pageableQueryReportsTotalFromResult() {

		this.flushTestUsers();

		Page<User> firstPage = this.repository.findAll(PageRequest.of(0, 10));
		Assertions.assertThat(firstPage.getContent()).hasSize(4);
		Assertions.assertThat(firstPage.getTotalElements()).isEqualTo(4L);

		Page<User> secondPage = this.repository.findAll(PageRequest.of(1, 3));
		Assertions.assertThat(secondPage.getContent()).hasSize(1);
		Assertions.assertThat(secondPage.getTotalElements()).isEqualTo(4L);
	}

	@Test
	public void pageableQueryReportsTotalFromCount() {

		this.flushTestUsers();

		Page<User> firstPage = this.repository.findAll(PageRequest.of(0, 4));
		Assertions.assertThat(firstPage.getContent()).hasSize(4);
		Assertions.assertThat(firstPage.getTotalElements()).isEqualTo(4L);

		Page<User> secondPage = this.repository.findAll(PageRequest.of(10, 10));
		Assertions.assertThat(secondPage.getContent()).hasSize(0);
		Assertions.assertThat(secondPage.getTotalElements()).isEqualTo(4L);
	}

	@Test
	public void invokesQueryWithWrapperType() {

		this.flushTestUsers();

		Optional<User> result = this.repository.findOptionalByEmailAddress("gierke@synyx.de");

		Assertions.assertThat(result.isPresent()).isEqualTo(true);
		Assertions.assertThat(result.get()).isEqualTo(this.firstUser);
	}

	@Test
	public void shouldFindUserByFirstnameAndLastnameWithSpelExpressionInStringBasedQuery() {

		this.flushTestUsers();
		List<User> users = this.repository.findByFirstnameAndLastnameWithSpelExpression("Oliver", "ierk");

		Assertions.assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	public void shouldFindUserByLastnameWithSpelExpressionInStringBasedQuery() {

		this.flushTestUsers();
		List<User> users = this.repository.findByLastnameWithSpelExpression("ierk");

		Assertions.assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	public void shouldFindBySpELExpressionWithoutArgumentsWithQuestionmark() {

		this.flushTestUsers();
		List<User> users = this.repository.findOliverBySpELExpressionWithoutArgumentsWithQuestionmark();

		Assertions.assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	public void shouldFindBySpELExpressionWithoutArgumentsWithColon() {

		this.flushTestUsers();
		List<User> users = this.repository.findOliverBySpELExpressionWithoutArgumentsWithColon();

		Assertions.assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	public void shouldFindUsersByAgeForSpELExpression() {

		this.flushTestUsers();
		List<User> users = this.repository.findUsersByAgeForSpELExpressionByIndexedParameter(35);

		Assertions.assertThat(users).containsOnly(this.secondUser);
	}

	@Test
	public void shouldfindUsersByFirstnameForSpELExpressionWithParameterNameVariableReference() {

		this.flushTestUsers();
		List<User> users = this.repository.findUsersByFirstnameForSpELExpression("Joachim");

		Assertions.assertThat(users).containsOnly(this.secondUser);
	}

	@Test
	public void shouldfindUsersByFirstnameForSpELExpressionOnlyWithParameterNameVariableReference() {

		this.flushTestUsers();
		List<User> users = this.repository.findUsersByFirstnameForSpELExpressionWithParameterVariableOnly("Joachim");

		Assertions.assertThat(users).containsOnly(this.secondUser);
	}

	@Test
	public void shouldfindUsersByFirstnameForSpELExpressionOnlyWithParameterIndexReference() {

		this.flushTestUsers();
		List<User> users = this.repository.findUsersByFirstnameForSpELExpressionWithParameterIndexOnly("Joachim");

		Assertions.assertThat(users).containsOnly(this.secondUser);
	}

	@Test
	public void shouldFindUsersInNativeQueryWithPagination() {

		this.flushTestUsers();

		Page<User> users = this.repository.findUsersInNativeQueryWithPagination(PageRequest.of(0, 3));

		SoftAssertions softly = new SoftAssertions();

		softly.assertThat(users.getContent()).extracting(User::getFirstname).containsExactly("Dave", "Joachim",
				"kevin");

		users = this.repository.findUsersInNativeQueryWithPagination(PageRequest.of(1, 3));

		softly.assertThat(users.getContent()).extracting(User::getFirstname).containsExactly("Oliver");

		softly.assertAll();
	}

	@Test
	public void shouldFindUsersByUserFirstnameAsSpELExpressionAndLastnameAsStringInStringBasedQuery() {

		this.flushTestUsers();

		List<User> users = this.repository.findUsersByUserFirstnameAsSpELExpressionAndLastnameAsString(this.firstUser,
				this.firstUser.getLastname());

		Assertions.assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	public void shouldFindUsersByFirstnameAsStringAndUserLastnameAsSpELExpressionInStringBasedQuery() {

		this.flushTestUsers();

		List<User> users = this.repository.findUsersByFirstnameAsStringAndUserLastnameAsSpELExpression(
				this.firstUser.getFirstname(), this.firstUser);

		Assertions.assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	public void shouldFindUsersByUserFirstnameAsSpELExpressionAndLastnameAsFakeSpELExpressionInStringBasedQuery() {

		this.flushTestUsers();

		List<User> users = this.repository.findUsersByUserFirstnameAsSpELExpressionAndLastnameAsFakeSpELExpression(
				this.firstUser, this.firstUser.getLastname());

		Assertions.assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	public void shouldFindUsersByFirstnameAsFakeSpELExpressionAndUserLastnameAsSpELExpressionInStringBasedQuery() {

		this.flushTestUsers();

		List<User> users = this.repository.findUsersByFirstnameAsFakeSpELExpressionAndUserLastnameAsSpELExpression(
				this.firstUser.getFirstname(), this.firstUser);

		Assertions.assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	public void shouldFindUsersByFirstnameWithLeadingPageableParameter() {

		this.flushTestUsers();

		List<User> users = this.repository.findUsersByFirstnamePaginated(PageRequest.of(0, 2),
				this.firstUser.getFirstname());

		Assertions.assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	public void shouldfindUsersBySpELExpressionParametersWithSpelTemplateExpression() {

		this.flushTestUsers();
		List<User> users = this.repository
				.findUsersByFirstnameForSpELExpressionWithParameterIndexOnlyWithEntityExpression("Joachim", "Arrasz");

		Assertions.assertThat(users).containsOnly(this.secondUser);
	}

	@Test
	public void findByEmptyCollectionOfStrings() {

		this.flushTestUsers();

		List<User> users = this.repository.findByAttributesIn(new HashSet<>());
		Assertions.assertThat(users).hasSize(0);
	}

	@Test
	public void findByEmptyCollectionOfIntegers() {

		this.flushTestUsers();

		List<User> users = this.repository.findByAgeIn(Collections.emptyList());
		Assertions.assertThat(users).hasSize(0);
	}

	@Test
	public void findByEmptyArrayOfIntegers() {

		this.flushTestUsers();

		List<User> users = this.repository.queryByAgeIn(new Integer[0]);
		Assertions.assertThat(users).hasSize(0);
	}

	@Test
	public void findByAgeWithEmptyArrayOfIntegersOrFirstName() {

		this.flushTestUsers();

		List<User> users = this.repository.queryByAgeInOrFirstname(new Integer[0], this.secondUser.getFirstname());
		Assertions.assertThat(users).containsOnly(this.secondUser);
	}

	@Test
	public void shouldSupportJava8StreamsForRepositoryFinderMethods() {

		this.flushTestUsers();

		try (Stream<User> stream = this.repository.findAllByCustomQueryAndStream()) {
			Assertions.assertThat(stream).hasSize(4);
		}
	}

	@Test
	public void shouldSupportJava8StreamsForRepositoryDerivedFinderMethods() {

		this.flushTestUsers();

		try (Stream<User> stream = this.repository.readAllByFirstnameNotNull()) {
			Assertions.assertThat(stream).hasSize(4);
		}
	}

	@Test
	public void supportsJava8StreamForPageableMethod() {

		this.flushTestUsers();

		try (Stream<User> stream = this.repository.streamAllPaged(PageRequest.of(0, 2))) {
			Assertions.assertThat(stream).hasSize(2);
		}
	}

	@Test
	public void findAllByExample() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setAge(28);
		prototype.setCreatedAt(null);

		List<User> users = this.repository.findAll(Example.of(prototype));

		Assertions.assertThat(users).hasSize(1);
		Assertions.assertThat(users.get(0)).isEqualTo(this.firstUser);
	}

	@Test
	public void findAllByExampleWithEmptyProbe() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setCreatedAt(null);

		List<User> users = this.repository.findAll(
				Example.of(prototype, ExampleMatcher.matching().withIgnorePaths("age", "createdAt", "active")));

		Assertions.assertThat(users).hasSize(4);
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void findAllByNullExample() {
		this.repository.findAll((Example<User>) null);
	}

	@Test
	public void findAllByExampleWithExcludedAttributes() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setAge(28);

		Example<User> example = Example.of(prototype, ExampleMatcher.matching().withIgnorePaths("createdAt"));
		List<User> users = this.repository.findAll(example);

		Assertions.assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	public void findAllByExampleWithAssociation() {

		this.flushTestUsers();

		this.firstUser.setManager(this.secondUser);
		this.thirdUser.setManager(this.firstUser);
		this.repository.saveAll(Arrays.asList(this.firstUser, this.thirdUser));

		User manager = new User();
		manager.setLastname("Arrasz");
		manager.setAge(this.secondUser.getAge());
		manager.setCreatedAt(null);

		User prototype = new User();
		prototype.setCreatedAt(null);
		prototype.setManager(manager);

		Example<User> example = Example.of(prototype, ExampleMatcher.matching().withIgnorePaths("age"));
		List<User> users = this.repository.findAll(example);

		Assertions.assertThat(users).hasSize(1);
		Assertions.assertThat(users.get(0)).isEqualTo(this.firstUser);
	}

	@Test
	public void findAllByExampleWithEmbedded() {

		this.flushTestUsers();

		this.firstUser.setAddress(new Address("germany", "dresden", "", ""));
		this.repository.save(this.firstUser);

		User prototype = new User();
		prototype.setCreatedAt(null);
		prototype.setAddress(new Address("germany", null, null, null));

		Example<User> example = Example.of(prototype, ExampleMatcher.matching().withIgnorePaths("age"));
		List<User> users = this.repository.findAll(example);

		Assertions.assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	public void findAllByExampleWithStartingStringMatcher() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("Ol");

		Example<User> example = Example.of(prototype, ExampleMatcher.matching()
				.withStringMatcher(StringMatcher.STARTING).withIgnorePaths("age", "createdAt"));
		List<User> users = this.repository.findAll(example);

		Assertions.assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	public void findAllByExampleWithEndingStringMatcher() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("ver");

		Example<User> example = Example.of(prototype,
				ExampleMatcher.matching().withStringMatcher(StringMatcher.ENDING).withIgnorePaths("age", "createdAt"));
		List<User> users = this.repository.findAll(example);

		Assertions.assertThat(users).containsOnly(this.firstUser);
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void findAllByExampleWithRegexStringMatcher() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("^Oliver$");

		Example<User> example = Example.of(prototype, ExampleMatcher.matching().withStringMatcher(StringMatcher.REGEX));
		this.repository.findAll(example);
	}

	@Test
	public void findAllByExampleWithIgnoreCase() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("oLiVer");

		Example<User> example = Example.of(prototype,
				ExampleMatcher.matching().withIgnoreCase().withIgnorePaths("age", "createdAt"));

		List<User> users = this.repository.findAll(example);

		Assertions.assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	public void findAllByExampleWithStringMatcherAndIgnoreCase() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("oLiV");

		Example<User> example = Example.of(prototype, ExampleMatcher.matching()
				.withStringMatcher(StringMatcher.STARTING).withIgnoreCase().withIgnorePaths("age", "createdAt"));

		List<User> users = this.repository.findAll(example);

		Assertions.assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	public void findAllByExampleWithIncludeNull() {

		this.flushTestUsers();

		this.firstUser.setAddress(new Address("andor", "caemlyn", "", ""));

		User fifthUser = new User();
		fifthUser.setEmailAddress("foo@bar.com");
		fifthUser.setActive(this.firstUser.isActive());
		fifthUser.setAge(this.firstUser.getAge());
		fifthUser.setFirstname(this.firstUser.getFirstname());
		fifthUser.setLastname(this.firstUser.getLastname());

		this.repository.saveAll(Arrays.asList(this.firstUser, fifthUser));

		User prototype = new User();
		prototype.setFirstname(this.firstUser.getFirstname());

		Example<User> example = Example.of(prototype, ExampleMatcher.matching().withIncludeNullValues()
				.withIgnorePaths("id", "binaryData", "lastname", "emailAddress", "age", "createdAt"));

		List<User> users = this.repository.findAll(example);

		Assertions.assertThat(users).containsOnly(fifthUser);
	}

	@Test
	public void findAllByExampleWithPropertySpecifier() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("oLi");

		Example<User> example = Example.of(prototype,
				ExampleMatcher.matching().withIgnoreCase().withIgnorePaths("age", "createdAt").withMatcher("firstname",
						new GenericPropertyMatcher().startsWith()));

		List<User> users = this.repository.findAll(example);

		Assertions.assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	public void findAllByExampleWithSort() {

		this.flushTestUsers();

		User user1 = new User("Oliver", "Srping", "o@s.de");
		user1.setAge(30);

		this.repository.save(user1);

		User prototype = new User();
		prototype.setFirstname("oLi");

		Example<User> example = Example.of(prototype, ExampleMatcher.matching().withIgnoreCase()
				.withIgnorePaths("age", "createdAt").withStringMatcher(StringMatcher.STARTING).withIgnoreCase());

		List<User> users = this.repository.findAll(example, Sort.by(Direction.DESC, "age"));

		Assertions.assertThat(users).hasSize(2).containsExactly(user1, this.firstUser);
	}

	@Test
	public void findAllByExampleWithPageable() {

		this.flushTestUsers();

		for (int i = 0; i < 99; i++) {
			User user1 = new User("Oliver-" + i, "Srping", "o" + i + "@s.de");
			user1.setAge(30 + i);

			this.repository.save(user1);
		}

		User prototype = new User();
		prototype.setFirstname("oLi");

		Example<User> example = Example.of(prototype, ExampleMatcher.matching().withIgnoreCase()
				.withIgnorePaths("age", "createdAt").withStringMatcher(StringMatcher.STARTING).withIgnoreCase());

		Page<User> users = this.repository.findAll(example, PageRequest.of(0, 10, Sort.by(Direction.DESC, "age")));

		Assertions.assertThat(users.getSize()).isEqualTo(10);
		Assertions.assertThat(users.hasNext()).isEqualTo(true);
		Assertions.assertThat(users.getTotalElements()).isEqualTo(100L);
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void findAllByExampleShouldNotAllowCycles() {

		this.flushTestUsers();

		User user1 = new User();
		user1.setFirstname("user1");

		user1.setManager(user1);

		Example<User> example = Example.of(user1, ExampleMatcher.matching().withIgnoreCase()
				.withIgnorePaths("age", "createdAt").withStringMatcher(StringMatcher.STARTING).withIgnoreCase());

		this.repository.findAll(example, PageRequest.of(0, 10, Sort.by(Direction.DESC, "age")));
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void findAllByExampleShouldNotAllowCyclesOverSeveralInstances() {

		this.flushTestUsers();

		User user1 = new User();
		user1.setFirstname("user1");

		User user2 = new User();
		user2.setFirstname("user2");

		user1.setManager(user2);
		user2.setManager(user1);

		Example<User> example = Example.of(user1, ExampleMatcher.matching().withIgnoreCase()
				.withIgnorePaths("age", "createdAt").withStringMatcher(StringMatcher.STARTING).withIgnoreCase());

		this.repository.findAll(example, PageRequest.of(0, 10, Sort.by(Direction.DESC, "age")));
	}

	@Test
	public void findOneByExampleWithExcludedAttributes() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setAge(28);

		Example<User> example = Example.of(prototype, ExampleMatcher.matching().withIgnorePaths("createdAt"));

		Assertions.assertThat(this.repository.findOne(example)).contains(this.firstUser);
	}

	@Test
	public void countByExampleWithExcludedAttributes() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setAge(28);

		Example<User> example = Example.of(prototype, ExampleMatcher.matching().withIgnorePaths("createdAt"));
		long count = this.repository.count(example);

		Assertions.assertThat(count).isEqualTo(1L);
	}

	@Test
	public void existsByExampleWithExcludedAttributes() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setAge(28);

		Example<User> example = Example.of(prototype, ExampleMatcher.matching().withIgnorePaths("createdAt"));
		boolean exists = this.repository.exists(example);

		Assertions.assertThat(exists).isEqualTo(true);
	}

	@Test
	public void dynamicProjectionReturningStream() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.findAsStreamByFirstnameLike("%O%", User.class)).hasSize(1);
	}

	@Test
	public void dynamicProjectionReturningList() {

		this.flushTestUsers();

		List<User> users = this.repository.findAsListByFirstnameLike("%O%", User.class);

		Assertions.assertThat(users).hasSize(1);
	}

	@Test
	public void duplicateSpelsWorkAsIntended() {

		this.flushTestUsers();

		List<User> users = this.repository.findUsersByDuplicateSpel("Oliver");

		Assertions.assertThat(users).hasSize(1);
	}

	@Test
	public void supportsProjectionsWithNativeQueries() {

		this.flushTestUsers();

		User user = this.repository.findAll().get(0);

		UserRepository.NameOnly result = this.repository.findByNativeQuery(user.getId());

		Assertions.assertThat(result.getFirstname()).isEqualTo(user.getFirstname());
		Assertions.assertThat(result.getLastname()).isEqualTo(user.getLastname());
	}

	@Test
	public void supportsProjectionsWithNativeQueriesAndCamelCaseProperty() {

		this.flushTestUsers();
		User user = this.repository.findAll().get(0);

		UserRepository.EmailOnly result = this.repository.findEmailOnlyByNativeQuery(user.getId());

		String emailAddress = result.getEmailAddress();

		Assertions.assertThat(emailAddress) //
				.isEqualTo(user.getEmailAddress()) //
				.as("ensuring email is actually not null") //
				.isNotNull();
	}

	@Test
	public void handlesColonsFollowedByIntegerInStringLiteral() {

		String firstName = "aFirstName";

		User expected = new User(firstName, "000:1", "something@something");
		User notExpected = new User(firstName, "000\\:1", "something@something.else");

		this.repository.save(expected);
		this.repository.save(notExpected);

		Assertions.assertThat(this.repository.findAll()).hasSize(2);

		List<User> users = this.repository.queryWithIndexedParameterAndColonFollowedByIntegerInString(firstName);

		Assertions.assertThat(users).extracting(User::getId).containsExactly(expected.getId());
	}

	@Test
	public void handlesCountQueriesWithLessParametersSingleParam() {
		this.repository.findAllOrderedBySpecialNameSingleParam("Oliver", PageRequest.of(2, 3));
	}

	@Test
	public void handlesCountQueriesWithLessParametersMoreThanOne() {
		this.repository.findAllOrderedBySpecialNameMultipleParams("Oliver", "x", PageRequest.of(2, 3));
	}

	@Test
	public void handlesCountQueriesWithLessParametersMoreThanOneIndexed() {
		this.repository.findAllOrderedBySpecialNameMultipleParamsIndexed("Oliver", "x", PageRequest.of(2, 3));
	}

	@Test
	public void executeNativeQueryWithPage() {

		this.flushTestUsers();

		Page<User> firstPage = this.repository.findByNativeNamedQueryWithPageable(PageRequest.of(0, 3));
		Page<User> secondPage = this.repository.findByNativeNamedQueryWithPageable(PageRequest.of(1, 3));

		SoftAssertions softly = new SoftAssertions();

		Assertions.assertThat(firstPage.getTotalElements()).isEqualTo(4L);
		Assertions.assertThat(firstPage.getNumberOfElements()).isEqualTo(3);
		Assertions.assertThat(firstPage.getContent()) //
				.extracting(User::getFirstname) //
				.containsExactly("Dave", "Joachim", "kevin");

		Assertions.assertThat(secondPage.getTotalElements()).isEqualTo(4L);
		Assertions.assertThat(secondPage.getNumberOfElements()).isEqualTo(1);
		Assertions.assertThat(secondPage.getContent()) //
				.extracting(User::getFirstname) //
				.containsExactly("Oliver");

		softly.assertAll();
	}

	@Test
	public void executeNativeQueryWithPageWorkaround() {

		this.flushTestUsers();

		Page<String> firstPage = this.repository.findByNativeQueryWithPageable(PageRequest.of(0, 3));
		Page<String> secondPage = this.repository.findByNativeQueryWithPageable(PageRequest.of(1, 3));

		SoftAssertions softly = new SoftAssertions();

		Assertions.assertThat(firstPage.getTotalElements()).isEqualTo(4L);
		Assertions.assertThat(firstPage.getNumberOfElements()).isEqualTo(3);
		Assertions.assertThat(firstPage.getContent()) //
				.containsExactly("Dave", "Joachim", "kevin");

		Assertions.assertThat(secondPage.getTotalElements()).isEqualTo(4L);
		Assertions.assertThat(secondPage.getNumberOfElements()).isEqualTo(1);
		Assertions.assertThat(secondPage.getContent()) //
				.containsExactly("Oliver");

		softly.assertAll();
	}

	@Test
	public void bindsNativeQueryResultsToProjectionByName() {

		this.flushTestUsers();

		List<UserRepository.NameOnly> result = this.repository.findByNamedQueryWithAliasInInvertedOrder();

		Assertions.assertThat(result).element(0).satisfies((it) -> {
			Assertions.assertThat(it.getFirstname()).isEqualTo("Joachim");
			Assertions.assertThat(it.getLastname()).isEqualTo("Arrasz");
		});
	}

	@Test
	public void returnsNullValueInMap() {

		this.firstUser.setLastname(null);
		this.flushTestUsers();

		Map<String, Object> map = this.repository.findMapWithNullValues();

		SoftAssertions softly = new SoftAssertions();

		softly.assertThat(map.keySet()).containsExactlyInAnyOrder("firstname", "lastname");

		softly.assertThat(map.containsKey("firstname")).isTrue();
		softly.assertThat(map.containsKey("lastname")).isTrue();

		softly.assertThat(map.get("firstname")).isEqualTo("Oliver");
		softly.assertThat(map.get("lastname")).isNull();

		softly.assertThat(map.get("non-existent")).isNull();

		softly.assertThat(map.get(new Object())).isNull();

		softly.assertAll();
	}

	@Test
	public void testFindByEmailAddressJdbcStyleParameter() {

		this.flushTestUsers();

		Assertions.assertThat(this.repository.findByEmailNativeAddressJdbcStyleParameter("gierke@synyx.de"))
				.isEqualTo(this.firstUser);
	}

	@Test
	public void savingUserThrowsAnException() {
		// if this test fails this means deleteNewInstanceSucceedsByDoingNothing() might
		// actually save the user without the
		// test failing, which would be a bad thing.
		Assertions.assertThatThrownBy(() -> this.repository.save(new User()))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	public void deleteNewInstanceSucceedsByDoingNothing() {
		this.repository.delete(new User());
	}

}
