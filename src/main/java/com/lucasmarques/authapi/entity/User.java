package com.lucasmarques.authapi.entity;

import com.lucasmarques.authapi.enums.UserRole;
import jakarta.persistence.*;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;
import lombok.ToString;


import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(nullable = false, unique = true)
    private String userName;

    @NotNull
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @ToString
    public class Usuario {
        private UUID id;
        private String nome;
        private UserRole role;

        @ToString.Exclude
        private String password;
    }
}
