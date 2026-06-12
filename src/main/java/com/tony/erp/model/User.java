package com.tony.erp.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Entidad JPA que representa un usuario del sistema ERP.
 *
 * Relación con roles: ManyToMany (un usuario puede tener varios roles,
 * aunque en la práctica actual se asigna uno solo por usuario).
 *
 * Seguridad: la contraseña nunca se incluye en toString() para evitar
 * que aparezca en logs o trazas del sistema.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "password")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre de usuario único para el login. Máximo 50 caracteres. */
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    /** Correo electrónico único. Máximo 100 caracteres. */
    @Column(unique = true, nullable = false, length = 100)
    private String email;

    /** Contraseña almacenada como hash BCrypt (nunca en texto plano). */
    @Column(nullable = false, length = 255)
    private String password;

    /**
     * Indica si la cuenta está activa.
     * Los usuarios inactivos no pueden iniciar sesión aunque sus credenciales sean correctas.
     */
    private boolean active = true;

    /**
     * Roles asignados al usuario. Se cargan de forma EAGER porque Spring Security
     * los necesita en el momento de la autenticación, dentro del mismo contexto transaccional.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns        = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
}
