package org.springframework.data.mybatis.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.domain.Sort.Direction.ASC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.mybatis.domain.sample.User;
import org.springframework.data.mybatis.repository.sample.UserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context.xml")
@Transactional
public class UserRepositoryTests {

	@Autowired
	UserRepository repository;

	User firstUser, secondUser, thirdUser, fourthUser;

	Integer id;

	@Before
	public void setUp() throws Exception {
		firstUser = new User("Oliver", "Gierke", "gierke@synyx.de");
		firstUser.setAge(28);
		secondUser = new User("Joachim", "Arrasz", "arrasz@synyx.de");
		secondUser.setAge(35);
		Thread.sleep(10);
		thirdUser = new User("Dave", "Matthews", "no@email.com");
		thirdUser.setAge(43);
		fourthUser = new User("kevin", "raymond", "no@gmail.com");
		fourthUser.setAge(31);
	}

	protected void flushTestUsers() {
		firstUser = repository.save(firstUser);
		secondUser = repository.save(secondUser);
		thirdUser = repository.save(thirdUser);
		fourthUser = repository.save(fourthUser);

		id = firstUser.getId();

		assertThat(id).isNotNull();
		assertThat(secondUser.getId()).isNotNull();
		assertThat(thirdUser.getId()).isNotNull();
		assertThat(fourthUser.getId()).isNotNull();

		assertThat(repository.existsById(id)).isTrue();
		assertThat(repository.existsById(secondUser.getId())).isTrue();
		assertThat(repository.existsById(thirdUser.getId())).isTrue();
		assertThat(repository.existsById(fourthUser.getId())).isTrue();
	}

	@Test
	public void testCreation() {

		long before = repository.count();

		flushTestUsers();

		assertThat(repository.count()).isEqualTo(before + 4L);
	}

	@Test
	public void testRead() throws Exception {

		flushTestUsers();

		assertThat(repository.findById(id)).map(User::getFirstname)
				.contains(firstUser.getFirstname());
	}

	@Test
	public void findsAllByGivenIds() {

		flushTestUsers();

		assertThat(repository
				.findAllById(Arrays.asList(firstUser.getId(), secondUser.getId())))
						.contains(firstUser, secondUser);
	}

	@Test
	public void testReadByIdReturnsNullForNotFoundEntities() {

		flushTestUsers();

		assertThat(repository.findById(id * 27)).isNotPresent();
	}

	@Test
	public void savesCollectionCorrectly() throws Exception {

		assertThat(repository.saveAll(Arrays.asList(firstUser, secondUser, thirdUser)))
				.hasSize(3).contains(firstUser, secondUser, thirdUser);
	}

	@Test
	public void savingEmptyCollectionIsNoOp() throws Exception {
		assertThat(repository.saveAll(new ArrayList<>())).isEmpty();
	}

	@Test
	public void testUpdate() {

		flushTestUsers();

		User foundPerson = repository.findById(id).get();
		foundPerson.setLastname("Schlicht");

		assertThat(repository.findById(id)).map(User::getFirstname)
				.contains(foundPerson.getFirstname());
	}

	@Test
	public void existReturnsWhetherAnEntityCanBeLoaded() throws Exception {

		flushTestUsers();
		assertThat(repository.existsById(id)).isTrue();
		assertThat(repository.existsById(id * 27)).isFalse();
	}

	@Test
	public void deletesAUserById() {

		flushTestUsers();

		repository.deleteById(firstUser.getId());

		assertThat(repository.existsById(id)).isFalse();
		assertThat(repository.findById(id)).isNotPresent();
	}

	@Test
	public void testDelete() {

		flushTestUsers();

		repository.delete(firstUser);

		assertThat(repository.existsById(id)).isFalse();
		assertThat(repository.findById(id)).isNotPresent();
	}

	@Test
	public void returnsAllSortedCorrectly() throws Exception {

		flushTestUsers();

		assertThat(repository.findAll(Sort.by(ASC, "lastname"))).hasSize(4)
				.containsExactly(secondUser, firstUser, thirdUser, fourthUser);
	}

	@Test
	public void returnsAllIgnoreCaseSortedCorrectly() throws Exception {

		flushTestUsers();

		Sort.Order order = new Sort.Order(ASC, "firstname").ignoreCase();
		List<User> result = repository.findAll(Sort.by(order));

		assertThat(repository.findAll(Sort.by(order))).hasSize(4)
				.containsExactly(thirdUser, secondUser, fourthUser, firstUser);
	}

