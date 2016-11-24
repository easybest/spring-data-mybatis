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
import org.springframework.data.domain.Sort;
import org.springframework.data.mybatis.config.sample.TestConfig;
import org.springframework.data.mybatis.domain.sample.Role;
import org.springframework.data.mybatis.domain.sample.User;
import org.springframework.data.mybatis.repository.sample.RoleRepository;
import org.springframework.data.mybatis.repository.sample.UserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;

/**
 * @author Jarvis Song
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class UserRepositoryFinderTests {
    @Autowired
    UserRepository userRepository;
    @Autowired
    RoleRepository roleRepository;

    User dave, carter, oliver;
    Role drummer, guitarist, singer;

    @Before
    public void setUp() {
        drummer = roleRepository.save(new Role("DRUMMER"));
        guitarist = roleRepository.save(new Role("GUITARIST"));
        singer = roleRepository.save(new Role("SINGER"));

        dave = userRepository.save(new User("Dave", "Matthews", "dave@dmband.com", singer));
        carter = userRepository.save(new User("Carter", "Beauford", "carter@dmband.com", singer, drummer));
        oliver = userRepository.save(new User("Oliver August", "Matthews", "oliver@dmband.com"));
    }


    @Test
    public void testSimpleCustomCreatedFinder() {
        // select user0_.id as id1_2_, user0_.active as active2_2_, user0_.city as city3_2_, user0_.country as country4_2_, user0_.streetName as streetNa5_2_, user0_.streetNo as streetNo6_2_, user0_.age as age7_2_, user0_.binaryData as binaryDa8_2_, user0_.createdAt as createdA9_2_, user0_.dateOfBirth as dateOfB10_2_, user0_.emailAddress as emailAd11_2_, user0_.firstname as firstna12_2_, user0_.lastname as lastnam13_2_, user0_.manager_id as manager14_2_ from User user0_ where user0_.emailAddress=? and user0_.lastname=?
        // select user0_.id as id2_1_, user0_.active as active3_1_, user0_.city as city4_1_, user0_.country as country5_1_, user0_.streetName as streetNa6_1_, user0_.streetNo as streetNo7_1_, user0_.age as age8_1_, user0_.binaryData as binaryDa9_1_, user0_.createdAt as created10_1_, user0_.dateOfBirth as dateOfB11_1_, user0_.emailAddress as emailAd12_1_, user0_.firstname as firstna13_1_, user0_.lastname as lastnam14_1_, user0_.manager_id as manager15_1_, user0_.DTYPE as DTYPE1_1_ from DS_USER user0_ where user0_.emailAddress=? and user0_.lastname=?

        User user = userRepository.findByEmailAddressAndLastname("dave@dmband.com", "Matthews");
        assertEquals(dave, user);
    }


    @Test
    public void returnsNullIfNothingFound() {
        // select user0_.id as id1_2_, user0_.active as active2_2_, user0_.city as city3_2_, user0_.country as country4_2_, user0_.streetName as streetNa5_2_, user0_.streetNo as streetNo6_2_, user0_.age as age7_2_, user0_.binaryData as binaryDa8_2_, user0_.createdAt as createdA9_2_, user0_.dateOfBirth as dateOfB10_2_, user0_.emailAddress as emailAd11_2_, user0_.firstname as firstna12_2_, user0_.lastname as lastnam13_2_, user0_.manager_id as manager14_2_ from User user0_ where user0_.emailAddress=?

        User user = userRepository.findByEmailAddress("foobar");
        assertEquals(null, user);
    }

    @Test
    public void testAndOrFinder() {
        // select user0_.id as id1_2_, user0_.active as active2_2_, user0_.city as city3_2_, user0_.country as country4_2_, user0_.streetName as streetNa5_2_, user0_.streetNo as streetNo6_2_, user0_.age as age7_2_, user0_.binaryData as binaryDa8_2_, user0_.createdAt as createdA9_2_, user0_.dateOfBirth as dateOfB10_2_, user0_.emailAddress as emailAd11_2_, user0_.firstname as firstna12_2_, user0_.lastname as lastnam13_2_, user0_.manager_id as manager14_2_ from User user0_ where user0_.emailAddress=? and user0_.lastname=? or user0_.firstname=?

        List<User> users = userRepository.findByEmailAddressAndLastnameOrFirstname("dave@dmband.com", "Matthews", "Carter");
        assertNotNull(users);
        assertEquals(2, users.size());
        assertTrue(users.contains(dave));
        assertTrue(users.contains(carter));
    }

    @Test
    public void executesPagingMethodToPageCorrectly() {
        // select user0_.id as id2_2_, user0_.active as active3_2_, user0_.city as city4_2_, user0_.country as country5_2_, user0_.streetName as streetNa6_2_, user0_.streetNo as streetNo7_2_, user0_.age as age8_2_, user0_.binaryData as binaryDa9_2_, user0_.createdAt as created10_2_, user0_.dateOfBirth as dateOfB11_2_, user0_.emailAddress as emailAd12_2_, user0_.firstname as firstna13_2_, user0_.lastname as lastnam14_2_, user0_.manager_id as manager15_2_, user0_.DTYPE as DTYPE1_2_ from User user0_ where user0_.lastname=? limit ?


        // select count(user0_.id) as col_0_0_ from DS_USER user0_ where user0_.lastname=?
        // select * from ( select user0_.id as id2_1_, user0_.active as active3_1_, user0_.city as city4_1_, user0_.country as country5_1_, user0_.streetName as streetName6_1_, user0_.streetNo as streetNo7_1_, user0_.age as age8_1_, user0_.binaryData as binaryData9_1_, user0_.createdAt as createdAt10_1_, user0_.dateOfBirth as dateOfBirth11_1_, user0_.emailAddress as emailAddress12_1_, user0_.firstname as firstname13_1_, user0_.lastname as lastname14_1_, user0_.manager_id as manager_id15_1_, user0_.DTYPE as DTYPE1_1_ from DS_USER user0_ where user0_.lastname=? ) where rownum <= ?


        // select * from ( select user0_.id as id2_1_, user0_.active as active3_1_, user0_.city as city4_1_, user0_.country as country5_1_, user0_.streetName as streetName6_1_, user0_.streetNo as streetNo7_1_, user0_.age as age8_1_, user0_.binaryData as binaryData9_1_, user0_.createdAt as createdAt10_1_, user0_.dateOfBirth as dateOfBirth11_1_, user0_.emailAddress as emailAddress12_1_, user0_.firstname as firstname13_1_, user0_.lastname as lastname14_1_, user0_.manager_id as manager_id15_1_, user0_.DTYPE as DTYPE1_1_ from DS_USER user0_ where user0_.lastname=? order by user0_.firstname desc ) where rownum <= ?


        // select count(user0_.id) as col_0_0_ from DS_USER user0_ where user0_.lastname=?
        // select * from ( select user0_.id as id2_1_, user0_.active as active3_1_, user0_.city as city4_1_, user0_.country as country5_1_, user0_.streetName as streetName6_1_, user0_.streetNo as streetNo7_1_, user0_.age as age8_1_, user0_.binaryData as binaryData9_1_, user0_.createdAt as createdAt10_1_, user0_.dateOfBirth as dateOfBirth11_1_, user0_.emailAddress as emailAddress12_1_, user0_.firstname as firstname13_1_, user0_.lastname as lastname14_1_, user0_.manager_id as manager_id15_1_, user0_.DTYPE as DTYPE1_1_ from DS_USER user0_ where user0_.lastname=? order by user0_.firstname desc ) where rownum <= ?

        // select TOP ?  user0_.id as id2_1_, user0_.active as active3_1_, user0_.city as city4_1_, user0_.country as country5_1_, user0_.streetName as streetNa6_1_, user0_.streetNo as streetNo7_1_, user0_.age as age8_1_, user0_.binaryData as binaryDa9_1_, user0_.createdAt as created10_1_, user0_.dateOfBirth as dateOfB11_1_, user0_.emailAddress as emailAd12_1_, user0_.firstname as firstna13_1_, user0_.lastname as lastnam14_1_, user0_.manager_id as manager15_1_, user0_.DTYPE as DTYPE1_1_ from DS_USER user0_ where user0_.lastname=? order by user0_.firstname desc

        // select TOP(?) user0_.id as id2_1_, user0_.active as active3_1_, user0_.city as city4_1_, user0_.country as country5_1_, user0_.streetName as streetNa6_1_, user0_.streetNo as streetNo7_1_, user0_.age as age8_1_, user0_.binaryData as binaryDa9_1_, user0_.createdAt as created10_1_, user0_.dateOfBirth as dateOfB11_1_, user0_.emailAddress as emailAd12_1_, user0_.firstname as firstna13_1_, user0_.lastname as lastnam14_1_, user0_.manager_id as manager15_1_, user0_.DTYPE as DTYPE1_1_ from DS_USER user0_ where user0_.lastname=?

        // WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( select user0_.id as id2_1_, user0_.active as active3_1_, user0_.city as city4_1_, user0_.country as country5_1_, user0_.streetName as streetNa6_1_, user0_.streetNo as streetNo7_1_, user0_.age as age8_1_, user0_.binaryData as binaryDa9_1_, user0_.createdAt as created10_1_, user0_.dateOfBirth as dateOfB11_1_, user0_.emailAddress as emailAd12_1_, user0_.firstname as firstna13_1_, user0_.lastname as lastnam14_1_, user0_.manager_id as manager15_1_, user0_.DTYPE as DTYPE1_1_ from DS_USER user0_ where user0_.lastname=? ) inner_query ) SELECT id2_1_, active3_1_, city4_1_, country5_1_, streetNa6_1_, streetNo7_1_, age8_1_, binaryDa9_1_, created10_1_, dateOfB11_1_, emailAd12_1_, firstna13_1_, lastnam14_1_, manager15_1_, DTYPE1_1_ FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?
        // WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( select TOP(?) user0_.id as id2_1_, user0_.active as active3_1_, user0_.city as city4_1_, user0_.country as country5_1_, user0_.streetName as streetNa6_1_, user0_.streetNo as streetNo7_1_, user0_.age as age8_1_, user0_.binaryData as binaryDa9_1_, user0_.createdAt as created10_1_, user0_.dateOfBirth as dateOfB11_1_, user0_.emailAddress as emailAd12_1_, user0_.firstname as firstna13_1_, user0_.lastname as lastnam14_1_, user0_.manager_id as manager15_1_, user0_.DTYPE as DTYPE1_1_ from DS_USER user0_ where user0_.lastname=? order by user0_.firstname desc ) inner_query ) SELECT id2_1_, active3_1_, city4_1_, country5_1_, streetNa6_1_, streetNo7_1_, age8_1_, binaryDa9_1_, created10_1_, dateOfB11_1_, emailAd12_1_, firstna13_1_, lastnam14_1_, manager15_1_, DTYPE1_1_ FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?

        // select user0_.id as id2_1_, user0_.active as active3_1_, user0_.city as city4_1_, user0_.country as country5_1_, user0_.streetName as streetNa6_1_, user0_.streetNo as streetNo7_1_, user0_.age as age8_1_, user0_.binaryData as binaryDa9_1_, user0_.createdAt as created10_1_, user0_.dateOfBirth as dateOfB11_1_, user0_.emailAddress as emailAd12_1_, user0_.firstname as firstna13_1_, user0_.lastname as lastnam14_1_, user0_.manager_id as manager15_1_, user0_.DTYPE as DTYPE1_1_ from DS_USER user0_ where user0_.lastname=? order by user0_.firstname desc limit ?, ?


        Page<User> page = userRepository.findByLastnameOrderByFirstnameDesc(new PageRequest(1, 1), "Matthews");
        assertThat(page.getNumberOfElements(), is(1));
        assertThat(page.getTotalElements(), is(2L));
        assertThat(page.getTotalPages(), is(2));
    }

    @Test
    public void respectsPageableOrderOnQueryGenerateFromMethodName() throws Exception {
        //  select * from ( select user0_.id as id2_1_, user0_.active as active3_1_, user0_.city as city4_1_, user0_.country as country5_1_, user0_.streetName as streetName6_1_, user0_.streetNo as streetNo7_1_, user0_.age as age8_1_, user0_.binaryData as binaryData9_1_, user0_.createdAt as createdAt10_1_, user0_.dateOfBirth as dateOfBirth11_1_, user0_.emailAddress as emailAddress12_1_, user0_.firstname as firstname13_1_, user0_.lastname as lastname14_1_, user0_.manager_id as manager_id15_1_, user0_.DTYPE as DTYPE1_1_ from DS_USER user0_ where upper(user0_.lastname)=upper(?) order by user0_.firstname desc ) where rownum <= ?

        // select TOP(?) user0_.id as id2_1_, user0_.active as active3_1_, user0_.city as city4_1_, user0_.country as country5_1_, user0_.streetName as streetNa6_1_, user0_.streetNo as streetNo7_1_, user0_.age as age8_1_, user0_.binaryData as binaryDa9_1_, user0_.createdAt as created10_1_, user0_.dateOfBirth as dateOfB11_1_, user0_.emailAddress as emailAd12_1_, user0_.firstname as firstna13_1_, user0_.lastname as lastnam14_1_, user0_.manager_id as manager15_1_, user0_.DTYPE as DTYPE1_1_ from DS_USER user0_ where upper(user0_.lastname)=upper(?) order by user0_.firstname desc

        Page<User> ascending = userRepository.findByLastnameIgnoringCase(new PageRequest(0, 10, new Sort(ASC, "firstname")),
                "Matthews");
        Page<User> descending = userRepository
                .findByLastnameIgnoringCase(new PageRequest(0, 10, new Sort(DESC, "firstname")), "Matthews");
        assertThat(ascending.getTotalElements(), is(2L));
        assertThat(descending.getTotalElements(), is(2L));
        assertThat(ascending.getContent().get(0).getFirstname(),
                is(not(equalTo(descending.getContent().get(0).getFirstname()))));
        assertThat(ascending.getContent().get(0).getFirstname(),
                is(equalTo(descending.getContent().get(1).getFirstname())));
        assertThat(ascending.getContent().get(1).getFirstname(),
                is(equalTo(descending.getContent().get(0).getFirstname())));
    }

    @Test
    public void findsByLastnameIgnoringCaseLike() throws Exception {
        // select user0_.id as id2_1_, user0_.active as active3_1_, user0_.city as city4_1_, user0_.country as country5_1_, user0_.streetName as streetName6_1_, user0_.streetNo as streetNo7_1_, user0_.age as age8_1_, user0_.binaryData as binaryData9_1_, user0_.createdAt as createdAt10_1_, user0_.dateOfBirth as dateOfBirth11_1_, user0_.emailAddress as emailAddress12_1_, user0_.firstname as firstname13_1_, user0_.lastname as lastname14_1_, user0_.manager_id as manager_id15_1_, user0_.DTYPE as DTYPE1_1_ from DS_USER user0_ where upper(user0_.lastname) like upper(?)

        // select user0_.id as id2_1_, user0_.active as active3_1_, user0_.city as city4_1_, user0_.country as country5_1_, user0_.streetName as streetNa6_1_, user0_.streetNo as streetNo7_1_, user0_.age as age8_1_, user0_.binaryData as binaryDa9_1_, user0_.createdAt as created10_1_, user0_.dateOfBirth as dateOfB11_1_, user0_.emailAddress as emailAd12_1_, user0_.firstname as firstna13_1_, user0_.lastname as lastnam14_1_, user0_.manager_id as manager15_1_, user0_.DTYPE as DTYPE1_1_ from DS_USER user0_ where upper(user0_.lastname) like upper(?)

        List<User> result = userRepository.findByLastnameIgnoringCaseLike("BeAUfo%");
        assertThat(result.size(), is(1));
        assertThat(result.get(0), is(carter));
    }
}
