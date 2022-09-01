/*
 * Copyright 2019-2022 the original author or authors.
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

package io.easybest.mybatis.repository;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import io.easybest.mybatis.domain.sample.Address;
import io.easybest.mybatis.domain.sample.Role;
import io.easybest.mybatis.domain.sample.SpecialUser;
import io.easybest.mybatis.domain.sample.User;
import io.easybest.mybatis.repository.query.criteria.Criteria;
import io.easybest.mybatis.repository.query.criteria.LambdaCriteria;
import io.easybest.mybatis.repository.sample.RoleRepository;
import io.easybest.mybatis.repository.sample.UserRepository;
import io.easybest.mybatis.repository.sample.UserRepository.NameOnly;
import lombok.Data;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mybatis.spring.MyBatisSystemException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.data.domain.Example.of;
import static org.springframework.data.domain.ExampleMatcher.matching;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;

/**
 * Base integration test class for {@code UserRepository}.
 * <p>
 * To test further persistence providers subclass this class and provide a custom provider
 * configuration.
 *
 * @author Jarvis Song
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:application-context.xml")
@Transactional
public class UserRepositoryTests {

	@Autowired
	UserRepository repository;

	@Autowired
	RoleRepository roleRepository;

	private User firstUser;

	private User secondUser;

	private User thirdUser;

	private User fourthUser;

	private Integer id;

	private Role adminRole;

	void flushTestUsers() {

		this.roleRepository.save(this.adminRole);

		this.firstUser = this.repository.save(this.firstUser);
		this.secondUser = this.repository.save(this.secondUser);
		this.thirdUser = this.repository.save(this.thirdUser);
		this.fourthUser = this.repository.save(this.fourthUser);

		this.id = this.firstUser.getId();

		assertThat(this.id).isNotNull();
		assertThat(this.secondUser.getId()).isNotNull();
		assertThat(this.thirdUser.getId()).isNotNull();
		assertThat(this.fourthUser.getId()).isNotNull();

		assertThat(this.repository.existsById(this.id)).isTrue();
		assertThat(this.repository.existsById(this.secondUser.getId())).isTrue();
		assertThat(this.repository.existsById(this.thirdUser.getId())).isTrue();
		assertThat(this.repository.existsById(this.fourthUser.getId())).isTrue();

	}

	@BeforeEach
	void setUp() throws Exception {

		this.firstUser = new User("Oliver", "Gierke", "gierke@synyx.de");
		this.firstUser.setAge(28);

		this.secondUser = new User("Joachim", "Arrasz", "arrasz@synyx.de");
		this.secondUser.setAge(35);
		this.secondUser.setCreatedAt(new Date(this.firstUser.getCreatedAt().getTime() + 5000L));

		// Thread.sleep(1000);
		this.thirdUser = new User("Dave", "Matthews", "no@email.com");
		this.thirdUser.setAge(43);
		this.thirdUser.setCreatedAt(new Date(this.secondUser.getCreatedAt().getTime() + 5000L));

		this.fourthUser = new User("kevin", "raymond", "no@gmail.com");
		this.fourthUser.setAge(31);
		this.fourthUser.setCreatedAt(new Date(this.thirdUser.getCreatedAt().getTime() + 5000L));

		this.adminRole = new Role("admin");

	}

	@Test
	void testCreation() {

		long before = this.repository.count();

		this.flushTestUsers();

		assertThat(this.repository.count()).isEqualTo(before + 4L);
	}

	@Test
	void testRead() {
		this.flushTestUsers();

		assertThat(this.repository.findById(this.id)).map(User::getFirstname).contains(this.firstUser.getFirstname());
	}

	@Test
	void findsAllByGivenIds() {

		this.flushTestUsers();

		assertThat(this.repository.findAllById(asList(this.firstUser.getId(), this.secondUser.getId()))) //
				.containsExactlyInAnyOrder(this.firstUser, this.secondUser);
	}

	@Test
	void testReadByIdReturnsNullForNotFoundEntities() {

		this.flushTestUsers();

		assertThat(this.repository.findById(this.id * 27)).isNotPresent();
	}

	@Test
	void savesCollectionCorrectly() {

		assertThat(this.repository.saveAll(asList(this.firstUser, this.secondUser, this.thirdUser))) //
				.containsExactlyInAnyOrder(this.firstUser, this.secondUser, this.thirdUser);
	}

	@Test
	void savingEmptyCollectionIsNoOp() {
		assertThat(this.repository.saveAll(new ArrayList<>())).isEmpty();
	}

	@Test
	void testUpdate() {

		this.flushTestUsers();

		User foundPerson = this.repository.findById(this.id).get();
		foundPerson.setLastname("Schlicht");
		this.repository.save(this.fourthUser);
		assertThat(this.repository.findById(this.id)).map(User::getFirstname).contains(foundPerson.getFirstname());
	}

	@Test
	void existReturnsWhetherAnEntityCanBeLoaded() {

		this.flushTestUsers();
		assertThat(this.repository.existsById(this.id)).isTrue();
		assertThat(this.repository.existsById(this.id * 27)).isFalse();
	}

	@Test
	void deletesAUserById() {

		this.flushTestUsers();

		this.repository.deleteById(this.firstUser.getId());

		assertThat(this.repository.existsById(this.id)).isFalse();
		assertThat(this.repository.findById(this.id)).isNotPresent();
	}

	@Test
	void testDelete() {

		this.flushTestUsers();

		this.repository.delete(this.firstUser);

		assertThat(this.repository.existsById(this.id)).isFalse();
		assertThat(this.repository.findById(this.id)).isNotPresent();
	}

	@Test
	void returnsAllSortedCorrectly() {

		this.flushTestUsers();

		assertThat(this.repository.findAll(Sort.by(ASC, "lastname"))).hasSize(4).containsExactly(this.secondUser,
				this.firstUser, this.thirdUser, this.fourthUser);
	}

	@Test
	void returnsAllIgnoreCaseSortedCorrectly() {

		this.flushTestUsers();

		Sort.Order order = new Sort.Order(ASC, "firstname").ignoreCase();

		assertThat(this.repository.findAll(Sort.by(order))) //
				.hasSize(4)//
				.containsExactly(this.thirdUser, this.secondUser, this.fourthUser, this.firstUser);
	}

	@Test
	void deleteCollectionOfEntities() {

		this.flushTestUsers();

		long before = this.repository.count();

		this.repository.deleteAll(asList(this.firstUser, this.secondUser));

		assertThat(this.repository.existsById(this.firstUser.getId())).isFalse();
		assertThat(this.repository.existsById(this.secondUser.getId())).isFalse();
		assertThat(this.repository.count()).isEqualTo(before - 2);
	}

	@Test
	void batchDeleteCollectionOfEntities() {

		this.flushTestUsers();

		long before = this.repository.count();

		this.repository.deleteAllInBatch(asList(this.firstUser, this.secondUser));

		assertThat(this.repository.existsById(this.firstUser.getId())).isFalse();
		assertThat(this.repository.existsById(this.secondUser.getId())).isFalse();
		assertThat(this.repository.count()).isEqualTo(before - 2);
	}

	@Test
	void deleteCollectionOfEntitiesById() {

		this.flushTestUsers();

		long before = this.repository.count();

		this.repository.deleteAllById(asList(this.firstUser.getId(), this.secondUser.getId()));

		assertThat(this.repository.existsById(this.firstUser.getId())).isFalse();
		assertThat(this.repository.existsById(this.secondUser.getId())).isFalse();
		assertThat(this.repository.count()).isEqualTo(before - 2);
	}

	@Test
	void deleteEmptyCollectionDoesNotDeleteAnything() {

		this.assertDeleteCallDoesNotDeleteAnything(new ArrayList<>());
	}

	private void assertDeleteCallDoesNotDeleteAnything(List<User> collection) {

		this.flushTestUsers();
		long count = this.repository.count();

		this.repository.deleteAll(collection);
		assertThat(this.repository.count()).isEqualTo(count);
	}

	@Test
	void executesManipulatingQuery() {

		this.flushTestUsers();
		this.repository.renameAllUsersTo("newLastname");

		long expected = this.repository.count();
		assertThat(this.repository.findByLastname("newLastname").size()).isEqualTo(Long.valueOf(expected).intValue());
	}

	@Test
	void testFinderInvocationWithNullParameter() {

		this.flushTestUsers();

		this.repository.findByLastname(null);
	}

	@Test
	void testFindByLastname() {

		this.flushTestUsers();

		assertThat(this.repository.findByLastname("Gierke")).containsOnly(this.firstUser);
	}

	@Test
	void testCriteria() {

		this.flushTestUsers();

		LambdaCriteria<User> criteria = Criteria.lambda(User.class).eq(User::getFirstname, "Oliver")
				.eq(User::getLastname, "Gierke");
		assertThat(this.repository.findAll(criteria)).containsOnly(this.firstUser);

		assertThat(this.repository
				.findAll(Criteria.create(User.class).eq("firstname", "Oliver").or().eq("lastname", "Gierke").or()
						.eq("lastname", "DDD").or(c -> c.eq("lastname", "XXX").ne("firstname", "CCC"))))
								.containsOnly(this.firstUser);

	}

	@Test
	void testFindByEmailAddress() {

		this.flushTestUsers();

		assertThat(this.repository.findByEmailAddress("gierke@synyx.de")).isEqualTo(this.firstUser);
	}

	@Test
	void testReadAll() {

		this.flushTestUsers();

		assertThat(this.repository.count()).isEqualTo(4L);
		assertThat(this.repository.findAll()).contains(this.firstUser, this.secondUser, this.thirdUser,
				this.fourthUser);
	}

	@Test
	void deleteAll() {

		this.flushTestUsers();

		this.repository.deleteAll();

		assertThat(this.repository.count()).isZero();
	}

	@Test
	void deleteAllInBatch() {

		this.flushTestUsers();

		this.repository.deleteAllInBatch();

		assertThat(this.repository.count()).isZero();
	}

	@Test
	void testCountsCorrectly() {

		long count = this.repository.count();

		User user = new User();
		user.setEmailAddress("gierke@synyx.de");
		this.repository.save(user);

		assertThat(this.repository.count()).isEqualTo(count + 1);
	}

	@Test
	void testInvocationOfCustomImplementation() {

		this.repository.someCustomMethod(new User());
	}

	@Test
	void testOverwritingFinder() {

		this.repository.findByOverrridingMethod();
	}

	@Test
	void testUsesQueryAnnotation() {

		assertThat(this.repository.findByAnnotatedQuery("gierke@synyx.de")).isNull();
	}

	@Test
	void testExecutionOfProjectingMethod() {

		this.flushTestUsers();
		assertThat(this.repository.countWithFirstname("Oliver")).isEqualTo(1L);
	}

	@Test
	void executesMethodWithAnnotatedNamedParametersCorrectly() {

		this.firstUser = this.repository.save(this.firstUser);
		this.secondUser = this.repository.save(this.secondUser);

		assertThat(this.repository.findByLastnameOrFirstname("Oliver", "Arrasz")).contains(this.firstUser,
				this.secondUser);
	}

	@Test
	void executesMethodWithNamedParametersCorrectlyOnMethodsWithQueryCreation() {

		this.firstUser = this.repository.save(this.firstUser);
		this.secondUser = this.repository.save(this.secondUser);

		assertThat(this.repository.findByFirstnameOrLastname("Oliver", "Arrasz")).containsOnly(this.firstUser,
				this.secondUser);
	}

	@Test
	void executesLikeAndOrderByCorrectly() {

		this.flushTestUsers();

		assertThat(this.repository.findByLastnameLikeOrderByFirstnameDesc("%r%")).hasSize(3)
				.containsExactly(this.fourthUser, this.firstUser, this.secondUser);
	}

	@Test
	void executesNotLikeCorrectly() {

		this.flushTestUsers();

		assertThat(this.repository.findByLastnameNotLike("%er%")).containsOnly(this.secondUser, this.thirdUser,
				this.fourthUser);
	}

	@Test
	void executesSimpleNotCorrectly() {

		this.flushTestUsers();

		assertThat(this.repository.findByLastnameNot("Gierke")).containsOnly(this.secondUser, this.thirdUser,
				this.fourthUser);
	}

	@Test
	void executesQueryMethodWithDeepTraversalCorrectly() {

		this.flushTestUsers();

		this.firstUser.setManager(this.secondUser);
		this.thirdUser.setManager(this.firstUser);
		this.repository.saveAll(asList(this.firstUser, this.thirdUser));

		assertThat(this.repository.findByManagerLastname("Arrasz")).containsOnly(this.firstUser);
		assertThat(this.repository.findByManagerLastname("Gierke")).containsOnly(this.thirdUser);
	}

	@Test
	void returnsSameListIfNoSortIsGiven() {

		this.flushTestUsers();
		assertSameElements(this.repository.findAll(Sort.unsorted()), this.repository.findAll());
	}

	private static <T> void assertSameElements(Collection<T> first, Collection<T> second) {

		for (T element : first) {
			assertThat(element).isIn(second);
		}

		for (T element : second) {
			assertThat(element).isIn(first);
		}
	}

	@Test
	void returnsAllAsPageIfNoPageableIsGiven() {

		this.flushTestUsers();
		assertThat(this.repository.findAll(Pageable.unpaged())).isEqualTo(new PageImpl<>(this.repository.findAll()));
	}

	@Test
	void executesFindByColleaguesLastnameCorrectly() {

		this.flushTestUsers();

		// this.firstUser.addColleague(this.secondUser);
		// this.thirdUser.addColleague(this.firstUser);
		// this.repository.saveCascadeAll(asList(this.firstUser, this.thirdUser));

		this.repository.saveColleagues(this.firstUser.getId(), this.secondUser.getId());
		this.repository.saveColleagues(this.thirdUser.getId(), this.firstUser.getId());

		assertThat(this.repository.findByColleaguesLastname(this.secondUser.getLastname()))
				.containsOnly(this.firstUser);

		assertThat(this.repository.findByColleaguesLastname("Gierke")).containsOnly(this.thirdUser, this.secondUser);
	}

	@Test
	void executesFindByNotNullLastnameCorrectly() {

		this.flushTestUsers();

		assertThat(this.repository.findByLastnameNotNull()).containsOnly(this.firstUser, this.secondUser,
				this.thirdUser, this.fourthUser);
	}

	void executesFindByNullLastnameCorrectly() {

		this.flushTestUsers();
		User forthUser = this.repository.save(new User("Foo", null, "email@address.com"));

		assertThat(this.repository.findByLastnameNull()).containsOnly(forthUser);
	}

	@Test
	void findsSortedByLastname() {

		this.flushTestUsers();

		assertThat(this.repository.findByEmailAddressLike("%@%", Sort.by(Sort.Direction.ASC, "lastname")))
				.containsExactly(this.secondUser, this.firstUser, this.thirdUser, this.fourthUser);
	}

	@Test
	void findsUsersBySpringDataNamedQuery() {

		this.flushTestUsers();

		assertThat(this.repository.findBySpringDataNamedQuery("Gierke")).containsOnly(this.firstUser);
	}

	@Test
	void readsPageWithGroupByClauseCorrectly() {

		this.flushTestUsers();

		Page<String> result = this.repository.findByLastnameGrouped(PageRequest.of(0, 10));
		assertThat(result.getTotalPages()).isEqualTo(1);
	}

	@Test
	void handlesIterableOfIdsCorrectly() {

		this.flushTestUsers();

		Set<Integer> set = new HashSet<>();
		set.add(this.firstUser.getId());
		set.add(this.secondUser.getId());

		assertThat(this.repository.findAllById(set)).containsOnly(this.firstUser, this.secondUser);
	}

	@Test
	void executesLessThatOrEqualQueriesCorrectly() {

		this.flushTestUsers();

		assertThat(this.repository.findByAgeLessThanEqual(35)).containsOnly(this.firstUser, this.secondUser,
				this.fourthUser);
	}

	@Test
	void executesGreaterThatOrEqualQueriesCorrectly() {

		this.flushTestUsers();

		assertThat(this.repository.findByAgeGreaterThanEqual(35)).containsOnly(this.secondUser, this.thirdUser);
	}

	@Test
	void executesFinderWithTrueKeywordCorrectly() {

		this.flushTestUsers();
		this.firstUser.setActive(false);
		this.repository.save(this.firstUser);

		assertThat(this.repository.findByActiveTrue()).containsOnly(this.secondUser, this.thirdUser, this.fourthUser);
	}

	@Test
	void executesFinderWithFalseKeywordCorrectly() {

		this.flushTestUsers();
		this.firstUser.setActive(false);
		this.repository.save(this.firstUser);

		assertThat(this.repository.findByActiveFalse()).containsOnly(this.firstUser);
	}

	// @Test
	// void executesAnnotatedCollectionMethodCorrectly() {
	//
	// this.flushTestUsers();
	// this.firstUser.addColleague(this.thirdUser);
	// this.repository.save(this.firstUser);
	//
	// List<User> result = this.repository.findColleaguesFor(this.firstUser);
	// assertThat(result).containsOnly(this.thirdUser);
	// }

	@Test
	void executesFinderWithAfterKeywordCorrectly() {

		this.flushTestUsers();

		assertThat(this.repository.findByCreatedAtAfter(this.secondUser.getCreatedAt())).containsOnly(this.thirdUser,
				this.fourthUser);
	}

	@Test
	void executesFinderWithBeforeKeywordCorrectly() {

		this.flushTestUsers();

		assertThat(this.repository.findByCreatedAtBefore(this.thirdUser.getCreatedAt())).containsOnly(this.firstUser,
				this.secondUser);
	}

	@Test
	void executesFinderWithStartingWithCorrectly() {

		this.flushTestUsers();

		assertThat(this.repository.findByFirstnameStartingWith("Oli")).containsOnly(this.firstUser);
	}

	@Test
	void executesFinderWithEndingWithCorrectly() {

		this.flushTestUsers();

		assertThat(this.repository.findByFirstnameEndingWith("er")).containsOnly(this.firstUser);
	}

	@Test
	void executesFinderWithContainingCorrectly() {

		this.flushTestUsers();

		assertThat(this.repository.findByFirstnameContaining("a")).containsOnly(this.secondUser, this.thirdUser);
	}

	@Test
	void allowsExecutingPageableMethodWithUnpagedArgument() {

		this.flushTestUsers();

		assertThat(this.repository.findByFirstname("Oliver", null)).containsOnly(this.firstUser);

		Page<User> page = this.repository.findByFirstnameIn(Pageable.unpaged(), "Oliver");
		assertThat(page.getNumberOfElements()).isEqualTo(1);
		assertThat(page.getContent()).contains(this.firstUser);

		page = this.repository.findAll(Pageable.unpaged());
		assertThat(page.getNumberOfElements()).isEqualTo(4);
		assertThat(page.getContent()).contains(this.firstUser, this.secondUser, this.thirdUser, this.fourthUser);
	}

	@Test
	void findAllReturnsEmptyIterableIfNoIdsGiven() {

		assertThat(this.repository.findAllById(Collections.emptySet())).isEmpty();
	}

	@Test
	void looksUpEntityReference() {

		this.flushTestUsers();

		User result = this.repository.getById(this.firstUser.getId());
		assertThat(result).isEqualTo(this.firstUser);
	}

	@Test
	void looksUpEntityReferenceUsingGetById() {

		this.flushTestUsers();

		User result = this.repository.getById(this.firstUser.getId());
		assertThat(result).isEqualTo(this.firstUser);

	}

	@Test
	void pageableQueryReportsTotalFromResult() {

		this.flushTestUsers();

		Page<User> firstPage = this.repository.findAll(PageRequest.of(0, 10));
		assertThat(firstPage.getContent()).hasSize(4);
		assertThat(firstPage.getTotalElements()).isEqualTo(4L);

		Page<User> secondPage = this.repository.findAll(PageRequest.of(1, 3));
		assertThat(secondPage.getContent()).hasSize(1);
		assertThat(secondPage.getTotalElements()).isEqualTo(4L);
	}

	@Test
	void pageableQueryReportsTotalFromCount() {

		this.flushTestUsers();

		Page<User> firstPage = this.repository.findAll(PageRequest.of(0, 4));
		assertThat(firstPage.getContent()).hasSize(4);
		assertThat(firstPage.getTotalElements()).isEqualTo(4L);

		Page<User> secondPage = this.repository.findAll(PageRequest.of(10, 10));
		assertThat(secondPage.getContent()).hasSize(0);
		assertThat(secondPage.getTotalElements()).isEqualTo(4L);
	}

	@Test
	void sortByEmbeddedProperty() {

		this.thirdUser.setAddress(new Address("Germany", "Saarbrücken", "HaveItYourWay", "123"));
		this.flushTestUsers();

		Page<User> page = this.repository
				.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "address.streetName")));

		assertThat(page.getContent()).hasSize(4);
		assertThat(page.getContent().get(3)).isEqualTo(this.thirdUser);
	}

	@Test
	void ordersByReferencedEntityCorrectly() {

		this.flushTestUsers();
		this.firstUser.setManager(this.thirdUser);
		this.repository.save(this.firstUser);

		Page<User> all = this.repository.findAll(PageRequest.of(0, 10, Sort.by("manager.id")));

		assertThat(all.getContent().isEmpty()).isFalse();
	}

	@Test
	void bindsSortingToOuterJoinCorrectly() {

		this.flushTestUsers();

		// Managers not set, make sure adding the sort does not rule out those Users
		Page<User> result = this.repository.findAllPaged(PageRequest.of(0, 10, Sort.by("manager.lastname")));
		assertThat(result.getContent()).hasSize((int) this.repository.count());
	}

	@Test
	void shouldGenerateLeftOuterJoinInFindAllWithPaginationAndSortOnNestedPropertyPath() {

		this.firstUser.setManager(null);
		this.secondUser.setManager(null);
		this.thirdUser.setManager(this.firstUser); // manager Oliver
		this.fourthUser.setManager(this.secondUser); // manager Joachim

		this.flushTestUsers();

		Page<User> pages = this.repository
				.findAll(PageRequest.of(0, 4, Sort.by(Sort.Direction.ASC, "manager.firstname")));
		assertThat(pages.getSize()).isEqualTo(4);
		assertThat(pages.getContent().get(0).getManager()).isNull();
		assertThat(pages.getContent().get(1).getManager()).isNull();
		assertThat(pages.getContent().get(2).getManager().getFirstname()).isEqualTo("Joachim");
		assertThat(pages.getContent().get(3).getManager().getFirstname()).isEqualTo("Oliver");
		assertThat(pages.getTotalElements()).isEqualTo(4L);
	}

	@Test
	void executesManualQueryWithPositionLikeExpressionCorrectly() {

		this.flushTestUsers();

		List<User> result = this.repository.findByFirstnameLike("Da");

		assertThat(result).containsOnly(this.thirdUser);
	}

	@Test
	void executesManualQueryWithNamedLikeExpressionCorrectly() {

		this.flushTestUsers();

		List<User> result = this.repository.findByFirstnameLikeNamed("Da");

		assertThat(result).containsOnly(this.thirdUser);
	}

	@Test
	void executesDerivedCountQueryToLong() {

		this.flushTestUsers();

		assertThat(this.repository.countByLastname("Matthews")).isEqualTo(1L);
	}

	@Test
	void executesDerivedCountQueryToInt() {

		this.flushTestUsers();

		assertThat(this.repository.countUsersByFirstname("Dave")).isEqualTo(1);
	}

	@Test
	void executesManuallyDefinedQueryWithFieldProjection() {

		this.flushTestUsers();
		List<String> lastname = this.repository.findFirstnamesByLastname("Matthews");

		assertThat(lastname).containsOnly("Dave");
	}

	@Test
	void executesDerivedExistsQuery() {

		this.flushTestUsers();

		assertThat(this.repository.existsByLastname("Matthews")).isEqualTo(true);
		assertThat(this.repository.existsByLastname("Hans Peter")).isEqualTo(false);
	}

	@Test
	void invokesQueryWithVarargsParametersCorrectly() {

		this.flushTestUsers();

		Collection<User> result = this.repository.findByIdIn(this.firstUser.getId(), this.secondUser.getId());

		assertThat(result).containsOnly(this.firstUser, this.secondUser);
	}

	@Test
	void shouldSupportModifyingQueryWithVarArgs() {

		this.flushTestUsers();

		this.repository.updateUserActiveState(false, this.firstUser.getId(), this.secondUser.getId(),
				this.thirdUser.getId(), this.fourthUser.getId());

		long expectedCount = this.repository.count();
		assertThat(this.repository.findByActiveFalse().size()).isEqualTo((int) expectedCount);
		assertThat(this.repository.findByActiveTrue().size()).isEqualTo(0);
	}

	@Test
	void executesFinderWithOrderClauseOnly() {

		this.flushTestUsers();

		assertThat(this.repository.findAllByOrderByLastnameAsc()).containsOnly(this.secondUser, this.firstUser,
				this.thirdUser, this.fourthUser);
	}

	@Test
	void sortByAssociationPropertyShouldUseLeftOuterJoin() {

		this.secondUser.getColleagues().add(this.firstUser);
		this.fourthUser.getColleagues().add(this.thirdUser);
		this.flushTestUsers();

		List<User> result = this.repository.findAll(Sort.by(Sort.Direction.ASC, "colleagues.id"));

		assertThat(result).hasSize(4);
	}

	@Test
	void sortByAssociationPropertyInPageableShouldUseLeftOuterJoin() {

		this.secondUser.getColleagues().add(this.firstUser);
		this.fourthUser.getColleagues().add(this.thirdUser);
		this.flushTestUsers();

		Page<User> page = this.repository.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "colleagues.id")));

		assertThat(page.getContent()).hasSize(4);
	}

	@Test
	void findsUserByBinaryDataReference() throws Exception {

		byte[] data = "Woho!!".getBytes(StandardCharsets.UTF_8);
		this.firstUser.setBinaryData(data);

		this.flushTestUsers();

		List<User> result = this.repository.findByBinaryData(data);
		assertThat(result).containsOnly(this.firstUser);
		assertThat(result.get(0).getBinaryData()).isEqualTo(data);
	}

	@Test
	void customFindByQueryWithPositionalVarargsParameters() {

		this.flushTestUsers();

		Collection<User> result = this.repository.findByIdsCustomWithPositionalVarArgs(this.firstUser.getId(),
				this.secondUser.getId());

		assertThat(result).containsOnly(this.firstUser, this.secondUser);
	}

	@Test
	void customFindByQueryWithNamedVarargsParameters() {

		this.flushTestUsers();

		Collection<User> result = this.repository.findByIdsCustomWithNamedVarArgs(this.firstUser.getId(),
				this.secondUser.getId());

		assertThat(result).containsOnly(this.firstUser, this.secondUser);
	}

	@Test
	void findAllByUntypedExampleShouldReturnSubTypesOfRepositoryEntity() {

		this.flushTestUsers();

		SpecialUser user = new SpecialUser();
		user.setFirstname("Thomas");
		user.setEmailAddress("thomas@example.org");

		this.repository.save(user);

		List<User> result = this.repository.findAll(
				Example.of(new User(), ExampleMatcher.matching().withIgnorePaths("age", "createdAt", "dateOfBirth")));

		assertThat(result).hasSize(5);
	}

	@Test
	void findAllByTypedUserExampleShouldReturnSubTypesOfRepositoryEntity() {

		this.flushTestUsers();

		SpecialUser user = new SpecialUser();
		user.setFirstname("Thomas");
		user.setEmailAddress("thomas@example.org");

		this.repository.save(user);

		Example<User> example = Example.of(new User(), matching().withIgnorePaths("age", "createdAt", "dateOfBirth"));
		List<User> result = this.repository.findAll(example);

		assertThat(result).hasSize(5);
	}

	// @Test
	// void findAllByTypedSpecialUserExampleShouldReturnSubTypesOfRepositoryEntity() {
	//
	// this.flushTestUsers();
	//
	// SpecialUser user = new SpecialUser();
	// user.setFirstname("Thomas");
	// user.setEmailAddress("thomas@example.org");
	//
	// this.repository.save(user);
	//
	// Example<SpecialUser> example = Example.of(new SpecialUser(),
	// matching().withIgnorePaths("age", "createdAt", "dateOfBirth"));
	// List<SpecialUser> result = this.repository.findAll(example);
	//
	// assertThat(result).hasSize(1);
	// }

	@Test
	void sortByNestedAssociationPropertyWithSortInPageable() {

		this.flushTestUsers();

		this.firstUser.setManager(this.thirdUser);
		this.thirdUser.setManager(this.fourthUser);

		this.flushTestUsers();

		Page<User> page = this.repository.findAll(PageRequest.of(0, 10, //
				Sort.by(Sort.Direction.ASC, "manager.manager.firstname")));

		assertThat(page.getContent()).hasSize(4);
		assertThat(page.getContent().get(3)).isEqualTo(this.firstUser);
	}

	@Test
	void sortByNestedAssociationPropertyWithSortOrderIgnoreCaseInPageable() {

		this.flushTestUsers();

		this.firstUser.setManager(this.thirdUser);
		this.thirdUser.setManager(this.fourthUser);

		this.flushTestUsers();

		Page<User> page = this.repository.findAll(PageRequest.of(0, 10, //
				Sort.by(new Sort.Order(Sort.Direction.ASC, "manager.manager.firstname").ignoreCase())));

		assertThat(page.getContent()).hasSize(4);
		assertThat(page.getContent().get(3)).isEqualTo(this.firstUser);
	}

	@Test
	void deleteByShouldReturnListOfDeletedElementsWhenRetunTypeIsCollectionLike() {

		this.flushTestUsers();

		List<User> result = this.repository.deleteByLastname(this.firstUser.getLastname());
		assertThat(result).containsOnly(this.firstUser);
	}

	@Test
	void deleteByShouldRemoveElementsMatchingDerivedQuery() {

		this.flushTestUsers();

		this.repository.deleteByLastname(this.firstUser.getLastname());
		assertThat(this.repository.countByLastname(this.firstUser.getLastname())).isEqualTo(0L);
	}

	@Test
	void deleteByShouldReturnNumberOfEntitiesRemovedIfReturnTypeIsLong() {

		this.flushTestUsers();

		assertThat(this.repository.removeByLastname(this.firstUser.getLastname())).isEqualTo(1L);
	}

	@Test
	void deleteByShouldReturnZeroInCaseNoEntityHasBeenRemovedAndReturnTypeIsNumber() {

		this.flushTestUsers();

		assertThat(this.repository.removeByLastname("bubu")).isEqualTo(0L);
	}

	@Test
	void deleteByShouldReturnEmptyListInCaseNoEntityHasBeenRemovedAndReturnTypeIsCollectionLike() {

		this.flushTestUsers();

		assertThat(this.repository.deleteByLastname("dorfuaeB")).isEmpty();
	}

	@Test
	void findBinaryDataByIdNative() {

		byte[] data = "Woho!!".getBytes(StandardCharsets.UTF_8);
		this.firstUser.setBinaryData(data);

		this.flushTestUsers();

		byte[] result = this.repository.findBinaryDataByIdNative(this.firstUser.getId());

		assertThat(result).isEqualTo(data);
		assertThat(result.length).isEqualTo(data.length);
	}

	@Test
	void findPaginatedExplicitQueryWithCountQueryProjection() {

		this.firstUser.setFirstname(null);

		this.flushTestUsers();

		Page<User> result = this.repository.findAllByFirstnameLike("", PageRequest.of(0, 10));

		assertThat(result.getContent().size()).isEqualTo(3);
	}

	@Test
	void findPaginatedNamedQueryWithCountQueryProjection() {

		this.flushTestUsers();

		Page<User> result = this.repository.findByNamedQueryAndCountProjection("Gierke", PageRequest.of(0, 10));

		assertThat(result.getContent().size()).isEqualTo(1);
	}

	@Test
	void findOldestUser() {

		this.flushTestUsers();

		User oldest = this.thirdUser;

		assertThat(this.repository.findFirstByOrderByAgeDesc()).isEqualTo(oldest);
		assertThat(this.repository.findFirst1ByOrderByAgeDesc()).isEqualTo(oldest);
	}

	@Test
	void findYoungestUser() {

		this.flushTestUsers();

		User youngest = this.firstUser;

		assertThat(this.repository.findTopByOrderByAgeAsc()).isEqualTo(youngest);
		assertThat(this.repository.findTop1ByOrderByAgeAsc()).isEqualTo(youngest);
	}

	@Test
	void find2OldestUsers() {

		this.flushTestUsers();

		User oldest1 = this.thirdUser;
		User oldest2 = this.secondUser;

		assertThat(this.repository.findFirst2ByOrderByAgeDesc()).contains(oldest1, oldest2);
		assertThat(this.repository.findTop2ByOrderByAgeDesc()).contains(oldest1, oldest2);
	}

	@Test
	void find2YoungestUsers() {

		this.flushTestUsers();

		User youngest1 = this.firstUser;
		User youngest2 = this.fourthUser;

		assertThat(this.repository.findFirst2UsersBy(Sort.by(ASC, "age"))).contains(youngest1, youngest2);
		assertThat(this.repository.findTop2UsersBy(Sort.by(ASC, "age"))).contains(youngest1, youngest2);
	}

	@Test
	void find3YoungestUsersPageableWithPageSize2() {

		this.flushTestUsers();

		User youngest1 = this.firstUser;
		User youngest2 = this.fourthUser;
		User youngest3 = this.secondUser;

		Page<User> firstPage = this.repository.findFirst3UsersBy(PageRequest.of(0, 2, ASC, "age"));
		assertThat(firstPage.getContent()).contains(youngest1, youngest2);

		Page<User> secondPage = this.repository.findFirst3UsersBy(PageRequest.of(1, 2, ASC, "age"));
		assertThat(secondPage.getContent()).contains(youngest3);
	}

	@Test
	void find2YoungestUsersPageableWithPageSize3() {

		this.flushTestUsers();

		User youngest1 = this.firstUser;
		User youngest2 = this.fourthUser;
		User youngest3 = this.secondUser;

		Page<User> firstPage = this.repository.findFirst2UsersBy(PageRequest.of(0, 3, ASC, "age"));
		assertThat(firstPage.getContent()).contains(youngest1, youngest2);

		Page<User> secondPage = this.repository.findFirst2UsersBy(PageRequest.of(1, 3, ASC, "age"));
		assertThat(secondPage.getContent()).contains(youngest3);
	}

	@Test
	void find3YoungestUsersPageableWithPageSize2Sliced() {

		this.flushTestUsers();

		User youngest1 = this.firstUser;
		User youngest2 = this.fourthUser;
		User youngest3 = this.secondUser;

		Slice<User> firstPage = this.repository.findTop3UsersBy(PageRequest.of(0, 2, ASC, "age"));
		assertThat(firstPage.getContent()).contains(youngest1, youngest2);

		Slice<User> secondPage = this.repository.findTop3UsersBy(PageRequest.of(1, 2, ASC, "age"));
		assertThat(secondPage.getContent()).contains(youngest3);
	}

	@Test
	void find2YoungestUsersPageableWithPageSize3Sliced() {

		this.flushTestUsers();

		User youngest1 = this.firstUser;
		User youngest2 = this.fourthUser;
		User youngest3 = this.secondUser;

		Slice<User> firstPage = this.repository.findTop2UsersBy(PageRequest.of(0, 3, ASC, "age"));
		assertThat(firstPage.getContent()).contains(youngest1, youngest2);

		Slice<User> secondPage = this.repository.findTop2UsersBy(PageRequest.of(1, 3, ASC, "age"));
		assertThat(secondPage.getContent()).contains(youngest3);
	}

	@Test
	void invokesQueryWithWrapperType() {

		this.flushTestUsers();

		Optional<User> result = this.repository.findOptionalByEmailAddress("gierke@synyx.de");

		assertThat(result.isPresent()).isEqualTo(true);
		assertThat(result.get()).isEqualTo(this.firstUser);
	}

	@Test
	void shouldFindUserByFirstnameAndLastnameWithSpelExpressionInStringBasedQuery() {

		this.flushTestUsers();
		List<User> users = this.repository.findByFirstnameAndLastnameWithSpelExpression("Oliver", "ierk");

		assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	void shouldFindUserByLastnameWithSpelExpressionInStringBasedQuery() {

		this.flushTestUsers();
		List<User> users = this.repository.findByLastnameWithSpelExpression("ierk");

		assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	void shouldFindBySpELExpressionWithoutArgumentsWithQuestionmark() {

		this.flushTestUsers();
		List<User> users = this.repository.findOliverBySpELExpressionWithoutArgumentsWithQuestionmark();

		assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	void shouldFindBySpELExpressionWithoutArgumentsWithColon() {

		this.flushTestUsers();
		List<User> users = this.repository.findOliverBySpELExpressionWithoutArgumentsWithColon();

		assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	void shouldFindUsersByAgeForSpELExpression() {

		this.flushTestUsers();
		List<User> users = this.repository.findUsersByAgeForSpELExpressionByIndexedParameter(35);

		assertThat(users).containsOnly(this.secondUser);
	}

	@Test
	void shouldfindUsersByFirstnameForSpELExpressionWithParameterNameVariableReference() {

		this.flushTestUsers();
		List<User> users = this.repository.findUsersByFirstnameForSpELExpression("Joachim");

		assertThat(users).containsOnly(this.secondUser);
	}

	@Test
	void shouldfindUsersByFirstnameForSpELExpressionOnlyWithParameterNameVariableReference() {

		this.flushTestUsers();
		List<User> users = this.repository.findUsersByFirstnameForSpELExpressionWithParameterVariableOnly("Joachim");

		assertThat(users).containsOnly(this.secondUser);
	}

	@Test
	void shouldfindUsersByFirstnameForSpELExpressionOnlyWithParameterIndexReference() {

		this.flushTestUsers();
		List<User> users = this.repository.findUsersByFirstnameForSpELExpressionWithParameterIndexOnly("Joachim");

		assertThat(users).containsOnly(this.secondUser);
	}

	@Test
	void shouldFindUsersInNativeQueryWithPagination() {

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
	void shouldFindUsersByUserFirstnameAsSpELExpressionAndLastnameAsStringInStringBasedQuery() {

		this.flushTestUsers();

		List<User> users = this.repository.findUsersByUserFirstnameAsSpELExpressionAndLastnameAsString(this.firstUser,
				this.firstUser.getLastname());

		assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	void shouldFindUsersByFirstnameAsStringAndUserLastnameAsSpELExpressionInStringBasedQuery() {

		this.flushTestUsers();

		List<User> users = this.repository.findUsersByFirstnameAsStringAndUserLastnameAsSpELExpression(
				this.firstUser.getFirstname(), this.firstUser);

		assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	void shouldFindUsersByUserFirstnameAsSpELExpressionAndLastnameAsFakeSpELExpressionInStringBasedQuery() {

		this.flushTestUsers();

		List<User> users = this.repository.findUsersByUserFirstnameAsSpELExpressionAndLastnameAsFakeSpELExpression(
				this.firstUser, this.firstUser.getLastname());

		assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	void shouldFindUsersByFirstnameAsFakeSpELExpressionAndUserLastnameAsSpELExpressionInStringBasedQuery() {

		this.flushTestUsers();

		List<User> users = this.repository.findUsersByFirstnameAsFakeSpELExpressionAndUserLastnameAsSpELExpression(
				this.firstUser.getFirstname(), this.firstUser);

		assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	void shouldFindUsersByFirstnameWithLeadingPageableParameter() {

		this.flushTestUsers();

		List<User> users = this.repository.findUsersByFirstnamePaginated(PageRequest.of(0, 2),
				this.firstUser.getFirstname());

		assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	void shouldFindUsersBySpELExpressionParametersWithSpelTemplateExpression() {

		this.flushTestUsers();
		List<User> users = this.repository
				.findUsersByFirstnameForSpELExpressionWithParameterIndexOnlyWithEntityExpression("Joachim", "Arrasz");

		assertThat(users).containsOnly(this.secondUser);
	}

	@Test
	void findByEmptyCollectionOfIntegers() {

		this.flushTestUsers();

		List<User> users = this.repository.findByAgeIn(Collections.emptyList());
		assertThat(users).hasSize(0);
	}

	@Test
	void findByCollectionWithPageable() {

		this.flushTestUsers();

		Page<User> userPage = this.repository.findByAgeIn(Arrays.asList(28, 35), (Pageable) PageRequest.of(0, 2));

		assertThat(userPage).hasSize(2);
		assertThat(userPage.getTotalElements()).isEqualTo(2);
		assertThat(userPage.getTotalPages()).isEqualTo(1);
		assertThat(userPage.getContent()).containsExactlyInAnyOrder(this.firstUser, this.secondUser);
	}

	@Test
	void findByCollectionWithPageRequest() {

		this.flushTestUsers();

		Page<User> userPage = this.repository.findByAgeIn(Arrays.asList(28, 35), PageRequest.of(0, 2));

		assertThat(userPage).hasSize(2);
		assertThat(userPage.getTotalElements()).isEqualTo(2);
		assertThat(userPage.getTotalPages()).isEqualTo(1);
		assertThat(userPage.getContent()).containsExactlyInAnyOrder(this.firstUser, this.secondUser);
	}

	@Test
	void findByEmptyArrayOfIntegers() {

		this.flushTestUsers();

		List<User> users = this.repository.queryByAgeIn(new Integer[0]);
		assertThat(users).hasSize(0);
	}

	@Test
	void findByAgeWithEmptyArrayOfIntegersOrFirstName() {

		this.flushTestUsers();

		List<User> users = this.repository.queryByAgeInOrFirstname(new Integer[0], this.secondUser.getFirstname());
		assertThat(users).containsOnly(this.secondUser);
	}

	@Test
	void shouldSupportJava8StreamsForRepositoryFinderMethods() {

		this.flushTestUsers();

		try (Stream<User> stream = this.repository.findAllByCustomQueryAndStream()) {
			assertThat(stream).hasSize(4);
		}
	}

	@Test
	void shouldSupportJava8StreamsForRepositoryDerivedFinderMethods() {

		this.flushTestUsers();

		try (Stream<User> stream = this.repository.readAllByFirstnameNotNull()) {
			assertThat(stream).hasSize(4);
		}
	}

	@Test
	void supportsJava8StreamForPageableMethod() {

		this.flushTestUsers();

		try (Stream<User> stream = this.repository.streamAllPaged(PageRequest.of(0, 2))) {
			assertThat(stream).hasSize(2);
		}
	}

	@Test
	void findAllByExample() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setAge(28);
		prototype.setCreatedAt(null);

		List<User> users = this.repository.findAll(Example.of(prototype));

		assertThat(users).hasSize(1);
		assertThat(users.get(0)).isEqualTo(this.firstUser);
	}

	@Test
	void findAllByExampleWithEmptyProbe() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setCreatedAt(null);

		List<User> users = this.repository
				.findAll(of(prototype, ExampleMatcher.matching().withIgnorePaths("age", "createdAt", "active")));

		assertThat(users).hasSize(4);
	}

	@Test
	void findAllByNullExample() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.repository.findAll((Example<User>) null));
	}

	@Test
	void findAllByExampleWithExcludedAttributes() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setAge(28);

		Example<User> example = Example.of(prototype, matching().withIgnorePaths("createdAt"));
		List<User> users = this.repository.findAll(example);

		assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	void findAllByExampleWithAssociation() {

		this.flushTestUsers();

		this.firstUser.setManager(this.secondUser);
		this.thirdUser.setManager(this.firstUser);
		this.repository.saveAll(asList(this.firstUser, this.thirdUser));

		User manager = new User();
		manager.setLastname("Arrasz");
		manager.setAge(this.secondUser.getAge());
		manager.setCreatedAt(null);

		User prototype = new User();
		prototype.setCreatedAt(null);
		prototype.setManager(manager);

		Example<User> example = Example.of(prototype, matching().withIgnorePaths("age"));
		List<User> users = this.repository.findAll(example);

		assertThat(users).hasSize(1);
		assertThat(users.get(0)).isEqualTo(this.firstUser);
	}

	@Test
	void findAllByExampleWithEmbedded() {

		this.flushTestUsers();

		this.firstUser.setAddress(new Address("germany", "dresden", "", ""));
		this.repository.save(this.firstUser);

		User prototype = new User();
		prototype.setCreatedAt(null);
		prototype.setAddress(new Address("germany", null, null, null));

		Example<User> example = Example.of(prototype, matching().withIgnorePaths("age"));
		List<User> users = this.repository.findAll(example);

		assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	void findAllByExampleWithStartingStringMatcher() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("Ol");

		Example<User> example = Example.of(prototype, matching()
				.withStringMatcher(ExampleMatcher.StringMatcher.STARTING).withIgnorePaths("age", "createdAt"));
		List<User> users = this.repository.findAll(example);

		assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	void findAllByExampleWithEndingStringMatcher() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("ver");

		Example<User> example = Example.of(prototype,
				matching().withStringMatcher(ExampleMatcher.StringMatcher.ENDING).withIgnorePaths("age", "createdAt"));
		List<User> users = this.repository.findAll(example);

		assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	void findAllByExampleWithRegexStringMatcher() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("^Oliver$");

		Example<User> example = Example.of(prototype,
				matching().withStringMatcher(ExampleMatcher.StringMatcher.REGEX).withIgnorePaths("age", "createdAt"));
		List<User> users = this.repository.findAll(example);

		assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	void findAllByExampleWithIgnoreCase() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("oLiVer");

		Example<User> example = Example.of(prototype, matching().withIgnoreCase().withIgnorePaths("age", "createdAt"));

		List<User> users = this.repository.findAll(example);

		assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	void findAllByExampleWithStringMatcherAndIgnoreCase() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("oLiV");

		Example<User> example = Example.of(prototype,
				matching().withStringMatcher(ExampleMatcher.StringMatcher.STARTING).withIgnoreCase()
						.withIgnorePaths("age", "createdAt"));

		List<User> users = this.repository.findAll(example);

		assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	void findAllByExampleWithIncludeNull() {

		this.flushTestUsers();

		this.firstUser.setAddress(new Address("andor", "caemlyn", "", ""));

		User fifthUser = new User();
		fifthUser.setEmailAddress("foo@bar.com");
		fifthUser.setActive(this.firstUser.isActive());
		fifthUser.setAge(this.firstUser.getAge());
		fifthUser.setFirstname(this.firstUser.getFirstname());
		fifthUser.setLastname(this.firstUser.getLastname());

		this.repository.saveAll(asList(this.firstUser, fifthUser));

		User prototype = new User();
		prototype.setFirstname(this.firstUser.getFirstname());

		Example<User> example = Example.of(prototype, matching().withIncludeNullValues().withIgnorePaths("id",
				"binaryData", "lastname", "emailAddress", "age", "createdAt"));

		List<User> users = this.repository.findAll(example);

		assertThat(users).containsOnly(fifthUser);
	}

	@Test
	void findAllByExampleWithPropertySpecifier() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("oLi");

		Example<User> example = Example.of(prototype, matching().withIgnoreCase().withIgnorePaths("age", "createdAt")
				.withMatcher("firstname", new ExampleMatcher.GenericPropertyMatcher().startsWith()));

		List<User> users = this.repository.findAll(example);

		assertThat(users).containsOnly(this.firstUser);
	}

	@Test
	void findAllByExampleWithSort() {

		this.flushTestUsers();

		User user1 = new User("Oliver", "Spring", "o@s.de");
		user1.setAge(30);

		this.repository.save(user1);

		User prototype = new User();
		prototype.setFirstname("oLi");

		Example<User> example = Example.of(prototype, matching().withIgnoreCase().withIgnorePaths("age", "createdAt")
				.withStringMatcher(ExampleMatcher.StringMatcher.STARTING).withIgnoreCase());

		List<User> users = this.repository.findAll(example, Sort.by(DESC, "age"));

		assertThat(users).hasSize(2).containsExactly(user1, this.firstUser);
	}

	@Test
	void findAllByExampleWithPageable() {

		this.flushTestUsers();

		for (int i = 0; i < 99; i++) {
			User user1 = new User("Oliver-" + i, "Srping", "o" + i + "@s.de");
			user1.setAge(30 + i);

			this.repository.save(user1);
		}

		User prototype = new User();
		prototype.setFirstname("oLi");

		Example<User> example = Example.of(prototype, matching().withIgnoreCase().withIgnorePaths("age", "createdAt")
				.withStringMatcher(ExampleMatcher.StringMatcher.STARTING).withIgnoreCase());

		Page<User> users = this.repository.findAll(example, PageRequest.of(0, 10, Sort.by(DESC, "age")));

		assertThat(users.getSize()).isEqualTo(10);
		assertThat(users.hasNext()).isEqualTo(true);
		assertThat(users.getTotalElements()).isEqualTo(100L);
	}

	@Test
	void findAllByExampleShouldNotAllowCycles() {

		this.flushTestUsers();

		User user1 = new User();
		user1.setFirstname("user1");

		user1.setManager(user1);

		Example<User> example = Example.of(user1, matching().withIgnoreCase().withIgnorePaths("age", "createdAt")
				.withStringMatcher(ExampleMatcher.StringMatcher.STARTING).withIgnoreCase());

		assertThatExceptionOfType(MyBatisSystemException.class)
				.isThrownBy(() -> this.repository.findAll(example, PageRequest.of(0, 10, Sort.by(DESC, "age"))));
	}

	@Test
	void findAllByExampleShouldNotAllowCyclesOverSeveralInstances() {

		this.flushTestUsers();

		User user1 = new User();
		user1.setFirstname("user1");

		User user2 = new User();
		user2.setFirstname("user2");

		user1.setManager(user2);
		user2.setManager(user1);

		Example<User> example = Example.of(user1, matching().withIgnoreCase().withIgnorePaths("age", "createdAt")
				.withStringMatcher(ExampleMatcher.StringMatcher.STARTING).withIgnoreCase());

		assertThatExceptionOfType(MyBatisSystemException.class)
				.isThrownBy(() -> this.repository.findAll(example, PageRequest.of(0, 10, Sort.by(DESC, "age"))));
	}

	@Test
	void findOneByExampleWithExcludedAttributes() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setAge(28);

		Example<User> example = Example.of(prototype, matching().withIgnorePaths("createdAt"));

		assertThat(this.repository.findOne(example)).contains(this.firstUser);
	}

	@Test
	void findByFluentExampleWithSorting() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("v");

		List<User> users = this.repository.findBy(
				of(prototype,
						matching().withIgnorePaths("age", "createdAt", "active").withMatcher("firstname",
								ExampleMatcher.GenericPropertyMatcher::contains)), //
				q -> q.sortBy(Sort.by("firstname")).all());

		assertThat(users).containsExactly(this.thirdUser, this.firstUser, this.fourthUser);
	}

	@Test
	void findByFluentExampleFirstValue() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("v");

		User firstUser = this.repository.findBy(
				of(prototype,
						matching().withIgnorePaths("age", "createdAt", "active").withMatcher("firstname",
								ExampleMatcher.GenericPropertyMatcher::contains)), //
				q -> q.sortBy(Sort.by("firstname")).firstValue());

		assertThat(firstUser).isEqualTo(this.thirdUser);
	}

	@Test
	void findByFluentExampleOneValue() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("v");

		assertThatExceptionOfType(MyBatisSystemException.class)
				.isThrownBy(
						() -> this.repository.findBy(
								of(prototype,
										matching().withIgnorePaths("age", "createdAt", "active").withMatcher(
												"firstname", ExampleMatcher.GenericPropertyMatcher::contains)), //
								q -> q.sortBy(Sort.by("firstname")).oneValue()));
	}

	@Test
	void findByFluentExampleStream() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("v");

		Stream<User> userStream = this.repository.findBy(
				of(prototype,
						matching().withIgnorePaths("age", "createdAt", "active").withMatcher("firstname",
								ExampleMatcher.GenericPropertyMatcher::contains)), //
				q -> q.sortBy(Sort.by("firstname")).stream());

		assertThat(userStream).containsExactly(this.thirdUser, this.firstUser, this.fourthUser);
	}

	@Test
	void findByFluentExamplePage() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("v");

		Example<User> userProbe = of(prototype, matching().withIgnorePaths("age", "createdAt", "active")
				.withMatcher("firstname", ExampleMatcher.GenericPropertyMatcher::contains));

		Page<User> page0 = this.repository.findBy(userProbe, //
				q -> q.sortBy(Sort.by("firstname")).page(PageRequest.of(0, 2)));

		Page<User> page1 = this.repository.findBy(userProbe, //
				q -> q.sortBy(Sort.by("firstname")).page(PageRequest.of(1, 2)));

		assertThat(page0.getContent()).containsExactly(this.thirdUser, this.firstUser);
		assertThat(page1.getContent()).containsExactly(this.fourthUser);
	}

	@Test
	void findByFluentExampleWithInterfaceBasedProjection() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("v");

		List<UserProjectionInterfaceBased> users = this.repository.findBy(
				of(prototype,
						matching().withIgnorePaths("age", "createdAt", "active").withMatcher("firstname",
								ExampleMatcher.GenericPropertyMatcher::contains)), //
				q -> q.as(UserProjectionInterfaceBased.class).all());

		assertThat(users).extracting(UserProjectionInterfaceBased::getFirstname).containsExactlyInAnyOrder(
				this.firstUser.getFirstname(), this.thirdUser.getFirstname(), this.fourthUser.getFirstname());
	}

	// @Test
	// void findByFluentExampleWithSimplePropertyPathsDoesntLoadUnrequestedPaths() {
	//
	// flushTestUsers();
	//
	//
	// User prototype = new User();
	// prototype.setFirstname("v");
	//
	// List<User> users = repository.findBy(
	// of(prototype,
	// matching().withIgnorePaths("age", "createdAt", "active").withMatcher("firstname",
	// ExampleMatcher.GenericPropertyMatcher::contains)), //
	// q -> q.project("firstname").all());
	//
	//
	//
	// assertThat(users).extracting(User::getFirstname).containsExactlyInAnyOrder(firstUser.getFirstname(),
	// thirdUser.getFirstname(), fourthUser.getFirstname());
	//
	// assertThatExceptionOfType(LazyInitializationException.class) //
	// .isThrownBy( //
	// () -> users.forEach(u -> u.getRoles().size()) // forces loading of roles
	// );
	// }

	@Test
	void findByFluentExampleWithCollectionPropertyPathsDoesntLoadUnrequestedPaths() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("v");

		List<User> users = this.repository.findBy(
				of(prototype,
						matching().withIgnorePaths("age", "createdAt", "active").withMatcher("firstname",
								ExampleMatcher.GenericPropertyMatcher::contains)), //
				q -> q.project("firstname", "roles").all());

		assertThat(users).extracting(User::getFirstname).containsExactlyInAnyOrder(this.firstUser.getFirstname(),
				this.thirdUser.getFirstname(), this.fourthUser.getFirstname());

		assertThat(users).allMatch(u -> u.getRoles().isEmpty());
	}

	@Test
	void findByFluentExampleWithComplexPropertyPathsDoesntLoadUnrequestedPaths() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("v");

		List<User> users = this.repository.findBy(
				of(prototype,
						matching().withIgnorePaths("age", "createdAt", "active").withMatcher("firstname",
								ExampleMatcher.GenericPropertyMatcher::contains)), //
				q -> q.project("roles.name").all());

		assertThat(users).extracting(User::getFirstname).containsExactlyInAnyOrder(this.firstUser.getFirstname(),
				this.thirdUser.getFirstname(), this.fourthUser.getFirstname());

		assertThat(users).allMatch(u -> u.getRoles().isEmpty());
	}

	@Test
	void findByFluentExampleWithSortedInterfaceBasedProjection() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("v");

		List<UserProjectionInterfaceBased> users = this.repository.findBy(
				of(prototype,
						matching().withIgnorePaths("age", "createdAt", "active").withMatcher("firstname",
								ExampleMatcher.GenericPropertyMatcher::contains)), //
				q -> q.as(UserProjectionInterfaceBased.class).sortBy(Sort.by("firstname")).all());

		assertThat(users).extracting(UserProjectionInterfaceBased::getFirstname).containsExactlyInAnyOrder(
				this.thirdUser.getFirstname(), this.firstUser.getFirstname(), this.fourthUser.getFirstname());
	}

	@Test
	void fluentExamplesWithClassBasedDtosNotYetSupported() {

		@Data
		class UserDto {

			String firstname;

		}

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> {

			User prototype = new User();
			prototype.setFirstname("v");

			this.repository.findBy(
					of(prototype,
							matching().withIgnorePaths("age", "createdAt", "active").withMatcher("firstname",
									ExampleMatcher.GenericPropertyMatcher::contains)), //
					q -> q.as(UserDto.class).sortBy(Sort.by("firstname")).all());
		});
	}

	@Test
	void countByFluentExample() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("v");

		long numOfUsers = this.repository.findBy(
				of(prototype,
						matching().withIgnorePaths("age", "createdAt", "active").withMatcher("firstname",
								ExampleMatcher.GenericPropertyMatcher::contains)), //
				q -> q.sortBy(Sort.by("firstname")).count());

		assertThat(numOfUsers).isEqualTo(3);
	}

	@Test
	void existsByFluentExample() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("v");

		boolean exists = this.repository.findBy(
				of(prototype,
						matching().withIgnorePaths("age", "createdAt", "active").withMatcher("firstname",
								ExampleMatcher.GenericPropertyMatcher::contains)), //
				q -> q.sortBy(Sort.by("firstname")).exists());

		assertThat(exists).isTrue();
	}

	@Test
	void countByExampleWithExcludedAttributes() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setAge(28);

		Example<User> example = Example.of(prototype, matching().withIgnorePaths("createdAt"));
		long count = this.repository.count(example);

		assertThat(count).isEqualTo(1L);
	}

	@Test
	void existsByExampleWithExcludedAttributes() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setAge(28);

		Example<User> example = Example.of(prototype, matching().withIgnorePaths("createdAt"));
		boolean exists = this.repository.exists(example);

		assertThat(exists).isEqualTo(true);
	}

	@Test
	void existsByExampleNegative() {

		this.flushTestUsers();

		User prototype = new User();
		prototype.setAge(4711); // there is none with that age

		Example<User> example = Example.of(prototype, matching().withIgnorePaths("createdAt"));
		boolean exists = this.repository.exists(example);

		assertThat(exists).isEqualTo(false);
	}

	@Test
	void dynamicProjectionReturningStream() {

		this.flushTestUsers();

		assertThat(this.repository.findAsStreamByFirstnameLike("%O%", User.class)).hasSize(1);
	}

	@Test
	void dynamicProjectionReturningList() {

		this.flushTestUsers();

		List<User> users = this.repository.findAsListByFirstnameLike("%O%", User.class);

		assertThat(users).hasSize(1);
	}

	@Test
	void duplicateSpelsWorkAsIntended() {

		this.flushTestUsers();

		List<User> users = this.repository.findUsersByDuplicateSpel("Oliver");

		assertThat(users).hasSize(1);
	}

	@Test
	void supportsProjectionsWithNativeQueries() {

		this.flushTestUsers();

		User user = this.repository.findAll().get(0);

		NameOnly result = this.repository.findByNativeQuery(user.getId());

		assertThat(result.getFirstname()).isEqualTo(user.getFirstname());
		assertThat(result.getLastname()).isEqualTo(user.getLastname());
	}

	@Test
	void supportsProjectionsWithNativeQueriesAndCamelCaseProperty() {

		this.flushTestUsers();
		User user = this.repository.findAll().get(0);

		UserRepository.EmailOnly result = this.repository.findEmailOnlyByNativeQuery(user.getId());

		String emailAddress = result.getEmailAddress();

		assertThat(emailAddress) //
				.isEqualTo(user.getEmailAddress()) //
				.as("ensuring email is actually not null") //
				.isNotNull();
	}

	@Test
	void handlesColonsFollowedByIntegerInStringLiteral() {

		String firstName = "aFirstName";

		User expected = new User(firstName, "000:1", "something@something");
		User notExpected = new User(firstName, "000\\:1", "something@something.else");

		this.repository.save(expected);
		this.repository.save(notExpected);

		assertThat(this.repository.findAll()).hasSize(2);

		List<User> users = this.repository.queryWithIndexedParameterAndColonFollowedByIntegerInString(firstName);

		assertThat(users).extracting(User::getId).containsExactly(expected.getId());
	}

	@Test
	void handlesCountQueriesWithLessParametersSingleParam() {
		this.repository.findAllOrderedBySpecialNameSingleParam("Oliver", PageRequest.of(2, 3));
	}

	@Test
	void handlesCountQueriesWithLessParametersMoreThanOne() {
		this.repository.findAllOrderedBySpecialNameMultipleParams("Oliver", "x", PageRequest.of(2, 3));
	}

	@Test
	void handlesCountQueriesWithLessParametersMoreThanOneIndexed() {
		this.repository.findAllOrderedBySpecialNameMultipleParamsIndexed("Oliver", "x", PageRequest.of(2, 3));
	}

	@Test
	void executeNativeQueryWithPageWorkaround() {

		this.flushTestUsers();

		Page<String> firstPage = this.repository.findByNativeQueryWithPageable(PageRequest.of(0, 3));
		Page<String> secondPage = this.repository.findByNativeQueryWithPageable(PageRequest.of(1, 3));

		SoftAssertions softly = new SoftAssertions();

		assertThat(firstPage.getTotalElements()).isEqualTo(4L);
		assertThat(firstPage.getNumberOfElements()).isEqualTo(3);
		assertThat(firstPage.getContent()) //
				.containsExactly("Dave", "Joachim", "kevin");

		assertThat(secondPage.getTotalElements()).isEqualTo(4L);
		assertThat(secondPage.getNumberOfElements()).isEqualTo(1);
		assertThat(secondPage.getContent()) //
				.containsExactly("Oliver");

		softly.assertAll();
	}

	@Test
	void bindsNativeQueryResultsToProjectionByName() {

		this.flushTestUsers();

		List<NameOnly> result = this.repository.findByNamedQueryWithAliasInInvertedOrder();

		assertThat(result).element(0).satisfies(it -> {
			assertThat(it.getFirstname()).isEqualTo("Joachim");
			assertThat(it.getLastname()).isEqualTo("Arrasz");
		});
	}

	@Test
	void returnsNullValueInMap() {

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
	void testFindByEmailAddressJdbcStyleParameter() {

		this.flushTestUsers();

		assertThat(this.repository.findByEmailNativeAddressJdbcStyleParameter("gierke@synyx.de"))
				.isEqualTo(this.firstUser);
	}

	@Test
	void savingUserThrowsAnException() {
		assertThatThrownBy(() -> this.repository.save(new User())).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void deleteNewInstanceSucceedsByDoingNothing() {
		this.repository.delete(new User());
	}

	@Test
	void readsDtoProjections() {

		this.flushTestUsers();

		assertThat(this.repository.findAllDtoProjectedBy()).hasSize(4);
	}

	@Test
	void readsDerivedInterfaceProjections() {

		this.flushTestUsers();

		assertThat(this.repository.findAllInterfaceProjectedBy()).hasSize(4);
	}

	@Test
	void modifyingUpdateNativeQueryWorksWithJSQLParser() {

		this.flushTestUsers();

		Optional<User> byIdUser = this.repository.findById(this.firstUser.getId());
		assertThat(byIdUser).isPresent().map(User::isActive).get().isEqualTo(true);

		this.repository.setActiveToFalseWithModifyingNative(byIdUser.get().getId());

		Optional<User> afterUpdate = this.repository.findById(this.firstUser.getId());
		assertThat(afterUpdate).isPresent().map(User::isActive).get().isEqualTo(false);
	}

	@Test
	void correctlyBuildSortClauseWhenSortingByFunctionAliasAndFunctionContainsPositionalParameters() {
		this.repository.findAllAndSortByFunctionResultPositionalParameter("prefix", "suffix",
				Sort.by("idWithPrefixAndSuffix"));
	}

	@Test
	void correctlyBuildSortClauseWhenSortingByFunctionAliasAndFunctionContainsNamedParameters() {
		this.repository.findAllAndSortByFunctionResultNamedParameter("prefix", "suffix",
				Sort.by("idWithPrefixAndSuffix"));
	}

	private interface UserProjectionInterfaceBased {

		String getFirstname();

	}

}