	@Test
	public void deleteColletionOfEntities() {

		flushTestUsers();

		long before = repository.count();

		repository.deleteAll(Arrays.asList(firstUser, secondUser));
		assertThat(repository.existsById(firstUser.getId())).isFalse();
		assertThat(repository.existsById(secondUser.getId())).isFalse();
		assertThat(repository.count()).isEqualTo(before - 2);
	}

	@Test
	public void batchDeleteColletionOfEntities() {

		flushTestUsers();

		long before = repository.count();

		repository.deleteInBatch(Arrays.asList(firstUser, secondUser));

		assertThat(repository.existsById(firstUser.getId())).isFalse();
		assertThat(repository.existsById(secondUser.getId())).isFalse();
		assertThat(repository.count()).isEqualTo(before - 2);
	}

	@Test
	public void deleteEmptyCollectionDoesNotDeleteAnything() {

		assertDeleteCallDoesNotDeleteAnything(new ArrayList<User>());
	}

	@Test
	public void executesManipulatingQuery() throws Exception {

		flushTestUsers();
		repository.renameAllUsersTo("newLastname");

		long expected = repository.count();
		assertThat(repository.findByLastname("newLastname").size())
				.isEqualTo(Long.valueOf(expected).intValue());
	}

	@Test
	public void testFinderInvocationWithNullParameter() {

		flushTestUsers();

		repository.findByLastname((String) null);
	}

	@Test
	public void testFindByLastname() throws Exception {

		flushTestUsers();

		assertThat(repository.findByLastname("Gierke")).containsOnly(firstUser);
	}

	@Test
	public void testFindByEmailAddress() throws Exception {

		flushTestUsers();

		assertThat(repository.findByEmailAddress("gierke@synyx.de")).isEqualTo(firstUser);
	}

	@Test
	public void testReadAll() {

		flushTestUsers();

		assertThat(repository.count()).isEqualTo(4L);
		assertThat(repository.findAll()).contains(firstUser, secondUser, thirdUser,
				fourthUser);
	}

	@Test
	public void deleteAll() throws Exception {

		flushTestUsers();

		repository.deleteAll();

		assertThat(repository.count()).isZero();
	}

	@Test
	public void deleteAllInBatch() {

		flushTestUsers();

		repository.deleteAllInBatch();

		assertThat(repository.count()).isZero();
	}

	@Test
	public void testCountsCorrectly() {

		long count = repository.count();

		User user = new User();
		user.setEmailAddress("gierke@synyx.de");
		repository.save(user);

		assertThat(repository.count()).isEqualTo(count + 1);
	}

	@Test
	public void executesLikeAndOrderByCorrectly() throws Exception {

		flushTestUsers();

		assertThat(repository.findByLastnameLikeOrderByFirstnameDesc("%r%")).hasSize(3)
				.containsExactly(fourthUser, firstUser, secondUser);
	}

	@Test
	public void executesNotLikeCorrectly() throws Exception {

		flushTestUsers();

		assertThat(repository.findByLastnameNotLike("%er%")).containsOnly(secondUser,
				thirdUser, fourthUser);
	}

	@Test
	public void executesSimpleNotCorrectly() throws Exception {

		flushTestUsers();

		assertThat(repository.findByLastnameNot("Gierke")).containsOnly(secondUser,
				thirdUser, fourthUser);
	}

	@Test
	public void returnsSameListIfNoSortIsGiven() throws Exception {

		flushTestUsers();
		assertSameElements(repository.findAll(Sort.unsorted()), repository.findAll());
	}

	@Test
	public void returnsAllAsPageIfNoPageableIsGiven() throws Exception {

		flushTestUsers();
		assertThat(repository.findAll(Pageable.unpaged()))
				.isEqualTo(new PageImpl<>(repository.findAll()));
	}

	@Test
	public void executesFindByNotNullLastnameCorrectly() throws Exception {

		flushTestUsers();

		assertThat(repository.findByLastnameNotNull()).containsOnly(firstUser, secondUser,
				thirdUser, fourthUser);
	}

