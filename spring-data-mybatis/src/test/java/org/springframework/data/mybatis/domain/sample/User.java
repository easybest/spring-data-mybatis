package org.springframework.data.mybatis.domain.sample;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import lombok.Data;

@Data
@Entity
@Table(name = "ds_user")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Integer id;

	private String firstname;

	private String lastname;

	private int age;

	private boolean active;

	@Temporal(TemporalType.TIMESTAMP)
	private Date createdAt;

	@Column(nullable = false, unique = true)
	private String emailAddress;

	@Lob
	private byte[] binaryData;

	@Temporal(TemporalType.DATE)
	private Date dateOfBirth;

	/**
	 * Creates a new empty instance of {@code User}.
	 */
	public User() {
		this(null, null, null);
	}

	/**
	 * Creates a new instance of {@code User} with preinitialized values for firstname,
	 * lastname, email address and roles.
	 * @param firstname
	 * @param lastname
	 * @param emailAddress
	 */
	public User(String firstname, String lastname, String emailAddress) {

		this.firstname = firstname;
		this.lastname = lastname;
		this.emailAddress = emailAddress;
		this.active = true;

		this.createdAt = new Date();
	}

}
