package org.springframework.data.mybatis.domain.sample;

import lombok.Data;

import javax.persistence.*;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "ds_user")
public class User {

	@Id @GeneratedValue(strategy = GenerationType.AUTO) private Integer id;
	private String firstname;
	private String lastname;
	private int age;
	private boolean active;
	@Temporal(TemporalType.TIMESTAMP) private Date createdAt;

	@Column(nullable = false, unique = true) private String emailAddress;

	@ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE }) private Set<User> colleagues;

	@ManyToMany private Set<Role> roles;

	@ManyToOne private User manager;

	@Embedded private Address address;

	@Lob private byte[] binaryData;

	@ElementCollection private Set<String> attributes;

	@Temporal(TemporalType.DATE) private Date dateOfBirth;

	/**
	 * Creates a new empty instance of {@code User}.
	 */
	public User() {
		this(null, null, null);
	}

	/**
	 * Creates a new instance of {@code User} with preinitialized values for firstname, lastname, email address and roles.
	 *
	 * @param firstname
	 * @param lastname
	 * @param emailAddress
	 * @param roles
	 */
	public User(String firstname, String lastname, String emailAddress, Role... roles) {

		this.firstname = firstname;
		this.lastname = lastname;
		this.emailAddress = emailAddress;
		this.active = true;
		this.roles = new HashSet<Role>(Arrays.asList(roles));
		this.colleagues = new HashSet<User>();
		this.attributes = new HashSet<String>();
		this.createdAt = new Date();
	}

	/**
	 * Gives the user a role. Adding a role the user already owns is a no-op.
	 */
	public void addRole(Role role) {

		roles.add(role);
	}

	/**
	 * Revokes a role from a user.
	 *
	 * @param role
	 */
	public void removeRole(Role role) {

		roles.remove(role);
	}

	/**
	 * Adds a new colleague to the user. Adding the user himself as colleague is a no-op.
	 *
	 * @param collegue
	 */
	public void addColleague(User collegue) {

		// Prevent from adding the user himself as colleague.
		if (this.equals(collegue)) {
			return;
		}

		colleagues.add(collegue);
		collegue.getColleagues().add(this);
	}

	/**
	 * Removes a colleague from the list of colleagues.
	 *
	 * @param colleague
	 */
	public void removeColleague(User colleague) {

		colleagues.remove(colleague);
		colleague.getColleagues().remove(this);
	}

}
