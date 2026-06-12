package com.tony.erp.service;

import com.tony.erp.model.Role;
import com.tony.erp.model.User;
import com.tony.erp.repository.RoleRepository;
import com.tony.erp.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // ← org.springframework, NO jakarta

import java.util.Collections;
import java.util.Optional;

/**
 * Servicio de lógica de negocio para la gestión de usuarios del ERP.
 *
 * Responsabilidades:
 *   - Autenticación segura con BCrypt.
 *   - Registro de nuevos usuarios con validaciones de duplicados.
 *   - Actualización de contraseña por parte del administrador.
 *
 * IMPORTANTE sobre @Transactional:
 *   Se usa org.springframework.transaction.annotation.Transactional (NO jakarta.transaction).
 *   El de Jakarta no está integrado con el contexto de persistencia de Spring/Hibernate,
 *   lo que puede provocar que los cambios no se persistan correctamente en BD.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository  userRepository;
    private final RoleRepository  roleRepository;
    private final PasswordEncoder passwordEncoder;

    // -------------------------------------------------------------------------
    // Autenticación
    // -------------------------------------------------------------------------

    /**
     * Verifica si las credenciales proporcionadas son correctas.
     *
     * Proceso:
     *   1. Busca el usuario por nombre en la BD.
     *   2. Comprueba que la cuenta está activa.
     *   3. Compara la contraseña en texto plano con el hash BCrypt almacenado.
     *
     * @param username    Nombre de usuario.
     * @param rawPassword Contraseña en texto plano introducida en el login.
     * @return {@code true} si las credenciales son válidas y la cuenta está activa.
     */
    public boolean loginSeguro(String username, String rawPassword) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        if (!user.isActive()) {
            return false;
        }

        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    // -------------------------------------------------------------------------
    // Registro
    // -------------------------------------------------------------------------

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * Pasos:
     *   1. Valida que el nombre de usuario y el email no estén ya en uso.
     *   2. Cifra la contraseña con BCrypt antes de persistirla.
     *   3. Asigna el rol ROLE_EMPLOYEE por defecto.
     *   4. Guarda el usuario en la base de datos.
     *
     * @param newUser Objeto User con los datos del nuevo usuario (contraseña en texto plano).
     * @return El usuario guardado con ID asignado y contraseña cifrada.
     * @throws IllegalArgumentException si el nombre de usuario o el email ya están registrados.
     */
    @Transactional
    public User registrarUsuario(User newUser) {
        if (userRepository.findByUsername(newUser.getUsername()).isPresent()) {
            throw new IllegalArgumentException("El nombre de usuario ya está registrado.");
        }
        if (userRepository.findByEmail(newUser.getEmail()).isPresent()) {
            throw new IllegalArgumentException("El correo electrónico ya está registrado.");
        }

        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        newUser.setActive(true);

        Role employeeRole = roleRepository.findByName("ROLE_EMPLOYEE")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_EMPLOYEE")));
        newUser.setRoles(Collections.singleton(employeeRole));

        return userRepository.save(newUser);
    }

    // -------------------------------------------------------------------------
    // Administración
    // -------------------------------------------------------------------------

    /**
     * Permite al administrador cambiar la contraseña de una cuenta existente.
     *
     * Usa una query JPQL directa (@Modifying en el repositorio) en lugar de
     * cargar la entidad y hacer save(), para evitar problemas de caché de
     * Hibernate y garantizar que el UPDATE llega a la BD en la misma transacción.
     *
     * Por qué NO usamos save():
     *   Hibernate detecta si la entidad está "dirty" comparando con el snapshot
     *   que tiene en su caché de primer nivel. En algunos escenarios (especialmente
     *   con @Transactional de Jakarta en lugar de Spring), el flush no se ejecuta
     *   antes del commit, y el cambio se pierde silenciosamente.
     *
     * @param userId         ID del usuario a modificar.
     * @param newRawPassword Nueva contraseña en texto plano.
     * @throws EntityNotFoundException  si no existe ningún usuario con ese ID.
     * @throws IllegalArgumentException si la contraseña está vacía.
     */
    @Transactional
    public void updatePasswordByAdmin(Long userId, String newRawPassword) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("Usuario no encontrado con ID: " + userId);
        }

        if (newRawPassword == null || newRawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("La nueva contraseña no puede estar vacía.");
        }

        // Ciframos la contraseña y ejecutamos el UPDATE directo en BD
        String encodedPassword = passwordEncoder.encode(newRawPassword.trim());
        userRepository.updatePassword(userId, encodedPassword);
    }
}