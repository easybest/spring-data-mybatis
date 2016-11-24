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
import org.springframework.data.mybatis.domain.sample.Group;
import org.springframework.data.mybatis.domain.sample.Role;
import org.springframework.data.mybatis.repository.sample.GroupRepository;
import org.springframework.data.mybatis.repository.sample.RoleRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.*;

/**
 * @author Jarvis Song
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class RoleSimpleRepositoryTests {

    @Autowired
    RoleRepository  roleRepository;
    @Autowired
    GroupRepository groupRepository;
    Role manager, tester, developer;
    Group group1, group2;

    @Before
    public void setUp() throws Exception {

        group1 = new Group("group1", "First Group");
        group2 = new Group("group2", "Second Group");

        groupRepository.save(Arrays.asList(group1, group2));

        manager = new Role("manager", group1);
        tester = new Role("tester", group2);
        developer = new Role("developer", group2);
        roleRepository.save(Arrays.asList(manager, tester, developer));
    }

    @Test
    public void testSave() {

        Role role = new Role("assistant");
        assertNull(role.getId());
        role = roleRepository.save(role);
        assertNotNull(role.getId());
    }

    @Test
    public void testSaveMultiple() {
        Role assistant1 = new Role("assistant1");
        Role assistant2 = new Role("assistant2");
        List<Role> roles = new ArrayList<Role>();
        roles.add(assistant1);
        roles.add(assistant2);
        Iterable<Role> savedRoles = roleRepository.save(roles);
        assertThat(savedRoles, hasItem(assistant1));
        assertThat(savedRoles, hasItem(assistant2));
    }

    @Test
    public void testFindOne() {
        Role admin1 = roleRepository.findOne(this.manager.getId());
        assertEquals(this.manager.getId(), admin1.getId());
        assertNotNull(admin1.getGroup());
    }

    @Test
    public void testExists() {
        boolean exists = roleRepository.exists(manager.getId());
        assertTrue(exists);
        exists = roleRepository.exists(0);
        assertFalse(exists);
    }


    @Test
    public void testFindAll() {
        List<Role> all = roleRepository.findAll();
        assertEquals(3, all.size());

        for (Role role : all) {
            assertNotNull(role.getGroup());
        }

    }

    @Test
    public void testFindAllByIds() {
        Iterable<Integer> ids = Arrays.asList(manager.getId(), tester.getId());
        List<Role> all = roleRepository.findAll(ids);
        assertEquals(2, all.size());
        assertThat(all, hasItem(manager));
        assertThat(all, hasItem(tester));

    }

    @Test
    public void testCount() {
        long count = roleRepository.count();
        assertEquals(3, count);
    }

    @Test
    public void testDeleteById() {
        long count = roleRepository.count();
        assertEquals(3, count);
        roleRepository.delete(manager.getId());
        count = roleRepository.count();
        assertEquals(2, count);
    }

    @Test
    public void testDeleteByEntity() {
        long count = roleRepository.count();
        assertEquals(3, count);
        roleRepository.delete(manager);
        count = roleRepository.count();
        assertEquals(2, count);
    }

    @Test
    public void testDeleteMultiple() {
        long count = roleRepository.count();
        assertEquals(3, count);
        roleRepository.delete(Arrays.asList(manager, tester));
        count = roleRepository.count();
        assertEquals(1, count);
    }

    @Test
    public void testDeleteAll() {
        roleRepository.deleteAll();
        long count = roleRepository.count();
        assertEquals(0, count);
    }

    @Test
    public void testFindBySort() {
        List<Role> roles = roleRepository.findAll(new Sort(Sort.Direction.ASC, "name"));
        assertEquals(developer, roles.get(0));
        assertEquals(manager, roles.get(1));
        assertEquals(tester, roles.get(2));
    }

    @Test
    public void testFindByPage() {
        Page<Role> page = roleRepository.findAll(new PageRequest(0, 2));
        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertEquals(2, page.getContent().size());

        for (Role role : page) {
            assertNotNull(role.getGroup());
        }

    }

    @Test
    public void testFindBasicOne() {

    }
}
