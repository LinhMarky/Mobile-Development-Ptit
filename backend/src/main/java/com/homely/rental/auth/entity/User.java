package com.homely.rental.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.homely.rental.common.entity.AbstractAuditingEntity;
import com.homely.rental.auth.constant.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class User extends AbstractAuditingEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", length = 255, nullable = false, unique = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @Column(name = "password", length = 200, nullable = false)
    @NotBlank(message = "Password cannot be blank")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(name = "full_name", length = 100, nullable = false)
    @NotBlank(message = "Full name is required")
    private String fullName;

    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(name = "email_verified")
    private boolean emailVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private java.util.Set<Role> roles = new java.util.HashSet<>();

    public boolean isHost() {
        if (roles == null) return false;
        return roles.stream().anyMatch(r -> r.getName() == RoleName.ROLE_HOST);
    }

    public boolean isAdmin() {
        if (roles == null) return false;
        return roles.stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
    }

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    @JsonIgnore
    private String refreshToken;

    @Column(name = "suspended")
    private boolean suspended = false;

    @Column(name = "suspend_reason", length = 500)
    private String suspendReason;

    @Version
    private int version;

    @OneToOne(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, fetch = FetchType.LAZY)
    @JsonIgnore
    private AccountLifecycleLock lifecycleLock;

    @PrePersist
    void createLifecycleLock() {
        if (lifecycleLock == null) lifecycleLock = new AccountLifecycleLock(this);
    }
}
