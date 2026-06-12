package com.tony.erp.controller;

import com.tony.erp.config.JwtProvider;
import com.tony.erp.model.User;
import com.tony.erp.repository.UserRepository;
import com.tony.erp.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador REST para la autenticación y administración de usuarios.
 *
 * Endpoints públicos (no requieren token):
 *   POST /api/usuarios/login     → devuelve JWT si las credenciales son correctas.
 *   POST /api/usuarios/registro  → registra un nuevo empleado.
 *
 * Endpoints protegidos (solo ROLE_ADMIN):
 *   GET    /api/usuarios            → lista todos los usuarios.
 *   PUT    /api/usuarios/admin/{id} → cambia la contraseña de un usuario.
 *   DELETE /api/usuarios/{id}       → elimina un usuario del sistema.
 */
@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UserController {

    private final UserService    userService;
    private final UserRepository userRepository;
    private final JwtProvider    jwtProvider;

    // -------------------------------------------------------------------------
    // Autenticación
    // -------------------------------------------------------------------------

    /**
     * Procesa el login.
     * Si las credenciales son correctas, devuelve el token JWT, el rol y el username.
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (!userService.loginSeguro(username, password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(buildResponse(false, "El usuario o la contraseña son incorrectos.", null));
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(buildResponse(false, "Usuario no encontrado.", null));
        }

        User   user = userOpt.get();
        String rol  = user.getRoles().isEmpty()
                ? "ROLE_EMPLOYEE"
                : user.getRoles().iterator().next().getName();

        Map<String, Object> body = buildResponse(true, "Autenticación exitosa.", null);
        body.put("token",   jwtProvider.generarToken(username, rol));
        body.put("rol",     rol);
        body.put("usuario", username);

        return ResponseEntity.ok(body);
    }

    // -------------------------------------------------------------------------
    // Registro
    // -------------------------------------------------------------------------

    @PostMapping("/registro")
    public ResponseEntity<Map<String, Object>> registrar(@RequestBody User newUser) {
        try {
            User saved = userService.registrarUsuario(newUser);
            Map<String, Object> body = buildResponse(true, "Usuario registrado correctamente.", null);
            body.put("username", saved.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildResponse(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(buildResponse(false, "Error inesperado al registrar el usuario.", null));
        }
    }

    // -------------------------------------------------------------------------
    // Administración (solo ROLE_ADMIN)
    // -------------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<List<User>> listarTodos() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    /**
     * Cambia la contraseña de un usuario existente.
     *
     * El payload esperado es: {"nuevaPasswordPlain": "nueva123"}
     *
     * Delega en UserService.updatePasswordByAdmin(), que usa una query JPQL
     * directa para garantizar que el UPDATE llega correctamente a la BD.
     *
     * @param id      ID del usuario a modificar.
     * @param payload JSON con la clave "nuevaPasswordPlain".
     */
    @PutMapping("/admin/{id}")
    public ResponseEntity<Map<String, Object>> cambiarPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        try {
            String newPassword = payload.get("nuevaPasswordPlain");
            userService.updatePasswordByAdmin(id, newPassword);
            return ResponseEntity.ok(buildResponse(true, "Contraseña actualizada correctamente.", null));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildResponse(false, e.getMessage(), null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(buildResponse(false, e.getMessage(), null));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> eliminar(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildResponse(false, "El usuario con ID " + id + " no existe.", null));
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok(buildResponse(true, "Usuario eliminado correctamente.", null));
    }

    // -------------------------------------------------------------------------
    // Auxiliares privados
    // -------------------------------------------------------------------------

    private Map<String, Object> buildResponse(boolean success, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("exito",   success);
        response.put("mensaje", message);
        if (data != null) response.put("data", data);
        return response;
    }
}