	@Test
	public void executesFindByNullLastnameCorrectly() throws Exception {

		flushTestUsers();
		User forthUser = repository.save(new User("Foo", null, "email@address.com"));

		assertThat(repository.findByLastnameNull()).containsOnly(forthUser);
	}

	@Test
	public void findsSortedByLastname() throws Exception {

		flushTestUsers();

		assertThat(repository.findByEmailAddressLike("%@%",
				Sort.by(Sort.Direction.ASC, "lastname"))).containsExactly(secondUser,
						firstUser, thirdUser, fourthUser);
	}

	@Test
	public void executesLessThatOrEqualQueriesCorrectly() {

		flushTestUsers();

		assertThat(repository.findByAgeLessThanEqual(35)).containsOnly(firstUser,
				secondUser, fourthUser);
	}

	@Test
	public void executesGreaterThatOrEqualQueriesCorrectly() {

		flushTestUsers();

		assertThat(repository.findByAgeGreaterThanEqual(35)).containsOnly(secondUser,
				thirdUser);
	}

	@Test
	public void executesFinderWithTrueKeywordCorrectly() {

		flushTestUsers();
		firstUser.setActive(false);
		repository.save(firstUser);

		assertThat(repository.findByActiveTrue()).containsOnly(secondUser, thirdUser,
				fourthUser);
	}

	@Test
	public void executesFinderWithFalseKeywordCorrectly() {

		flushTestUsers();
		firstUser.setActive(false);
		repository.save(firstUser);

		assertThat(repository.findByActiveFalse()).containsOnly(firstUser);
	}

	@Test
	public void executesFinderWithAfterKeywordCorrectly() {

		flushTestUsers();

		assertThat(repository.findByCreatedAtAfter(secondUser.getCreatedAt()))
				.containsOnly(thirdUser, fourthUser);
	}

	@Test
	public void executesFinderWithBeforeKeywordCorrectly() {

		flushTestUsers();

		assertThat(repository.findByCreatedAtBefore(thirdUser.getCreatedAt()))
				.containsOnly(firstUser, secondUser);
	}

	@Test
	public void executesFinderWithStartingWithCorrectly() {

		flushTestUsers();

		assertThat(repository.findByFirstnameStartingWith("Oli")).containsOnly(firstUser);
	}

	@Test
	public void executesFinderWithEndingWithCorrectly() {

		flushTestUsers();

		assertThat(repository.findByFirstnameEndingWith("er")).containsOnly(firstUser);
	}

	@Test
	public void executesFinderWithContainingCorrectly() {

		flushTestUsers();

		assertThat(repository.findByFirstnameContaining("a")).containsOnly(secondUser,
				thirdUser);
	}

	@Test
	public void allowsExecutingPageableMethodWithUnpagedArgument() {

		flushTestUsers();

		assertThat(repository.findByFirstname("Oliver", null)).containsOnly(firstUser);

		Page<User> page = repository.findByFirstnameIn(Pageable.unpaged(), "Oliver");
		assertThat(page.getNumberOfElements()).isEqualTo(1);
		assertThat(page.getContent()).contains(firstUser);

		page = repository.findAll(Pageable.unpaged());
		assertThat(page.getNumberOfElements()).isEqualTo(4);
		assertThat(page.getContent()).contains(firstUser, secondUser, thirdUser,
				fourthUser);
	}

	@Test
	public void handlesIterableOfIdsCorrectly() {

		flushTestUsers();

		Set<Integer> set = new HashSet<>();
		set.add(firstUser.getId());
		set.add(secondUser.getId());

		assertThat(repository.findAllById(set)).containsOnly(firstUser, secondUser);
	}

	@Test
	public void executesDerivedCountQueryToLong() {

		flushTestUsers();

		assertThat(repository.countByLastname("Matthews")).isEqualTo(1L);
	}

	@Test
	public void executesDerivedCountQueryToInt() {

		flushTestUsers();

		assertThat(repository.countUsersByFirstname("Dave")).isEqualTo(1);
	}

	@Test
	public void executesDerivedExistsQuery() {

		flushTestUsers();

		assertThat(repository.existsByLastname("Matthews")).isEqualTo(true);
		assertThat(repository.existsByLastname("Hans Peter")).isEqualTo(false);
	}

