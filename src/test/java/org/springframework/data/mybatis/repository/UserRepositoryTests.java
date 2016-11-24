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
package org.springframework.data.mybatis.repository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mybatis.config.sample.TestJPAConfig;
import org.springframework.data.mybatis.domain.sample.Role;
import org.springframework.data.mybatis.domain.sample.User;
import org.springframework.data.mybatis.repository.sample.RoleRepository;
import org.springframework.data.mybatis.repository.sample.UserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.springframework.data.domain.Sort.Direction.ASC;

/**
 * @author Jarvis Song
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestJPAConfig.class)
@Transactional
public class UserRepositoryTests {


    @Autowired
    UserRepository repository;
    @Autowired
    RoleRepository roleRepository;

    User firstUser, secondUser, thirdUser, fourthUser;
    Integer id;
    Role    adminRole;

    @Before
    public void setUp() throws Exception {

        firstUser = new User("Oliver", "Gierke", "gierke@synyx.de");
        firstUser.setAge(28);
        secondUser = new User("Joachim", "Arrasz", "arrasz@synyx.de");
        secondUser.setAge(35);
        thirdUser = new User("Dave", "Matthews", "no@email.com");
        thirdUser.setAge(43);
        fourthUser = new User("kevin", "raymond", "no@gmail.com");
        fourthUser.setAge(31);
        adminRole = new Role("admin");

    }

    protected void flushTestUsers() {

        roleRepository.save(adminRole);

        firstUser = repository.save(firstUser);
        secondUser = repository.save(secondUser);
        thirdUser = repository.save(thirdUser);
        fourthUser = repository.save(fourthUser);


        id = firstUser.getId();

//        assertThat(id, is(notNullValue()));
//        assertThat(secondUser.getId(), is(notNullValue()));
//        assertThat(thirdUser.getId(), is(notNullValue()));
//        assertThat(fourthUser.getId(), is(notNullValue()));
//
//        assertThat(repository.exists(id), is(true));
//        assertThat(repository.exists(secondUser.getId()), is(true));
//        assertThat(repository.exists(thirdUser.getId()), is(true));
//        assertThat(repository.exists(fourthUser.getId()), is(true));
    }

    @Test
    public void executesLikeAndOrderByCorrectly() throws Exception {
        // select user0_.id as id2_1_, user0_.active as active3_1_, user0_.city as city4_1_, user0_.country as country5_1_, user0_.streetName as streetName6_1_, user0_.streetNo as streetNo7_1_, user0_.age as age8_1_, user0_.binaryData as binaryData9_1_, user0_.createdAt as createdAt10_1_, user0_.dateOfBirth as dateOfBirth11_1_, user0_.emailAddress as emailAddress12_1_, user0_.firstname as firstname13_1_, user0_.lastname as lastname14_1_, user0_.manager_id as manager_id15_1_, user0_.DTYPE as DTYPE1_1_ from DS_USER user0_ where user0_.lastname like ? order by user0_.firstname desc
        flushTestUsers();

        List<User> result = repository.findByLastnameLikeOrderByFirstnameDesc("%r%");
        assertThat(result.size(), is(3));
        assertEquals(fourthUser, result.get(0));
        assertEquals(firstUser, result.get(1));
        assertEquals(secondUser, result.get(2));
    }

    @Test
    public void find2YoungestUsersPageableWithPageSize3() {

        flushTestUsers();
        // select * from ( select count(user0_.id) as col_0_0_ from DS_USER user0_ ) where rownum <= ?
        // select * from ( select row_.*, rownum rownum_ from ( select user0_.id as id2_1_, user0_.active as active3_1_, user0_.city as city4_1_, user0_.country as country5_1_, user0_.streetName as streetName6_1_, user0_.streetNo as streetNo7_1_, user0_.age as age8_1_, user0_.binaryData as binaryData9_1_, user0_.createdAt as createdAt10_1_, user0_.dateOfBirth as dateOfBirth11_1_, user0_.emailAddress as emailAddress12_1_, user0_.firstname as firstname13_1_, user0_.lastname as lastname14_1_, user0_.manager_id as manager_id15_1_, user0_.DTYPE as DTYPE1_1_ from DS_USER user0_ order by user0_.age asc ) row_ where rownum <= ?) where rownum_ > ?


        // select user0_.id as id2_1_, user0_.active as active3_1_, user0_.city as city4_1_, user0_.country as country5_1_, user0_.streetName as streetNa6_1_, user0_.streetNo as streetNo7_1_, user0_.age as age8_1_, user0_.binaryData as binaryDa9_1_, user0_.createdAt as created10_1_, user0_.dateOfBirth as dateOfB11_1_, user0_.emailAddress as emailAd12_1_, user0_.firstname as firstna13_1_, user0_.lastname as lastnam14_1_, user0_.manager_id as manager15_1_, user0_.DTYPE as DTYPE1_1_ from DS_USER user0_ order by user0_.age asc limit ?, ?


        // select TOP(?) user0_.id as id2_1_, user0_.active as active3_1_, user0_.city as city4_1_, user0_.country as country5_1_, user0_.streetName as streetNa6_1_, user0_.streetNo as streetNo7_1_, user0_.age as age8_1_, user0_.binaryData as binaryDa9_1_, user0_.createdAt as created10_1_, user0_.dateOfBirth as dateOfB11_1_, user0_.emailAddress as emailAd12_1_, user0_.firstname as firstna13_1_, user0_.lastname as lastnam14_1_, user0_.manager_id as manager15_1_, user0_.DTYPE as DTYPE1_1_ from DS_USER user0_ order by user0_.age asc
        // select TOP(?) count(user0_.id) as col_0_0_ from DS_USER user0_

        // select user0_.id as id2_1_, user0_.active as active3_1_, user0_.city as city4_1_, user0_.country as country5_1_, user0_.streetName as streetNa6_1_, user0_.streetNo as streetNo7_1_, user0_.age as age8_1_, user0_.binaryData as binaryDa9_1_, user0_.createdAt as created10_1_, user0_.dateOfBirth as dateOfB11_1_, user0_.emailAddress as emailAd12_1_, user0_.firstname as firstna13_1_, user0_.lastname as lastnam14_1_, user0_.manager_id as manager15_1_, user0_.DTYPE as DTYPE1_1_ from DS_USER user0_ order by user0_.age asc limit ?, ?

        User youngest1 = firstUser;
        User youngest2 = fourthUser;
        User youngest3 = secondUser;

        Page<User> firstPage = repository.findFirst2UsersBy(new PageRequest(0, 3, ASC, "age"));
        assertThat(firstPage.getContent(), hasItems(youngest1, youngest2));

        // WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( select TOP(?) user0_.id as id2_1_, user0_.active as active3_1_, user0_.city as city4_1_, user0_.country as country5_1_, user0_.streetName as streetNa6_1_, user0_.streetNo as streetNo7_1_, user0_.age as age8_1_, user0_.binaryData as binaryDa9_1_, user0_.createdAt as created10_1_, user0_.dateOfBirth as dateOfB11_1_, user0_.emailAddress as emailAd12_1_, user0_.firstname as firstna13_1_, user0_.lastname as lastnam14_1_, user0_.manager_id as manager15_1_, user0_.DTYPE as DTYPE1_1_ from DS_USER user0_ order by user0_.age asc ) inner_query ) SELECT id2_1_, active3_1_, city4_1_, country5_1_, streetNa6_1_, streetNo7_1_, age8_1_, binaryDa9_1_, created10_1_, dateOfB11_1_, emailAd12_1_, firstna13_1_, lastnam14_1_, manager15_1_, DTYPE1_1_ FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?


        Page<User> secondPage = repository.findFirst2UsersBy(new PageRequest(1, 3, ASC, "age"));
        assertThat(secondPage.getContent(), hasItems(youngest3));
    }

    @Test
    public void findYoungestUser() {
        // select * from ( select user0_.id as id2_1_, user0_.active as active3_1_, user0_.city as city4_1_, user0_.country as country5_1_, user0_.streetName as streetName6_1_, user0_.streetNo as streetNo7_1_, user0_.age as age8_1_, user0_.binaryData as binaryData9_1_, user0_.createdAt as createdAt10_1_, user0_.dateOfBirth as dateOfBirth11_1_, user0_.emailAddress as emailAddress12_1_, user0_.firstname as firstname13_1_, user0_.lastname as lastname14_1_, user0_.manager_id as manager_id15_1_, user0_.DTYPE as DTYPE1_1_ from DS_USER user0_ order by user0_.age asc ) where rownum <= ?


        flushTestUsers();

        User youngest = firstUser;

        assertThat(repository.findTopByOrderByAgeAsc(), is(youngest));
        assertThat(repository.findTop1ByOrderByAgeAsc(), is(youngest));
    }

}
