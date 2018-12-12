package org.springframework.data.mybatis.repository;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;

public class CurrentUserAuditorAware implements AuditorAware<Integer> {

	private int i = 1;

	@Override
	public Optional<Integer> getCurrentAuditor() {
		return Optional.of(i++);
	}

}