	@Test
	public void findAllReturnsEmptyIterableIfNoIdsGiven() {

		assertThat(repository.findAllById(Collections.<Integer>emptySet())).isEmpty();
	}

	@Test
	public void looksUpEntityReference() {

		flushTestUsers();

		User result = repository.getById(firstUser.getId());
		assertThat(result).isEqualTo(firstUser);
	}

	@Test
	public void invokesQueryWithVarargsParametersCorrectly() {

		flushTestUsers();

		Collection<User> result = repository.findByIdIn(firstUser.getId(),
				secondUser.getId());

		assertThat(result).containsOnly(firstUser, secondUser);
	}

	@Test
	public void executesFinderWithOrderClauseOnly() {

		flushTestUsers();

		assertThat(repository.findAllByOrderByLastnameAsc()).containsOnly(secondUser,
				firstUser, thirdUser, fourthUser);
	}

	@Test
	public void findsUserByBinaryDataReference() throws Exception {

		byte[] data = "Woho!!".getBytes("UTF-8");
		firstUser.setBinaryData(data);

		flushTestUsers();

		List<User> result = repository.findByBinaryData(data);
		assertThat(result).containsOnly(firstUser);
		assertThat(result.get(0).getBinaryData()).isEqualTo(data);
	}

	@Test
	public void deleteByShouldReturnListOfDeletedElementsWhenRetunTypeIsCollectionLike() {

		flushTestUsers();

		List<User> result = repository.deleteByLastname(firstUser.getLastname());
		assertThat(result).containsOnly(firstUser);
	}

	@Test
	public void deleteByShouldRemoveElementsMatchingDerivedQuery() {

		flushTestUsers();

		repository.deleteByLastname(firstUser.getLastname());
		assertThat(repository.countByLastname(firstUser.getLastname())).isEqualTo(0L);
	}

	@Test
	public void deleteByShouldReturnNumberOfEntitiesRemovedIfReturnTypeIsLong() {

		flushTestUsers();

		assertThat(repository.removeByLastname(firstUser.getLastname())).isEqualTo(1L);
	}

	@Test
	public void deleteByShouldReturnZeroInCaseNoEntityHasBeenRemovedAndReturnTypeIsNumber() {

		flushTestUsers();

		assertThat(repository.removeByLastname("bubu")).isEqualTo(0L);
	}

	@Test
	public void deleteByShouldReturnEmptyListInCaseNoEntityHasBeenRemovedAndReturnTypeIsCollectionLike() {

		flushTestUsers();

		assertThat(repository.deleteByLastname("dorfuaeB")).isEmpty();
	}

	@Test
	public void findOldestUser() {

		flushTestUsers();

		User oldest = thirdUser;

		assertThat(repository.findFirstByOrderByAgeDesc()).isEqualTo(oldest);
		assertThat(repository.findFirst1ByOrderByAgeDesc()).isEqualTo(oldest);
	}

	@Test
	public void findYoungestUser() {

		flushTestUsers();

		User youngest = firstUser;

		assertThat(repository.findTopByOrderByAgeAsc()).isEqualTo(youngest);
		assertThat(repository.findTop1ByOrderByAgeAsc()).isEqualTo(youngest);
	}

	@Test
	public void find2OldestUsers() {

		flushTestUsers();

		User oldest1 = thirdUser;
		User oldest2 = secondUser;

		assertThat(repository.findFirst2ByOrderByAgeDesc()).contains(oldest1, oldest2);
		assertThat(repository.findTop2ByOrderByAgeDesc()).contains(oldest1, oldest2);
	}

	@Test
	public void find2YoungestUsers() {

		flushTestUsers();

		User youngest1 = firstUser;
		User youngest2 = fourthUser;

		assertThat(repository.findFirst2UsersBy(Sort.by(ASC, "age"))).contains(youngest1,
				youngest2);
		assertThat(repository.findTop2UsersBy(Sort.by(ASC, "age"))).contains(youngest1,
				youngest2);
	}

	@Test
	public void find3YoungestUsersPageableWithPageSize2() {

		flushTestUsers();

		User youngest1 = firstUser;
		User youngest2 = fourthUser;
		User youngest3 = secondUser;

		Page<User> firstPage = repository
				.findFirst3UsersBy(PageRequest.of(0, 2, ASC, "age"));
		assertThat(firstPage.getContent()).contains(youngest1, youngest2);

		Page<User> secondPage = repository
				.findFirst3UsersBy(PageRequest.of(1, 2, ASC, "age"));
		assertThat(secondPage.getContent()).contains(youngest3);
	}

