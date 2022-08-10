/*
 * Copyright 2019-2022 the original author or authors.
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

package io.easybest.mybatis.domain.sample;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import io.easybest.mybatis.annotation.DatabaseDefault;

import org.springframework.data.annotation.PersistenceCreator;

/**
 * Domain class representing a person emphasizing the use of {@code AbstractEntity}. No
 * declaration of an id is required. The id is typed by the parameterizable superclass.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Jens Schauder
 * @author Jeff Sheets
 * @author Jyotirmoy VS
 * @author Jarvis Song
 */
@NamedQueries({ //
		@NamedQuery(name = "User.findByEmailAddress", //
				query = "SELECT u FROM SD_User u WHERE u.emailAddress = ?1"), //
		@NamedQuery(name = "User.findByNamedQueryWithAliasInInvertedOrder", //
				query = "SELECT u.lastname AS lastname, u.firstname AS firstname FROM SD_User u ORDER BY u.lastname ASC"),
		@NamedQuery(name = "User.findByNamedQueryWithConstructorExpression",
				query = "SELECT new io.easybest.mybatis.repository.sample.NameOnlyDto(u.firstname, u.lastname) from SD_User u") })

@Entity
@Table(name = "SD_User")
// @LogicDelete
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

	@ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	private final Set<User> colleagues;

	@ManyToMany
	private final Set<Role> roles;

	@ManyToOne
	private User manager;

	@Embedded
	private Address address;

	@Lob
	private byte[] binaryData;

	@Temporal(TemporalType.DATE)
	private Date dateOfBirth;

	@Version
	@DatabaseDefault
	private Integer version;

	/**
	 * Creates a new empty instance of {@code User}.
	 */
	@PersistenceCreator
	public User() {
		this(null, null, null);
	}

	public User(String firstname, String lastname, String emailAddress, Role... roles) {

		this.firstname = firstname;
		this.lastname = lastname;
		this.emailAddress = emailAddress;
		this.active = true;
		this.roles = new HashSet<>(Arrays.asList(roles));
		this.colleagues = new HashSet<>();
		this.createdAt = new Date();
	}

	public Integer getId() {

		return this.id;
	}

	public void setId(Integer id) {

		this.id = id;
	}

	public String getFirstname() {

		return this.firstname;
	}

	public void setFirstname(final String firstname) {

		this.firstname = firstname;
	}

	public String getLastname() {

		return this.lastname;
	}

	public void setLastname(String lastname) {

		this.lastname = lastname;
	}

	public int getAge() {
		return this.age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getEmailAddress() {

		return this.emailAddress;
	}

	public void setEmailAddress(String emailAddress) {

		this.emailAddress = emailAddress;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isActive() {
		return this.active;
	}

	public Set<Role> getRoles() {

		return this.roles;
	}

	public void addRole(Role role) {

		this.roles.add(role);
	}

	public void removeRole(Role role) {

		this.roles.remove(role);
	}

	public Set<User> getColleagues() {

		return this.colleagues;
	}

	public void addColleague(User collegue) {

		// Prevent from adding the user himself as colleague.
		if (this.equals(collegue)) {
			return;
		}

		this.colleagues.add(collegue);
		collegue.getColleagues().add(this);
	}

	public void removeColleague(User colleague) {

		this.colleagues.remove(colleague);
		colleague.getColleagues().remove(this);
	}

	public User getManager() {

		return this.manager;
	}

	public void setManager(User manager) {

		this.manager = manager;
	}

	public Date getCreatedAt() {
		return this.createdAt;
	}

	public Address getAddress() {
		return this.address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public void setBinaryData(byte[] binaryData) {
		this.binaryData = binaryData;
	}

	public byte[] getBinaryData() {
		return this.binaryData;
	}

	public Integer getVersion() {
		return this.version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	@Override
	public boolean equals(Object obj) {

		if (!(obj instanceof User)) {
			return false;
		}

		User that = (User) obj;

		if (null == this.getId() || null == that.getId()) {
			return false;
		}

		return this.getId().equals(that.getId());
	}

	@Override
	public int hashCode() {

		return super.hashCode();
	}

	public Date getDateOfBirth() {
		return this.dateOfBirth;
	}

	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	@Override
	public String toString() {

		return "User: " + this.getId() + ", " + this.getFirstname() + " " + this.getLastname() + ", "
				+ this.getEmailAddress();
	}

}
