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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mybatis.domain.sample.Customer;
import org.springframework.data.mybatis.domain.sample.Customer.Constellation;
import org.springframework.data.mybatis.domain.sample.Customer.Gender;
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
@ContextConfiguration(locations = "classpath:config/namespace-application-context.xml")
@Transactional
public class CustomerRepositoryTest {

	@Autowired
	CustomerRepository repository;

	Customer first;

	Customer second;

	Customer third;

	Customer fourth;

	Name name;

	Customer dave;

	Customer carter;

	Customer oliver;

	@Before
	public void setUp() {

		this.first = new Customer("Oliver", "Gierke").setAge(28).setGender(Gender.MALE)
				.setConstellation(Constellation.Aquarius).setEmailAddress("gierke@synyx.de");
		this.second = new Customer("Joachim", "Arrasz").setAge(35).setGender(Gender.FEMALE)
				.setConstellation(Constellation.Cancer).setEmailAddress("arrasz@synyx.de");
		this.third = new Customer("Dave", "Matthews").setAge(43).setGender(Gender.MALE)
				.setConstellation(Constellation.Cancer).setEmailAddress("no@email.com");
		this.fourth = new Customer("kevin", "raymond").setAge(31).setGender(Gender.FEMALE)
				.setConstellation(Constellation.Libra).setEmailAddress("no@email.com");

	}

