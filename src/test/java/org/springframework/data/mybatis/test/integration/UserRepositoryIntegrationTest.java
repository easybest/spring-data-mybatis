/*
 *
 *   Copyright 2016 the original author or authors.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

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

        User tod = new User("Tod", "Eades");
        tod = repository.save(tod);
        User hahn = new User("Hahn", "Eades");
        hahn = repository.save(hahn);
        User macy = new User("Macy", "Eades");
        macy = repository.save(macy);

        User christy = new User("Christy", "Sach");
        christy = repository.save(christy);
        User hamlin = new User("Hamlin", "Sach");
        hamlin = repository.save(hamlin);

        List<User> result = repository.findByLastnameOrderByFirstnameAsc("Matthews");
        assertThat(result.size(), is(1));
        assertThat(result, hasItem(dave));

        result = repository.findByLastname("Beauford", new Sort(Sort.Direction.DESC, "firstname"));
        assertThat(result.size(), is(2));
        assertThat(result, hasItem(carter));
        assertThat(result, hasItem(luke));


        Page<User> page = repository.findByLastnameOrderByLastnameAsc("Beauford", new PageRequest(0, 1));
        assertThat(page.getSize(), is(1));
        assertThat(page.getTotalElements(), is(2L));

        Long count = repository.countByLastname("Beauford");
        assertThat(count, is(2L));

        Long deleteCount = repository.deleteByLastname("Eades");
        assertThat(deleteCount, is(3L));

        List<User> sachs = repository.removeByLastname("Sach");
        assertThat(sachs.size(), is(2));
    }
}
