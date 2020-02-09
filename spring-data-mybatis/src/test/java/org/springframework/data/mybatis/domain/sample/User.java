/*
 * Copyright 2012-2019 the original author or authors.
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
package org.springframework.data.mybatis.domain.sample;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NamedStoredProcedureQueries;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.NamedSubgraph;
import javax.persistence.ParameterMode;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.StoredProcedureParameter;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.springframework.data.mybatis.annotation.Example;

@Example
@Entity
@NamedEntityGraphs({ @NamedEntityGraph(name = "User.overview", attributeNodes = { @NamedAttributeNode("roles") }),
		@NamedEntityGraph(name = "User.detail",
				attributeNodes = { @NamedAttributeNode("roles"), @NamedAttributeNode("manager"),
						@NamedAttributeNode("colleagues") }),
		@NamedEntityGraph(name = "User.getOneWithDefinedEntityGraphById",
				attributeNodes = { @NamedAttributeNode("roles"), @NamedAttributeNode("manager"),
						@NamedAttributeNode("colleagues") }),
		@NamedEntityGraph(name = "User.withSubGraph",
				attributeNodes = { @NamedAttributeNode("roles"),
						@NamedAttributeNode(value = "colleagues", subgraph = "User.colleagues") },
				subgraphs = { @NamedSubgraph(name = "User.colleagues",
						attributeNodes = { @NamedAttributeNode("colleagues"), @NamedAttributeNode("roles") }) }),
		@NamedEntityGraph(name = "User.deepGraph",
				attributeNodes = { @NamedAttributeNode("roles"),
						@NamedAttributeNode(value = "colleagues", subgraph = "User.colleagues") },
				subgraphs = {
						@NamedSubgraph(name = "User.colleagues",
								attributeNodes = { @NamedAttributeNode("roles"),
										@NamedAttributeNode(value = "colleagues",
												subgraph = "User.colleaguesOfColleagues") }),
						@NamedSubgraph(name = "User.colleaguesOfColleagues",
								attributeNodes = { @NamedAttributeNode("roles") }) }) })
@NamedQueries({ //
		@NamedQuery(name = "User.findByEmailAddress", //
				query = "SELECT u.* FROM User u WHERE u.emailAddress = ?1"), //
		@NamedQuery(name = "User.findByNamedQueryWithAliasInInvertedOrder", //
				query = "SELECT u.lastname AS lastname, u.firstname AS firstname FROM User u ORDER BY u.lastname ASC"),
		@NamedQuery(name = "User.findByNamedQueryWithConstructorExpression",
				query = "SELECT new org.springframework.data.mybatis.repository.sample.NameOnlyDto(u.firstname, u.lastname) from User u") })

@NamedStoredProcedureQueries({ //
		@NamedStoredProcedureQuery(name = "User.plus1", procedureName = "plus1inout",
				parameters = { @StoredProcedureParameter(mode = ParameterMode.IN, name = "arg", type = Integer.class),
						@StoredProcedureParameter(mode = ParameterMode.OUT, name = "res", type = Integer.class) }) //
})
@NamedStoredProcedureQuery(name = "User.plus1IO", procedureName = "plus1inout",
		parameters = { @StoredProcedureParameter(mode = ParameterMode.IN, name = "arg", type = Integer.class),
				@StoredProcedureParameter(mode = ParameterMode.OUT, name = "res", type = Integer.class) })

// Annotations for native Query with pageable
@SqlResultSetMappings({
		@SqlResultSetMapping(name = "SqlResultSetMapping.count", columns = @ColumnResult(name = "cnt")) })
@NamedNativeQueries({
		@NamedNativeQuery(name = "User.findByNativeNamedQueryWithPageable", resultClass = User.class,
				query = "SELECT * FROM SD_USER ORDER BY UCASE(firstname)"),
		@NamedNativeQuery(name = "User.findByNativeNamedQueryWithPageable.count",
				resultSetMapping = "SqlResultSetMapping.count", query = "SELECT count(*) AS cnt FROM SD_USER") })
@Table(name = "user")
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
	@JoinTable(name = "user_colleagues", joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
			inverseJoinColumns = @JoinColumn(name = "colleagues_id", referencedColumnName = "id"))
	private Set<User> colleagues;

	@ManyToMany
	@JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"),
			inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles;

	@ManyToOne
	@JoinColumn(name = "manager_id", referencedColumnName = "id")
	private User manager;

	@Embedded
	private Address address;

	@Lob
	private byte[] binaryData;

	@ElementCollection
	private Set<String> attributes;

	@Temporal(TemporalType.DATE)
	private Date dateOfBirth;

	/**
	 * Creates a new empty instance of {@code User}.
	 */
	public User() {
		this(null, null, null);
	}

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
	 * @return the id
	 */
	public Integer getId() {

		return this.id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Integer id) {

		this.id = id;
	}

	/**
	 * Returns the firstname.
	 * @return the firstname
	 */
	public String getFirstname() {

		return this.firstname;
	}

	/**
	 * Sets the firstname.
	 * @param firstname the firstname to set
	 */
	public void setFirstname(final String firstname) {

		this.firstname = firstname;
	}

	/**
	 * Returns the lastname.
	 * @return the lastname
	 */
	public String getLastname() {

		return this.lastname;
	}

	/**
	 * Sets the lastname.
	 * @param lastname the lastname to set
	 */
	public void setLastname(String lastname) {

		this.lastname = lastname;
	}

	/**
	 * @return the age
	 */
	public int getAge() {
		return this.age;
	}

	/**
	 * @param age the age to set
	 */
	public void setAge(int age) {
		this.age = age;
	}

	/**
	 * Returns the email address.
	 * @return the emailAddress
	 */
	public String getEmailAddress() {

		return this.emailAddress;
	}

	/**
	 * Sets the email address.
	 * @param emailAddress the emailAddress to set
	 */
	public void setEmailAddress(String emailAddress) {

		this.emailAddress = emailAddress;
	}

	/**
	 * @param active the active to set
	 */
	public void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * @return the active
	 */
	public boolean isActive() {
		return this.active;
	}

	/**
	 * Returns the user's roles.
	 * @return the roles
	 */
	public Set<Role> getRoles() {

		return this.roles;
	}

	/**
	 * Gives the user a role. Adding a role the user already owns is a no-op.
	 */
	public void addRole(Role role) {

		this.roles.add(role);
	}

	public void removeRole(Role role) {

		this.roles.remove(role);
	}

	/**
	 * Returns the colleagues of the user.
	 * @return the colleagues
	 */
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

	/**
	 * @return the manager
	 */
	public User getManager() {

		return this.manager;
	}

	/**
	 * @param manager the manager to set
	 */
	public void setManager(User manager) {

		this.manager = manager;
	}

	/**
	 * @return the createdAt
	 */
	public Date getCreatedAt() {
		return this.createdAt;
	}

	/**
	 * @return the address
	 */
	public Address getAddress() {
		return this.address;
	}

	/**
	 * @param address the address to set
	 */
	public void setAddress(Address address) {
		this.address = address;
	}

	/**
	 * @param binaryData the binaryData to set
	 */
	public void setBinaryData(byte[] binaryData) {
		this.binaryData = binaryData;
	}

	/**
	 * @return the binaryData
	 */
	public byte[] getBinaryData() {
		return this.binaryData;
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

	/**
	 * @return the attributes
	 */
	public Set<String> getAttributes() {
		return this.attributes;
	}

	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(Set<String> attributes) {
		this.attributes = attributes;
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
