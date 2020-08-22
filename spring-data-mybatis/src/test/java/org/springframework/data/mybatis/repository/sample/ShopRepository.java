/*
 * Copyright 2012-2019 the original author or authors.
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
package org.springframework.data.mybatis.repository.sample;

import java.util.List;

import org.springframework.data.mybatis.domain.sample.Shop;
import org.springframework.data.mybatis.repository.Modifying;
import org.springframework.data.mybatis.repository.MybatisRepository;
import org.springframework.data.mybatis.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository interface for {@code Shop}.
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public interface ShopRepository extends MybatisRepository<Shop, Long>, ShopRepositoryCustom {

	List<Shop> findByNameLike(String name);

	@Modifying
	@Query("update #{#entityName} set active = :activeState where id in :ids")
	void updateActiveState(@Param("activeState") boolean activeState, @Param("ids") Long... ids);

	List<Shop> findByActiveTrue();

	List<Shop> findByActiveFalse();

	void removeByNameAndActiveOrEmailAddressNotLike(String name, Boolean active, String email);

}
