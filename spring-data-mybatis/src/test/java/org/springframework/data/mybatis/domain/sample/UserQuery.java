package org.springframework.data.mybatis.domain.sample;

import lombok.Data;

@Data
public class UserQuery extends User {

	private String fuzzyFirstname;

	private Integer startAge;

	private Integer endAge;

	public UserQuery() {
	}

}