	protected void flushSimple() {
		this.dave = this.repository.save(new Customer("Dave", "Matthews", "dave@dmband.com"));
		this.carter = this.repository.save(new Customer("Carter", "Beauford", "carter@dmband.com"));
		this.oliver = this.repository.save(new Customer("Oliver August", "Matthews", "oliver@dmband.com"));

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

		Assertions.assertThat(this.repository.findById(this.name)).map(Customer::getName)
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
		List<Customer> all = this.repository.findAll(Sort.by(Direction.ASC, "name.lastname"));
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

	private void assertDeleteCallDoesNotDeleteAnything(List<Customer> collection) {

		this.flushTestCustomers();
		long count = this.repository.count();

		this.repository.deleteAll(collection);
		Assertions.assertThat(this.repository.count()).isEqualTo(count);
	}

	@Test
	public void executesQueryLikeExpressionCorrectly() {

		this.flushTestCustomers();

		List<Customer> result = this.repository.findByNameFirstnameLike("Da");

		Assertions.assertThat(result).containsOnly(this.third);
	}

	@Test
	public void executesQueryInMapperCorrectly() {

		this.flushTestCustomers();

		List<Customer> result = this.repository.findByNameFirstnameLikeInMapper("Da");

		Assertions.assertThat(result).containsOnly(this.third);
	}

	@Test
	public void executesQueryInMapperSpecifyCorrectly() {

		this.flushTestCustomers();

		List<Customer> result = this.repository.findByFirstnameLikeInMapper("Da");

		Assertions.assertThat(result).containsOnly(this.third);
	}

	@Test
	public void executesDerivedCountQueryToLong() {

		this.flushTestCustomers();

		Assertions.assertThat(this.repository.countByNameLastname("Matthews")).isEqualTo(1L);
	}

	@Test
	public void executesDerivedCountQueryToInt() {

		this.flushTestCustomers();

		Assertions.assertThat(this.repository.countCustomersByNameFirstname("Dave")).isEqualTo(1);
	}

	@Test
	public void executesDerivedExistsQuery() {

		this.flushTestCustomers();

		Assertions.assertThat(this.repository.existsByNameLastname("Matthews")).isEqualTo(true);
		Assertions.assertThat(this.repository.existsByNameLastname("Hans Peter")).isEqualTo(false);
	}

	@Test
	public void executesManuallyDefinedQueryWithFieldProjection() {

		this.flushTestCustomers();
		List<String> lastname = this.repository.findFirstnamesByLastname("Matthews");

		Assertions.assertThat(lastname).containsOnly("Dave");
	}

	@Test
	public void executesFinderWithOrderClauseOnly() {

		this.flushTestCustomers();

		Assertions.assertThat(this.repository.findAllByOrderByNameLastnameAsc()).containsOnly(this.second, this.first,
				this.third, this.fourth);
	}

	@Test
	public void findOldestUser() {

		this.flushTestCustomers();

		Customer oldest = this.third;

		Assertions.assertThat(this.repository.findFirstByOrderByAgeDesc()).isEqualTo(oldest);
		Assertions.assertThat(this.repository.findFirst1ByOrderByAgeDesc()).isEqualTo(oldest);
	}

	@Test
	public void find2OldestUsers() {

		this.flushTestCustomers();

		Customer oldest1 = this.third;
		Customer oldest2 = this.second;

		Assertions.assertThat(this.repository.findFirst2ByOrderByAgeDesc()).contains(oldest1, oldest2);
		Assertions.assertThat(this.repository.findTop2ByOrderByAgeDesc()).contains(oldest1, oldest2);
	}

	@Test
	public void find3YoungestUsersPageableWithPageSize2Sliced() {

		this.flushTestCustomers();

		Customer youngest1 = this.first;
		Customer youngest2 = this.fourth;
		Customer youngest3 = this.second;

		Slice<Customer> firstPage = this.repository.findTop3CustomersBy(PageRequest.of(0, 2, Direction.ASC, "age"));
		Assertions.assertThat(firstPage.getContent()).contains(youngest1, youngest2);

		Slice<Customer> secondPage = this.repository.findTop3CustomersBy(PageRequest.of(1, 2, Direction.ASC, "age"));
		Assertions.assertThat(secondPage.getContent()).contains(youngest3);
	}

	@Test
	public void find2YoungestUsersPageableWithPageSize3Sliced() {

		this.flushTestCustomers();

		Customer youngest1 = this.first;
		Customer youngest2 = this.fourth;
		Customer youngest3 = this.second;

		Slice<Customer> firstPage = this.repository.findTop2CustomersBy(PageRequest.of(0, 3, Direction.ASC, "age"));
		Assertions.assertThat(firstPage.getContent()).contains(youngest1, youngest2);

		Slice<Customer> secondPage = this.repository.findTop2CustomersBy(PageRequest.of(1, 3, Direction.ASC, "age"));
		Assertions.assertThat(secondPage.getContent()).contains(youngest3);
	}

	@Test
	public void pageableQueryReportsTotalFromResult() {

		this.flushTestCustomers();

		Page<Customer> firstPage = this.repository.findAll(PageRequest.of(0, 10));
		Assertions.assertThat(firstPage.getContent()).hasSize(4);
		Assertions.assertThat(firstPage.getTotalElements()).isEqualTo(4L);

		Page<Customer> secondPage = this.repository.findAll(PageRequest.of(1, 3));
		Assertions.assertThat(secondPage.getContent()).hasSize(1);
		Assertions.assertThat(secondPage.getTotalElements()).isEqualTo(4L);
	}

	@Test
	public void pageableQueryReportsTotalFromCount() {

		this.flushTestCustomers();

		Page<Customer> firstPage = this.repository.findAll(PageRequest.of(0, 4));
		Assertions.assertThat(firstPage.getContent()).hasSize(4);
		Assertions.assertThat(firstPage.getTotalElements()).isEqualTo(4L);

		Page<Customer> secondPage = this.repository.findAll(PageRequest.of(10, 10));
		Assertions.assertThat(secondPage.getContent()).hasSize(0);
		Assertions.assertThat(secondPage.getTotalElements()).isEqualTo(4L);
	}

	@Test
	public void findAllByExampleWithPageable() {

		this.flushTestCustomers();

		for (int i = 0; i < 99; i++) {
			Customer customer1 = new Customer("Oliver-" + i, "Srping");
			customer1.setAge(30 + i);

			this.repository.save(customer1);
		}

		Customer prototype = new Customer();
		prototype.setName(new Name("oLi", null));

		Example<Customer> example = Example.of(prototype, ExampleMatcher.matching().withIgnoreCase()
				.withIgnorePaths("age").withStringMatcher(StringMatcher.STARTING).withIgnoreCase());

		Page<Customer> users = this.repository.findAll(example, PageRequest.of(0, 10, Sort.by(Direction.DESC, "age")));

		Assertions.assertThat(users.getSize()).isEqualTo(10);
		Assertions.assertThat(users.hasNext()).isEqualTo(true);
		Assertions.assertThat(users.getTotalElements()).isEqualTo(100L);
	}

	@Test
	public void findByEnum() {
		this.flushTestCustomers();

		List<Customer> customers = this.repository.findByGender(Gender.FEMALE);
		Assertions.assertThat(customers).hasSize(2).contains(this.second, this.fourth);

		customers = this.repository.findByConstellation(Constellation.Cancer);
		Assertions.assertThat(customers).hasSize(2).contains(this.second, this.third);
	}

	@Test
	public void findAllByExampleWithEnum() {
		this.flushTestCustomers();
		Customer prototype = new Customer();
		prototype.setGender(Gender.FEMALE);
		prototype.setConstellation(Constellation.Cancer);

		List<Customer> customers = this.repository.findAll(Example.of(prototype));
		assertThat(customers).hasSize(1).contains(this.second);

	}

	@Test
	public void testFindByEmailAddress() {

		this.flushTestCustomers();

		assertThat(this.repository.findByEmailAddress("gierke@synyx.de")).isEqualTo(this.first);
	}

	@Test
	public void testSimpleCustomCreatedFinder() {
		this.flushTestCustomers();
		Customer user = this.repository.findByEmailAddressAndNameLastname("no@email.com", "Matthews");
		assertThat(user).isEqualTo(this.third);
	}

	@Test
	public void testAndOrFinder() {
		this.flushTestCustomers();

		List<Customer> customers = this.repository.findByEmailAddressAndNameLastnameOrNameFirstname("no@email.com",
				"Matthews", "Joachim");

		assertThat(customers).isNotNull();
		assertThat(customers).containsExactlyInAnyOrder(this.third, this.second);
	}

	@Test
	public void testUsesQueryAnnotation() {
		assertThat(this.repository.findByAnnotatedQuery("gierke@synyx.de")).isNull();
	}

	@Test
	public void executesPagingMethodToPageCorrectly() {
		this.flushTestCustomers();

		Page<Customer> page = this.repository.findByNameLastname(PageRequest.of(0, 1), "Matthews");

		assertThat(page.getNumberOfElements()).isEqualTo(1);
		assertThat(page.getTotalElements()).isEqualTo(1);
		assertThat(page.getTotalPages()).isEqualTo(1);

		page = this.repository.findByEmailAddress(PageRequest.of(0, 1), "no@email.com");

		assertThat(page.getNumberOfElements()).isEqualTo(1);
		assertThat(page.getTotalElements()).isEqualTo(2L);
		assertThat(page.getTotalPages()).isEqualTo(2L);
	}

	@Test
	public void executesPagingMethodToListCorrectly() {
		this.flushTestCustomers();
		List<Customer> list = this.repository.findByNameFirstname("Dave", PageRequest.of(0, 1));
		assertThat(list).containsExactly(this.third);
	}

	@Test
	public void executesInKeywordForPageCorrectly() {
		this.flushTestCustomers();

		Page<Customer> page = this.repository.findByNameFirstnameIn(PageRequest.of(0, 1), "Dave", "Joachim");

		assertThat(page.getNumberOfElements()).isEqualTo(1);
		assertThat(page.getTotalElements()).isEqualTo(2L);
		assertThat(page.getTotalPages()).isEqualTo(2);
	}

	@Test
	public void executesNotInQueryCorrectly() {
		this.flushTestCustomers();

		List<Customer> result = this.repository.findByNameFirstnameNotIn(Arrays.asList("Dave", "Joachim"));

		assertThat(result).containsExactly(this.first, this.fourth);
	}

	@Test
	public void executesManualQueryWithPositionLikeExpressionCorrectly() {

		this.flushTestCustomers();

		List<Customer> result = this.repository.findByFirstnameLike("Da");

		assertThat(result).containsOnly(this.third);
	}

	@Test
	public void executesManualQueryWithNamedLikeExpressionCorrectly() {

		this.flushTestCustomers();

		List<Customer> result = this.repository.findByFirstnameLikeNamed("Da");

		assertThat(result).containsOnly(this.third);
	}

	@Test
	public void executesManipulatingQuery() throws Exception {

		this.flushTestCustomers();
		this.repository.renameAllUsersTo("newLastname");

		long expected = this.repository.count();
		assertThat(this.repository.findByNameLastname("newLastname").size())
				.isEqualTo(Long.valueOf(expected).intValue());
	}

	@Test
	public void testExecutionOfProjectingMethod() {

		this.flushTestCustomers();
		assertThat(this.repository.countWithFirstname("Oliver")).isEqualTo(1L);
	}

	@Test
	public void executesMethodWithAnnotatedNamedParametersCorrectly() {

		this.first = this.repository.save(this.first);
		this.second = this.repository.save(this.second);

		assertThat(this.repository.findByLastnameOrFirstname("Oliver", "Arrasz")).contains(this.first, this.second);
	}

	@Test
	public void executesMethodWithNamedParametersCorrectlyOnMethodsWithQueryCreation() {

		this.first = this.repository.save(this.first);
		this.second = this.repository.save(this.second);

		assertThat(this.repository.findByNameFirstnameOrNameLastname("Oliver", "Arrasz")).containsOnly(this.first,
				this.second);
	}

	@Test
	public void executesLikeAndOrderByCorrectly() {

		this.flushTestCustomers();

		assertThat(this.repository.findByNameLastnameLikeOrderByNameFirstnameDesc("%r%")).hasSize(3)
				.containsExactly(this.fourth, this.first, this.second);
	}

	@Test
	public void executesNotLikeCorrectly() {

		this.flushTestCustomers();

		assertThat(this.repository.findByNameLastnameNotLike("%er%")).containsOnly(this.second, this.third,
				this.fourth);
	}

	@Test
	public void executesFindByNotNullLastnameCorrectly() {

		this.flushTestCustomers();

		assertThat(this.repository.findByNameLastnameNotNull()).containsOnly(this.first, this.second, this.third,
				this.fourth);
	}

	@Test
	public void executesFindByNullLastnameCorrectly() {

		this.flushTestCustomers();
		Customer forthUser = this.repository.save(new Customer("Foo", "Bar").setEmailAddress(null));

		assertThat(this.repository.findByEmailAddressNull()).containsOnly(forthUser);
	}

	@Test
	public void findsSortedByLastname() {

		this.flushTestCustomers();

		assertThat(this.repository.findByEmailAddressLike("%@%", Sort.by(Direction.ASC, "name.lastname")))
				.containsExactly(this.second, this.first, this.third, this.fourth);
	}

	@Test
	public void findsUsersBySpringDataNamedQuery() {

		this.flushTestCustomers();

		assertThat(this.repository.findBySpringDataNamedQuery("Gierke")).containsOnly(this.first);
	}

	@Test
	public void findsByLastnameIgnoringCase() {
		this.flushTestCustomers();

		List<Customer> result = this.repository.findByNameLastnameIgnoringCase("gIeRKe");

		assertThat(result).containsExactly(this.first);
	}

	@Test
	public void respectsPageableOrderOnQueryGenerateFromMethodName() {
		this.flushSimple();
		Page<Customer> ascending = this.repository.findByNameLastnameIgnoringCase(
				PageRequest.of(0, 10, Sort.by(Direction.ASC, "name.firstname")), "Matthews");
		Page<Customer> descending = this.repository.findByNameLastnameIgnoringCase(
				PageRequest.of(0, 10, Sort.by(Direction.DESC, "name.firstname")), "Matthews");

		assertThat(ascending).containsExactly(this.dave, this.oliver);
		assertThat(descending).containsExactly(this.oliver, this.dave);
	}

	@Test
	public void findsByLastnameIgnoringCaseLike() {
		this.flushSimple();

		List<Customer> result = this.repository.findByNameLastnameIgnoringCaseLike("BeAUfo%");

		assertThat(result).containsExactly(this.carter);
	}

	@Test
	public void findByLastnameAndFirstnameAllIgnoringCase() {
		this.flushSimple();

		List<Customer> result = this.repository.findByNameLastnameAndNameFirstnameAllIgnoringCase("MaTTheWs", "DaVe");

		assertThat(result).containsExactly(this.dave);
	}

	@Test
	public void executesGreaterThatOrEqualQueriesCorrectly() {

		this.flushTestCustomers();

		assertThat(this.repository.findByAgeGreaterThanEqual(35)).containsOnly(this.second, this.third);
	}

	@Test
	public void executesLessThatOrEqualQueriesCorrectly() {

		this.flushTestCustomers();

		assertThat(this.repository.findByAgeLessThanEqual(35)).containsOnly(this.first, this.second, this.fourth);
	}

	@Test
	public void readsPageWithGroupByClauseCorrectly() {

		this.flushTestCustomers();

		Page<String> result = this.repository.findByLastnameGrouped(PageRequest.of(0, 10));
		assertThat(result.getTotalPages()).isEqualTo(1);
	}

	@Test
	public void executesFinderWithStartingWithCorrectly() {

		this.flushTestCustomers();

		assertThat(this.repository.findByNameFirstnameStartingWith("Oli")).containsOnly(this.first);
	}

	@Test
	public void executesFinderWithEndingWithCorrectly() {

		this.flushTestCustomers();

		assertThat(this.repository.findByNameFirstnameEndingWith("er")).containsOnly(this.first);
	}

	@Test
	public void executesFinderWithContainingCorrectly() {

		this.flushTestCustomers();

		assertThat(this.repository.findByNameFirstnameContaining("a")).containsOnly(this.second, this.third);
	}

	@Test
	public void executesNativeQueryForNonEntitiesCorrectly() {

		this.flushTestCustomers();

		List<Integer> result = this.repository.findOnesByNativeQuery();

		assertThat(result.size()).isEqualTo(4);
		assertThat(result).contains(1);
	}

	@Test
	public void deleteByShouldRemoveElementsMatchingDerivedQuery() {

		this.flushTestCustomers();

		this.repository.deleteByNameLastname(this.first.getName().getLastname());
		assertThat(this.repository.countByNameLastname(this.first.getName().getLastname())).isEqualTo(0L);
	}

	@Test
	public void findsUserByBinaryDataReference() {

		byte[] data = "Woho!!".getBytes(StandardCharsets.UTF_8);
		this.first.setBinaryData(data);

		this.flushTestCustomers();

		List<Customer> result = this.repository.findByBinaryData(data);
		assertThat(result).containsOnly(this.first);
		assertThat(result.get(0).getBinaryData()).isEqualTo(data);
	}

	@Test
	public void executesQueryToSlice() {
		this.flushSimple();
		Slice<Customer> slice = this.repository.findSliceByNameLastname("Matthews",
				PageRequest.of(0, 1, Direction.ASC, "name.firstname"));

		assertThat(slice.getContent()).containsExactly(this.dave);
		assertThat(slice.hasNext()).isTrue();
	}

	@Test
	public void executesQueryToSliceWithUnpaged() {
		this.flushSimple();

		Slice<Customer> slice = this.repository.findSliceByNameLastname("Matthews", Pageable.unpaged());

		assertThat(slice).containsExactlyInAnyOrder(this.dave, this.oliver);
		assertThat(slice.getNumberOfElements()).isEqualTo(2);
		assertThat(slice.hasNext()).isEqualTo(false);
	}

	@Test
	public void deleteByShouldReturnNumberOfEntitiesRemovedIfReturnTypeIsLong() {

		this.flushTestCustomers();

		assertThat(this.repository.removeByNameLastname(this.first.getName().getLastname())).isEqualTo(1L);
	}

	@Test
	public void deleteByShouldReturnListOfDeletedElementsWhenRetunTypeIsCollectionLike() {

		this.flushTestCustomers();

		List<Customer> result = this.repository.deleteByNameLastname(this.first.getName().getLastname());
		assertThat(result).containsOnly(this.first);
	}

	@Test
	public void deleteByShouldReturnEmptyListInCaseNoEntityHasBeenRemovedAndReturnTypeIsCollectionLike() {

		this.flushTestCustomers();

		assertThat(this.repository.deleteByNameLastname("dorfuaeB")).isEmpty();
	}

	@Test
	public void findYoungestUser() {

		this.flushTestCustomers();

		Customer youngest = this.first;

		assertThat(this.repository.findTopByOrderByAgeAsc()).isEqualTo(youngest);
		assertThat(this.repository.findTop1ByOrderByAgeAsc()).isEqualTo(youngest);
	}

	@Test
	public void find2YoungestUsers() {

		this.flushTestCustomers();

		Customer youngest1 = this.first;
		Customer youngest2 = this.fourth;

		assertThat(this.repository.findFirst2CustomersBy(Sort.by(Direction.ASC, "age"))).contains(youngest1, youngest2);
		assertThat(this.repository.findTop2CustomersBy(Sort.by(Direction.ASC, "age"))).contains(youngest1, youngest2);
	}

	@Test
	public void find3YoungestUsersPageableWithPageSize2() {

		this.flushTestCustomers();

		Customer youngest1 = this.first;
		Customer youngest2 = this.fourth;
		Customer youngest3 = this.second;

		Page<Customer> firstPage = this.repository.findFirst3CustomersBy(PageRequest.of(0, 2, Direction.ASC, "age"));
		assertThat(firstPage.getContent()).contains(youngest1, youngest2);

		Page<Customer> secondPage = this.repository.findFirst3CustomersBy(PageRequest.of(1, 2, Direction.ASC, "age"));
		assertThat(secondPage.getContent()).contains(youngest3);
	}

	@Test
	public void find2YoungestUsersPageableWithPageSize3() {

		this.flushTestCustomers();

		Customer youngest1 = this.first;
		Customer youngest2 = this.fourth;
		Customer youngest3 = this.second;

		Page<Customer> firstPage = this.repository.findFirst2CustomersBy(PageRequest.of(0, 3, Direction.ASC, "age"));
		assertThat(firstPage.getContent()).contains(youngest1, youngest2);

		Page<Customer> secondPage = this.repository.findFirst2CustomersBy(PageRequest.of(1, 3, Direction.ASC, "age"));
		assertThat(secondPage.getContent()).contains(youngest3);
	}

	@Test
	public void invokesQueryWithWrapperType() {

		this.flushTestCustomers();

		Optional<Customer> result = this.repository.findOptionalByEmailAddress("gierke@synyx.de");

		assertThat(result.isPresent()).isEqualTo(true);
		assertThat(result.get()).isEqualTo(this.first);
	}

	@Test
	public void shouldFindUserByFirstnameAndLastnameWithSpelExpressionInStringBasedQuery() {

		this.flushTestCustomers();
		List<Customer> users = this.repository.findByFirstnameAndLastnameWithSpelExpression("Oliver", "ierk");

		assertThat(users).containsOnly(this.first);
	}

	@Test
	public void shouldFindBySpELExpressionWithoutArgumentsWithQuestionmark() {

		this.flushTestCustomers();
		List<Customer> users = this.repository.findOliverBySpELExpressionWithoutArgumentsWithQuestionmark();

		assertThat(users).containsOnly(this.first);
	}

	@Test
	public void shouldFindUsersByAgeForSpELExpression() {

		this.flushTestCustomers();
		List<Customer> users = this.repository.findUsersByAgeForSpELExpressionByIndexedParameter(35);

		assertThat(users).containsOnly(this.second);
	}

	@Test
	public void findByEmptyCollectionOfIntegers() throws Exception {

		this.flushTestCustomers();

		List<Customer> users = this.repository.findByAgeIn(Collections.emptyList());
		assertThat(users).hasSize(0);
	}

	@Test
	public void findByEmptyArrayOfIntegers() throws Exception {

		this.flushTestCustomers();

		List<Customer> users = this.repository.queryByAgeIn(new Integer[0]);
		assertThat(users).hasSize(0);
	}

	@Test
	public void findByAgeWithEmptyArrayOfIntegersOrFirstName() {

		this.flushTestCustomers();

		List<Customer> users = this.repository.queryByAgeInOrNameFirstname(new Integer[0],
				this.second.getName().getFirstname());
		assertThat(users).containsOnly(this.second);
	}

	@Test
	public void shouldSupportJava8StreamsForRepositoryFinderMethods() {

		this.flushTestCustomers();

		try (Stream<Customer> stream = this.repository.findAllByCustomQueryAndStream()) {
			assertThat(stream).hasSize(4);
		}
	}

	@Test
	public void shouldSupportJava8StreamsForRepositoryDerivedFinderMethods() {

		this.flushTestCustomers();

		try (Stream<Customer> stream = this.repository.readAllByNameFirstnameNotNull()) {
			assertThat(stream).hasSize(4);
		}
	}

	@Test
	public void supportsJava8StreamForPageableMethod() {

		this.flushTestCustomers();

		try (Stream<Customer> stream = this.repository.streamAllPaged(PageRequest.of(0, 2))) {
			assertThat(stream).hasSize(2);
		}
	}

	@Test
	public void executesMethodWithNotContainingOnStringCorrectly() {
		this.flushSimple();
		assertThat(this.repository.findByNameLastnameNotContaining("u")).containsExactly(this.dave, this.oliver);
	}

}
