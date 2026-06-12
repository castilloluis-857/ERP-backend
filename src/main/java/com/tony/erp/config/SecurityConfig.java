package com.tony.erp.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración central de Spring Security para el ERP.
 *
 * Estrategia de seguridad:
 *   - Autenticación stateless mediante tokens JWT (sin sesiones HTTP).
 *   - CSRF deshabilitado porque los tokens JWT ya protegen contra ataques de este tipo.
 *   - Control de acceso por roles: ROLE_ADMIN tiene permisos completos;
 *     ROLE_EMPLOYEE puede leer y crear, pero no eliminar datos sensibles.
 *
 * Reglas de autorización (en orden de evaluación):
 *   1. /api/usuarios/login y /api/usuarios/registro → públicas (sin autenticar).
 *   2. Gestión de usuarios → solo ROLE_ADMIN.
 *   3. Eliminación de clientes → solo ROLE_ADMIN.
 *   4. Todo lo demás → cualquier usuario autenticado.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    /**
     * Define la cadena de filtros de seguridad HTTP con las reglas de autorización.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // No usamos CSRF porque la autenticación es stateless con JWT
                .csrf(csrf -> csrf.disable())

                // Configuración CORS delegada al bean corsConfigurationSource()
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Reglas de acceso por endpoint y método HTTP
                .authorizeHttpRequests(auth -> auth

                        // Rutas públicas: login, registro y endpoint de errores de Spring
                        .requestMatchers("/api/usuarios/login", "/api/usuarios/registro", "/error").permitAll()

                        // Módulo de inventario: cualquier usuario autenticado
                        .requestMatchers("/api/products", "/api/products/**").authenticated()
                        .requestMatchers("/api/categories", "/api/categories/**").authenticated()
                        // La exportación ahora está dentro del propio ProductController
                        .requestMatchers("/api/products/export/**").authenticated()

                        // Gestión de usuarios: solo administradores
                        .requestMatchers(HttpMethod.GET,    "/api/usuarios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,   "/api/usuarios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/usuarios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/usuarios/**").hasRole("ADMIN")

                        // Módulo de clientes: lectura y edición para todos; borrado solo para ADMIN
                        .requestMatchers(HttpMethod.GET,    "/api/clients", "/api/clients/**").authenticated()
                        .requestMatchers(HttpMethod.POST,   "/api/clients", "/api/clients/**").authenticated()
                        .requestMatchers(HttpMethod.PUT,    "/api/clients/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/clients/**").hasRole("ADMIN")

                        // Cualquier otra ruta requiere autenticación
                        .anyRequest().authenticated()
                )

                // Manejadores personalizados para errores de autenticación y autorización
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, ex) -> {
                            // 401: el token falta, es inválido o expiró
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No autorizado");
                        })
                        .accessDeniedHandler((request, response, ex) -> {
                            // 403: el usuario está autenticado pero no tiene el rol necesario
                            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso prohibido");
                        })
                )

                // Sesiones stateless: Spring nunca crea ni usa sesiones HTTP
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        // El filtro JWT debe ejecutarse antes del filtro de autenticación por defecto de Spring
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configuración CORS para permitir peticiones desde el cliente JavaFX (localhost).
     *
     * Nota: En producción, reemplaza {@code allowedOrigins("*")} por los orígenes
     * específicos de tu aplicación para mayor seguridad.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** Expone el AuthenticationManager como Bean para poder inyectarlo donde sea necesario. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /** Encoder de contraseñas BCrypt con factor de coste por defecto (10 rondas). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
