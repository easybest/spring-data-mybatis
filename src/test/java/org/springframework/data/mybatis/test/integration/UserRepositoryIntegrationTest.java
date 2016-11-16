package org.springframework.data.mybatis.test.integration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mybatis.test.domains.User;
import org.springframework.data.mybatis.test.repositories.UserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by songjiawei on 2016/11/9.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class UserRepositoryIntegrationTest {
    @Autowired
    UserRepository repository;

    @Test
    public void sampleTestCase() {
        User dave = new User("Dave", "Matthews");
        dave = repository.save(dave);

        User carter = new User("Carter", "Beauford");
        carter = repository.save(carter);

        User luke = new User("Luke", "Beauford");
        luke = repository.save(luke);

        List<User> result = repository.findByLastnameOrderByFirstnameAsc("Matthews");
        assertThat(result.size(), is(1));
        assertThat(result, hasItem(dave));

        result = repository.findByLastname("Beauford", new Sort(Sort.Direction.DESC, "firstname"));
        assertThat(result.size(), is(2));
        assertThat(result, hasItem(carter));
        assertThat(result, hasItem(luke));


        Page<User> page = repository.findByLastnameOrderByLastnameAsc("Beauford", new PageRequest(0, 1));
        assertThat(page.getSize(), is(1));
    }
}
