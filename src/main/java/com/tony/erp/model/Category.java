package com.tony.erp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA que representa una categoría de producto (p.ej. "Electrónica", "Alimentación").
 *
 * Relación:
 *   Una categoría → muchos productos (OneToMany).
 *   La relación inversa está en {@link Product#category}.
 *
 * {@code @JsonIgnore} en la lista de productos evita bucles de serialización JSON infinitos
 * cuando Jackson intenta serializar Category → Products → Category → ...
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre único de la categoría. Sensible a mayúsculas en la búsqueda (ignorado en BD). */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Product> products = new ArrayList<>();

    // Constructores

    public Category() {}

    public Category(String name) {
        this.name = name;
    }

    public Category(Long id, String name) {
        this.id   = id;
        this.name = name;
    }

    // Getters y Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<Product> getProducts() { return products; }
    public void setProducts(List<Product> products) { this.products = products; }

    /**
     * Devuelve el nombre de la categoría.
     * Necesario para que los ComboBox de JavaFX muestren el nombre directamente.
     */
    @Override
    public String toString() {
        return name;
    }
}
