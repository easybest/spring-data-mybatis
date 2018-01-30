package org.springframework.data.mybatis.repository.sample;

import org.springframework.data.mybatis.domain.sample.User;

public interface UserRepositoryCustom {
	/**
	 * Method actually triggering a finder but being overridden.
	 */
	void findByOverrridingMethod();

	/**
	 * Some custom method to implement.
	 *
	 * @param user
	 */
	void someCustomMethod(User user);
}
