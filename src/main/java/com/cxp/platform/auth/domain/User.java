package com.cxp.platform.auth.domain;

import com.cxp.platform.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;


/**
 * Domain entity representing user.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    public enum Role {
        CITIZEN
    }
}
