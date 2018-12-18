package org.springframework.data.mybatis.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mybatis.domain.sample.Role;
import org.springframework.data.mybatis.domain.sample.User;
import org.springframework.data.mybatis.domain.sample.UserRole;
import org.springframework.data.mybatis.domain.sample.UserRoleKey;
import org.springframework.data.mybatis.repository.sample.RoleRepository;
import org.springframework.data.mybatis.repository.sample.UserRepository;
import org.springframework.data.mybatis.repository.sample.UserRoleRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context.xml")
@Transactional
public class UserRoleRepositoryTests {

	@Autowired
	UserRoleRepository repository;

	@Autowired
	UserRepository userRepository;

	@Autowired
	RoleRepository roleRepository;

	User firstUser, secondUser, thirdUser, fourthUser;

	Role firstRole, secondRole;

	UserRole first, second, third, fourth;

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

		firstRole = Role.of("admin");
		secondRole = Role.of("user");
	}

	protected void flushTestUsers() {
		firstUser = userRepository.save(firstUser);
		secondUser = userRepository.save(secondUser);
		thirdUser = userRepository.save(thirdUser);
		fourthUser = userRepository.save(fourthUser);

		id = firstUser.getId();

		assertThat(id).isNotNull();
		assertThat(secondUser.getId()).isNotNull();
		assertThat(thirdUser.getId()).isNotNull();
		assertThat(fourthUser.getId()).isNotNull();

		assertThat(userRepository.existsById(id)).isTrue();
		assertThat(userRepository.existsById(secondUser.getId())).isTrue();
		assertThat(userRepository.existsById(thirdUser.getId())).isTrue();
		assertThat(userRepository.existsById(fourthUser.getId())).isTrue();

		roleRepository.save(firstRole);
		roleRepository.save(secondRole);

		assertThat(firstRole.getId()).isNotNull();
		assertThat(secondRole.getId()).isNotNull();
		assertThat(roleRepository.existsById(firstRole.getId())).isTrue();
		assertThat(roleRepository.existsById(secondRole.getId())).isTrue();

		first = new UserRole(firstUser.getId(), firstRole.getId());
		second = new UserRole(secondUser.getId(), secondRole.getId());
		third = new UserRole(thirdUser.getId(), firstRole.getId());
		fourth = new UserRole(fourthUser.getId(), secondRole.getId());

		repository.insert(first);
		repository.insert(second);
		repository.insert(third);
		repository.insert(fourth);

		assertThat(repository.existsById(first.getId())).isTrue();
		assertThat(repository.existsById(second.getId())).isTrue();
		assertThat(repository.existsById(third.getId())).isTrue();
		assertThat(repository.existsById(fourth.getId())).isTrue();

	}

	@Test
	public void testGetByEmbeddedId() {
		flushTestUsers();
		assertThat(
				repository.getById(new UserRoleKey(thirdUser.getId(), firstRole.getId())))
						.isNotNull().isEqualTo(third);

	}

	@Test
	public void testUpdateByEmbeddedId() {
		flushTestUsers();
		assertThat(first.getLastModifiedAt()).isEqualTo(first.getCreatedAt());

		first.setId(new UserRoleKey(firstUser.getId(), secondRole.getId()));
		repository.update(first);
		assertThat(first.getLastModifiedAt()).isNotEqualTo(first.getCreatedAt());
	}

}
