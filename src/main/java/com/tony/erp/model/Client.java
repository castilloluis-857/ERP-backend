package com.tony.erp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad JPA que representa a un cliente del ERP.
 *
 * El campo {@code nif} se almacena siempre en mayúsculas (normalización aplicada en el servicio).
 * El borrado es lógico: {@code active = false} en lugar de eliminar el registro,
 * para preservar el historial de ventas asociado al cliente.
 */
@Entity
@Table(name = "clients")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** NIF/DNI del cliente. Único, obligatorio y normalizado a mayúsculas. */
    @NotBlank(message = "El NIF es obligatorio")
    @Size(max = 20, message = "El NIF no puede superar los 20 caracteres")
    @Column(nullable = false, unique = true, length = 20)
    private String nif;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 255, message = "El nombre no puede superar los 255 caracteres")
    @Column(nullable = false, length = 255)
    private String name;

    @Email(message = "El formato del email no es válido")
    @Size(max = 255)
    @Column(length = 255)
    private String email;

    @Size(max = 255)
    @Column(length = 255)
    private String phone;

    /** {@code true} = cliente activo; {@code false} = cliente dado de baja lógicamente. */
    @Column(nullable = false)
    private boolean active = true;
}
