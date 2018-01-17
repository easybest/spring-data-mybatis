package org.springframework.data.mybatis.repository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mybatis.domain.sample.User;
import org.springframework.data.mybatis.repository.sample.UserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context.xml")
@Transactional
public class UserRepositoryTests {

	@Autowired UserRepository repository;
	User firstUser, secondUser, thirdUser, fourthUser;
	Long id;

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
	public void testRead() throws Exception {

		flushTestUsers();

		assertThat(repository.findById(id)).map(User::getFirstname).contains(firstUser.getFirstname());
	}
}
