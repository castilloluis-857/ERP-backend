package com.tony.erp.repository;

import com.tony.erp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link User}.
 *
 * Incluye una query nativa de actualización de contraseña para garantizar
 * que el UPDATE llega directamente a la BD sin depender del ciclo de vida
 * del contexto de persistencia de Hibernate.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    /**
     * Actualiza directamente la contraseña (ya cifrada con BCrypt) de un usuario por su ID.
     *
     * Se usa @Modifying + @Query para garantizar que el UPDATE se ejecuta
     * inmediatamente en la BD, evitando problemas de caché del contexto
     * de persistencia de Hibernate.
     *
     * @param id             ID del usuario.
     * @param encodedPassword Hash BCrypt de la nueva contraseña.
     */
    @Modifying
    @Query("UPDATE User u SET u.password = :password WHERE u.id = :id")
    void updatePassword(@Param("id") Long id, @Param("password") String encodedPassword);
}