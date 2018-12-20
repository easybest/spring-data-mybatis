package org.springframework.data.mybatis.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

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

	Role firstRole, secondRole, thirdRole;

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
		thirdRole = Role.of("system");
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
		roleRepository.save(thirdRole);

		assertThat(firstRole.getId()).isNotNull();
		assertThat(secondRole.getId()).isNotNull();
		assertThat(thirdRole.getId()).isNotNull();
		assertThat(roleRepository.existsById(firstRole.getId())).isTrue();
		assertThat(roleRepository.existsById(secondRole.getId())).isTrue();
		assertThat(roleRepository.existsById(thirdRole.getId())).isTrue();

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
		try {
			Thread.sleep(10);
		}
		catch (InterruptedException e) {
		}
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

		UserRoleKey urid = new UserRoleKey(firstUser.getId(), thirdRole.getId());
		first.setId(urid);
		repository.updateIgnoreNull(new UserRoleKey(firstUser.getId(), firstRole.getId()),
				first);
		UserRole ur = repository.getById(urid);
		assertThat(ur.getId().getRoleId()).isEqualTo(thirdRole.getId());
	}

	@Test
	public void testFind() {
		flushTestUsers();
		Optional<UserRole> ur = repository
				.findById(new UserRoleKey(secondUser.getId(), secondRole.getId()));
		assertThat(ur.isPresent()).isTrue();
		assertThat(ur.get()).isEqualTo(second);

	}

	@Test
	public void findByRoleId() {
		flushTestUsers();
		first.setId(new UserRoleKey(firstUser.getId(), secondRole.getId()));
		repository.update(new UserRoleKey(firstUser.getId(), firstRole.getId()), first);
		assertThat(first.getId().getRoleId()).isEqualTo(secondRole.getId());
		assertThat(repository.findByRoleId(secondRole.getId())).containsExactly(first,
				second, fourth);
	}

}
