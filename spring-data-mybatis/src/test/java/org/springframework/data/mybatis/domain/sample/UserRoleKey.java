package org.springframework.data.mybatis.domain.sample;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleKey {

	@Column(name = "ds_user_id")
	private Integer userId;

	@Column(name = "ds_role_id")
	private Long roleId;

}
