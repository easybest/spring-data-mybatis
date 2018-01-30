/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mybatis.repository.sample;

import org.springframework.data.mybatis.domain.sample.IdClassExampleEmployee;
import org.springframework.data.mybatis.domain.sample.IdClassExampleEmployeePK;
import org.springframework.data.mybatis.repository.MyBatisRepository;

/**
 * Demonstrates the support for composite primary keys with {@code @IdClass}.
 * 
 * @author Thomas Darimont
 * @author Mark Paluch
 */
//@Lazy
public interface EmployeeRepositoryWithIdClass
		extends MyBatisRepository<IdClassExampleEmployee, IdClassExampleEmployeePK>
//		, QuerydslPredicateExecutor<IdClassExampleEmployee>
{

//	List<IdClassExampleEmployee> findAll(Predicate predicate, OrderSpecifier<?>... orders);

	// DATAJPA-920
//	boolean existsByName(String name);
}
