package pl.travelapi.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "email", nullable = false, length = 150)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "display_name", nullable = false, length = 50)
	private String displayName;

	@Column(name = "bio", length = 300)
	private String bio;

	@Column(name = "is_public", nullable = false)
	private boolean publicProfile;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected User() {
	}

	private User(String email, String passwordHash, String displayName, Instant createdAt) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.displayName = displayName;
		this.createdAt = createdAt;
	}

	public static User create(String email, String passwordHash, String displayName, Instant createdAt) {
		return new User(email, passwordHash, displayName, createdAt);
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getBio() {
		return bio;
	}

	public boolean isPublicProfile() {
		return publicProfile;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
