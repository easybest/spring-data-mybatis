package org.springframework.data.mybatis.domain.sample;

import java.util.Date;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.mybatis.annotation.CreatedDate;
import org.springframework.data.mybatis.annotation.LastModifiedDate;

import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ds_user_ds_role")
@Data
@NoArgsConstructor
public class UserRole {

	@EmbeddedId
	private UserRoleKey id;

	@CreatedDate
	@Temporal(TemporalType.TIMESTAMP)
	private Date createdAt;

	@LastModifiedDate
	@Temporal(TemporalType.TIMESTAMP)
	private Date lastModifiedAt;

	@CreatedBy
	private Integer createdBy;

	@LastModifiedBy
	private Integer lastModifiedBy;

	public UserRole(Integer userId, Long roleId) {

		this.id = new UserRoleKey(userId, roleId);

	}

}
