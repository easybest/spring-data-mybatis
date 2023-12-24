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

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.easybest.mybatis.domain.sample.Role;
import io.easybest.mybatis.domain.sample.User;
import io.easybest.mybatis.repository.sample.RoleRepository;
import io.easybest.mybatis.repository.sample.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;

/**
 * .
 *
 * @author Jarvis Song
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:config/namespace-application-context.xml")
@Transactional
public class UserRepositoryFinderTests {

	@Autowired
	UserRepository userRepository;

	@Autowired
	RoleRepository roleRepository;

	private User dave;

	private User carter;

	private User oliver;

	private Role drummer;

	private Role guitarist;

	private Role singer;

	@BeforeEach
	void setUp() {

		this.drummer = this.roleRepository.save(new Role("DRUMMER"));
		this.guitarist = this.roleRepository.save(new Role("GUITARIST"));
		this.singer = this.roleRepository.save(new Role("SINGER"));

		this.dave = this.userRepository.saveWithRoles(new User("Dave", "Matthews", "dave@dmband.com", this.singer));

		this.carter = this.userRepository
				.saveWithRoles(new User("Carter", "Beauford", "carter@dmband.com", this.singer, this.drummer));
		this.oliver = this.userRepository.saveWithRoles(new User("Oliver August", "Matthews", "oliver@dmband.com"));
	}

	@AfterEach
	void clearUp() {

		this.userRepository.deleteAll();
		this.roleRepository.deleteAll();
	}

	@Test
	void testSimpleCustomCreatedFinder() {

		User user = this.userRepository.findByEmailAddressAndLastname("dave@dmband.com", "Matthews");
		assertThat(user).isEqualTo(this.dave);
	}

	@Test
	void returnsNullIfNothingFound() {

		User user = this.userRepository.findByEmailAddress("foobar");
		assertThat(user).isNull();
	}

	@Test
	void testAndOrFinder() {

		List<User> users = this.userRepository.findByEmailAddressAndLastnameOrFirstname("dave@dmband.com", "Matthews",
				"Carter");

		assertThat(users).isNotNull();
		assertThat(users).containsExactlyInAnyOrder(this.dave, this.carter);
	}

	@Test
	void executesPagingMethodToPageCorrectly() {

		Page<User> page = this.userRepository.findByLastname(PageRequest.of(0, 1), "Matthews");

		assertThat(page.getNumberOfElements()).isEqualTo(1);
		assertThat(page.getTotalElements()).isEqualTo(2L);
		assertThat(page.getTotalPages()).isEqualTo(2);
	}

	@Test
	void executesPagingMethodToListCorrectly() {

		List<User> list = this.userRepository.findByFirstname("Carter", PageRequest.of(0, 1));
		assertThat(list).containsExactly(this.carter);
	}

	@Test
	void executesInKeywordForPageCorrectly() {

		Page<User> page = this.userRepository.findByFirstnameIn(PageRequest.of(0, 1), "Dave", "Oliver August");

		assertThat(page.getNumberOfElements()).isEqualTo(1);
		assertThat(page.getTotalElements()).isEqualTo(2L);
		assertThat(page.getTotalPages()).isEqualTo(2);
	}

	@Test
	void executesNotInQueryCorrectly() {

		List<User> result = this.userRepository.findByFirstnameNotIn(Arrays.asList("Dave", "Carter"));

		assertThat(result).containsExactly(this.oliver);
	}

	@Test
	void findsByLastnameIgnoringCase() {

		List<User> result = this.userRepository.findByLastnameIgnoringCase("BeAUfoRd");

		assertThat(result).containsExactly(this.carter);
	}

	@Test
	void findsByLastnameIgnoringCaseLike() {

		List<User> result = this.userRepository.findByLastnameIgnoringCaseLike("BeAUfo%");

		assertThat(result).containsExactly(this.carter);
	}

	@Test
	void findByLastnameAndFirstnameAllIgnoringCase() {

		List<User> result = this.userRepository.findByLastnameAndFirstnameAllIgnoringCase("MaTTheWs", "DaVe");

		assertThat(result).containsExactly(this.dave);
	}

	@Test
	void respectsPageableOrderOnQueryGenerateFromMethodName() {

		Page<User> ascending = this.userRepository.findByLastnameIgnoringCase( //
				PageRequest.of(0, 10, Sort.by(ASC, "firstname")), //
				"Matthews" //
		);
		Page<User> descending = this.userRepository.findByLastnameIgnoringCase( //
				PageRequest.of(0, 10, Sort.by(DESC, "firstname")), //
				"Matthews" //
		);

		assertThat(ascending).containsExactly(this.dave, this.oliver);
		assertThat(descending).containsExactly(this.oliver, this.dave);
	}

	@Test
	void executesQueryToSlice() {

		Slice<User> slice = this.userRepository.findSliceByLastname("Matthews", PageRequest.of(0, 1, ASC, "firstname"));

		assertThat(slice.getContent()).containsExactly(this.dave);
		assertThat(slice.hasNext()).isTrue();
	}

	@Test
	void executesQueryToSliceWithUnpaged() {

		Slice<User> slice = this.userRepository.findSliceByLastname("Matthews", Pageable.unpaged());

		assertThat(slice).containsExactlyInAnyOrder(this.dave, this.oliver);
		assertThat(slice.getNumberOfElements()).isEqualTo(2);
		assertThat(slice.hasNext()).isEqualTo(false);
	}

	@Test
	void executesMethodWithNotContainingOnStringCorrectly() {

		assertThat(this.userRepository.findByLastnameNotContaining("u")) //
				.containsExactly(this.dave, this.oliver);
	}

	@Test
	void parametersForContainsGetProperlyEscaped() {

		assertThat(this.userRepository.findByFirstnameContaining("liv%")) //
				.isEmpty();
	}

	@Test
	void escapingInLikeSpels() {

		User extra = new User("extra", "Matt_ew", "extra");

		this.userRepository.save(extra);

		assertThat(this.userRepository.findContainingEscaped("att_")).containsExactly(extra);
	}

	@Test
	void escapingInLikeSpelsInThePresenceOfEscapeCharacters() {

		User withEscapeCharacter = this.userRepository.save(new User("extra", "Matt\\xew", "extra1"));
		this.userRepository.save(new User("extra", "Matt\\_ew", "extra2"));

		assertThat(this.userRepository.findContainingEscaped("att\\x")).containsExactly(withEscapeCharacter);
	}

	@Test
	void escapingInLikeSpelsInThePresenceOfEscapedWildcards() {

		this.userRepository.save(new User("extra", "Matt\\xew", "extra1"));
		User withEscapedWildcard = this.userRepository.save(new User("extra", "Matt\\_ew", "extra2"));

		assertThat(this.userRepository.findContainingEscaped("att\\_")).containsExactly(withEscapedWildcard);
	}

	@Test
	void translatesContainsToMemberOf() {

		assertThat(this.userRepository.findByRolesContaining(this.singer)) //
				.containsExactlyInAnyOrder(this.dave, this.carter);

		assertThat(this.userRepository.findByRolesContaining(this.drummer)) //
				.containsExactly(this.carter);
	}

	@Test
	void translatesNotContainsToNotMemberOf() {

		assertThat(this.userRepository.findByRolesNotContaining(this.drummer)) //
				.containsExactlyInAnyOrder(this.dave, this.oliver);
	}

	@Test
	void executesQueryWithProjectionContainingReferenceToPluralAttribute() {

		assertThat(this.userRepository.findRolesAndFirstnameBy()) //
				.isNotNull();
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void rejectsStreamExecutionIfNoSurroundingTransactionActive() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> this.userRepository.findAllByCustomQueryAndStream());
	}

	@Test
	void executesNamedQueryWithConstructorExpression() {
		this.userRepository.findByNamedQueryWithConstructorExpression();
	}

}
