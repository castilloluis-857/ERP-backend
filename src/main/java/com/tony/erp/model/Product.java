package com.tony.erp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad JPA que representa un producto del catálogo del ERP.
 *
 * Lógica de borrado: los productos no se eliminan físicamente de la base de datos
 * para preservar la integridad histórica de las ventas. En su lugar se usa un
 * borrado lógico con el campo {@code active = false}.
 */
@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Column(nullable = false)
    private String name;

    private String description;

    @Min(value = 0, message = "El precio no puede ser negativo")
    private double price;

    @Min(value = 0, message = "El stock no puede ser menor a cero")
    private int stock;

    /**
     * Categoría a la que pertenece el producto. Cargada de forma EAGER
     * para que siempre esté disponible al serializar el producto a JSON.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    @NotNull(message = "La categoría es obligatoria")
    private Category category;

    /**
     * Indica si el producto está activo. Los productos inactivos no aparecen
     * en el catálogo pero sus datos se conservan en el histórico de ventas.
     */
    @Column(nullable = false)
    private boolean active = true;
}
