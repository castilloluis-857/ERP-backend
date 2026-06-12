package com.tony.erp.service;

import com.tony.erp.model.User;
import com.tony.erp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación de {@link UserDetailsService} requerida por Spring Security.
 *
 * Conecta la tabla "users" de la base de datos con el motor de autenticación de Spring.
 * Es invocada automáticamente por {@link com.tony.erp.config.JwtFilter} al validar cada petición.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Carga los datos del usuario desde la base de datos por su nombre de usuario.
     *
     * Mapea los roles de la entidad {@link User} a objetos {@link SimpleGrantedAuthority}
     * que Spring Security entiende para el control de acceso por roles.
     *
     * @param username Nombre de usuario a buscar.
     * @return Objeto {@link UserDetails} con credenciales y permisos.
     * @throws UsernameNotFoundException si el usuario no existe en la BD.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isActive(),   // enabled
                true,              // accountNonExpired
                true,              // credentialsNonExpired
                true,              // accountNonLocked
                authorities
        );
    }
}
