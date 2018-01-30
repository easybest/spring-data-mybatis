package org.springframework.data.mybatis.repository.sample;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mybatis.domain.sample.User;
import org.springframework.data.mybatis.repository.support.SqlSessionRepositorySupport;

@Slf4j
public class UserRepositoryImpl extends SqlSessionRepositorySupport implements UserRepositoryCustom {

	@Autowired
	protected UserRepositoryImpl(SqlSessionTemplate sqlSessionTemplate) {
		super(sqlSessionTemplate);
	}

	@Override
	public void findByOverrridingMethod() {
		log.debug("A method overriding a finder was invoked!");

	}

	@Override
	public void someCustomMethod(User user) {
		log.debug("Some custom method was invoked!");

	}

	@Override
	protected String getNamespace() {
		return UserRepository.class.getName();
	}
}