	@Test
	public void find2YoungestUsersPageableWithPageSize3() {

		flushTestUsers();

		User youngest1 = firstUser;
		User youngest2 = fourthUser;
		User youngest3 = secondUser;

		Page<User> firstPage = repository
				.findFirst2UsersBy(PageRequest.of(0, 3, ASC, "age"));
		assertThat(firstPage.getContent()).contains(youngest1, youngest2);

		Page<User> secondPage = repository
				.findFirst2UsersBy(PageRequest.of(1, 3, ASC, "age"));
		// assertThat(secondPage.getContent()).contains(youngest3);
		assertThat(secondPage.getContent().isEmpty());
	}

	@Test
	public void find3YoungestUsersPageableWithPageSize2Sliced() {

		flushTestUsers();

		User youngest1 = firstUser;
		User youngest2 = fourthUser;
		User youngest3 = secondUser;

		Slice<User> firstPage = repository
				.findTop3UsersBy(PageRequest.of(0, 2, ASC, "age"));
		assertThat(firstPage.getContent()).contains(youngest1, youngest2);// 0,2

		Slice<User> secondPage = repository
				.findTop3UsersBy(PageRequest.of(1, 2, ASC, "age"));
		assertThat(secondPage.getContent()).contains(youngest3);// 2,1
	}

	@Test
	public void find2YoungestUsersPageableWithPageSize3Sliced() {

		flushTestUsers();

		User youngest1 = firstUser;
		User youngest2 = fourthUser;
		User youngest3 = secondUser;

		Slice<User> firstPage = repository
				.findTop2UsersBy(PageRequest.of(0, 3, ASC, "age"));
		assertThat(firstPage.getContent()).contains(youngest1, youngest2);// 0,2

		Slice<User> secondPage = repository
				.findTop2UsersBy(PageRequest.of(1, 3, ASC, "age"));
		// assertThat(secondPage.getContent()).contains(youngest3);
		assertThat(secondPage.getContent().isEmpty());
	}

	@Test
	public void pageableQueryReportsTotalFromResult() {

		flushTestUsers();

		Page<User> firstPage = repository.findAll(PageRequest.of(0, 10));
		assertThat(firstPage.getContent()).hasSize(4);
		assertThat(firstPage.getTotalElements()).isEqualTo(4L);

		Page<User> secondPage = repository.findAll(PageRequest.of(1, 3));
		assertThat(secondPage.getContent()).hasSize(1);
		assertThat(secondPage.getTotalElements()).isEqualTo(4L);
	}

	@Test
	public void pageableQueryReportsTotalFromCount() {

		flushTestUsers();

		Page<User> firstPage = repository.findAll(PageRequest.of(0, 4));
		assertThat(firstPage.getContent()).hasSize(4);
		assertThat(firstPage.getTotalElements()).isEqualTo(4L);

		Page<User> secondPage = repository.findAll(PageRequest.of(10, 10));
		assertThat(secondPage.getContent()).hasSize(0);
		assertThat(secondPage.getTotalElements()).isEqualTo(4L);
	}

	@Test
	public void findByEmptyArrayOfIntegers() throws Exception {

		flushTestUsers();

		List<User> users = repository.queryByAgeIn(new Integer[0]);
		assertThat(users).hasSize(0);
	}

	@Test
	public void findByAgeWithEmptyArrayOfIntegersOrFirstName() {

		flushTestUsers();

		List<User> users = repository.queryByAgeInOrFirstname(new Integer[0],
				secondUser.getFirstname());
		assertThat(users).containsOnly(secondUser);
	}

	private void assertDeleteCallDoesNotDeleteAnything(List<User> collection) {

		flushTestUsers();
		long count = repository.count();

		repository.deleteAll(collection);
		assertThat(repository.count()).isEqualTo(count);
	}

	private static <T> void assertSameElements(Collection<T> first,
			Collection<T> second) {

		for (T element : first) {
			assertThat(element).isIn(second);
		}

		for (T element : second) {
			assertThat(element).isIn(first);
		}
	}

}
