package org.springframework.data.mybatis.domain.sample;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.mybatis.annotation.Entity;
import org.springframework.data.mybatis.annotation.Id;

import java.util.Date;

@Data
@NoArgsConstructor
@ToString
@Entity(table = "ds_user")
public class User {
	@Id private Long id;
	private String firstname;
	private String lastname;
	private int age;
	private boolean active;
	private Date createdAt;
	private String emailAddress;

	public User(String firstname, String lastname, String emailAddress) {
		this.firstname = firstname;
		this.lastname = lastname;
		this.emailAddress = emailAddress;
		this.active = true;
		this.createdAt = new Date();
	}
}
