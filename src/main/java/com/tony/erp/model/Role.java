// ============================================================
// Role.java
// ============================================================
package com.tony.erp.model;

import jakarta.persistence.*;

/**
 * Entidad que representa un rol de usuario en el sistema (p.ej. ROLE_ADMIN, ROLE_EMPLOYEE).
 *
 * Los nombres de los roles siguen la convención de Spring Security:
 * deben comenzar con el prefijo "ROLE_" para que {@code hasRole("ADMIN")} funcione correctamente.
 */
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /** Nombre único del rol. Formato esperado: "ROLE_ADMIN", "ROLE_EMPLOYEE". */
    @Column(unique = true, nullable = false, length = 50)
    private String name;

    // Constructores

    public Role() {}

    public Role(String name) {
        this.name = name;
    }

    // Getters y Setters

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return name;
    }
}
