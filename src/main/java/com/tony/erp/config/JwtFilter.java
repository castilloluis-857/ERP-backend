package com.tony.erp.config;

import com.tony.erp.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de seguridad que se ejecuta exactamente una vez por cada petición HTTP entrante.
 *
 * Responsabilidad:
 *   1. Leer la cabecera "Authorization: Bearer <token>".
 *   2. Validar criptográficamente el token con {@link JwtProvider}.
 *   3. Si el token es válido, registrar al usuario en el contexto de Spring Security,
 *      lo que le otorga acceso a los endpoints protegidos.
 *
 * Si no hay token, o es inválido, simplemente deja pasar la petición sin autenticar.
 * Spring Security se encargará de rechazarla si la ruta lo requiere.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX        = "Bearer ";

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService userDetailsService;

    /**
     * @Lazy en {@code userDetailsService} rompe el ciclo de dependencias circular
     * que se produce durante la inicialización de Spring Security.
     */
    public JwtFilter(JwtProvider jwtProvider, @Lazy CustomUserDetailsService userDetailsService) {
        this.jwtProvider       = jwtProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        // Solo procesamos cabeceras con formato "Bearer <token>"
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());

            if (jwtProvider.validarToken(token)) {
                String username = jwtProvider.obtenerUsuarioDesdeToken(token);

                // Autenticamos solo si obtenemos un usuario válido y la sesión no estaba ya autenticada
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Registramos la autenticación en el contexto de seguridad de la petición actual
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        // Continuamos con el siguiente filtro en la cadena, independientemente del resultado
        filterChain.doFilter(request, response);
    }
}
