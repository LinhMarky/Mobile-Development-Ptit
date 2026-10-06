package com.homely.rental.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A separate lock row keeps account lifecycle locks away from user foreign keys. */
@Entity
@Table(name = "account_lifecycle_locks")
@Getter
@NoArgsConstructor
public class AccountLifecycleLock {
    @Id
    @Column(name = "user_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public AccountLifecycleLock(User user) { this.user = user; }
}
