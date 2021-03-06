package com.soapboxrace.core.jpa;

import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "USER")
@NamedQueries({ //
		@NamedQuery(name = "UserEntity.findAll", query = "SELECT obj FROM UserEntity obj"),
		@NamedQuery(name = "UserEntity.findByEmail", query = "SELECT obj FROM UserEntity obj WHERE obj.email = :email"),
		@NamedQuery(name = "UserEntity.findByVerifyToken",
				query = "SELECT obj FROM UserEntity obj WHERE obj.verifyToken = :token")
})
public class UserEntity {

	@Id
	@Column(name = "ID", nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "EMAIL", length = 255)
	private String email;

	@Column(name = "PASSWORD", length = 255)
	private String password;

	private String gameHardwareHash;

	@Column(name = "IP_ADDRESS")
	private String ipAddress;

	@OneToMany(mappedBy = "user", targetEntity = PersonaEntity.class)
	@Where(clause = "deletedAt IS NULL")
	private List<PersonaEntity> listOfProfile;

	@Column(name = "premium")
	private boolean premium;

	@Column(name = "isAdmin")
	private boolean isAdmin;

	@Column(name = "created")
	private LocalDateTime created;

	@Column(name = "lastLogin")
	private LocalDateTime lastLogin;

	private String userAgent;

	private String verifyToken;

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return this.id;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getEmail() {
		return this.email;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPassword() {
		return this.password;
	}

	public List<PersonaEntity> getListOfProfile() {
		return listOfProfile;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public boolean isPremium() {
		return premium;
	}

	public void setPremium(boolean premium) {
		this.premium = premium;
	}

	public boolean ownsPersona(Long id) {
		return this.listOfProfile.stream().anyMatch(p -> p.getPersonaId().equals(id));
	}

	public boolean isAdmin() {
		return isAdmin;
	}

	public void setAdmin(boolean admin) {
		isAdmin = admin;
	}

	public LocalDateTime getLastLogin() {
		return lastLogin;
	}

	public void setLastLogin(LocalDateTime lastLogin) {
		this.lastLogin = lastLogin;
	}

	public LocalDateTime getCreated() {
		return created;
	}

	public void setCreated(LocalDateTime created) {
		this.created = created;
	}

	public String getGameHardwareHash() {
		return gameHardwareHash;
	}

	public void setGameHardwareHash(String gameHardwareHash) {
		this.gameHardwareHash = gameHardwareHash;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public String getVerifyToken() {
		return verifyToken;
	}

	public void setVerifyToken(String verifyToken) {
		this.verifyToken = verifyToken;
	}
}
