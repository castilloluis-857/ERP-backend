package com.tony.erp.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Entidad JPA que representa una línea de ítem dentro de una venta.
 *
 * Cada ítem contiene:
 *   - El producto vendido.
 *   - La cantidad vendida.
 *   - El precio unitario en el momento de la venta (congelado para conservar el histórico).
 *
 * {@code @JsonBackReference} evita el bucle infinito de serialización
 * con la lista de ítems en {@link Sale#items}.
 */
@Entity
@Table(name = "sale_items")
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Venta a la que pertenece este ítem. */
    @ManyToOne
    @JoinColumn(name = "sale_id", nullable = false)
    @JsonBackReference
    private Sale sale;

    /** Producto vendido. Se conserva la referencia aunque el producto sea desactivado posteriormente. */
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Cantidad de unidades vendidas en este ítem. */
    private Integer quantity;

    /**
     * Precio unitario en el momento de la venta.
     * Se almacena para que el historial no cambie si el precio del producto se modifica más tarde.
     */
    private BigDecimal unitPrice;

    public SaleItem() {}

    // -------------------------------------------------------------------------
    // Lógica calculada
    // -------------------------------------------------------------------------

    /**
     * Calcula el subtotal de este ítem: cantidad × precio unitario.
     * Devuelve cero si alguno de los valores es nulo.
     */
    public BigDecimal getSubtotal() {
        if (unitPrice != null && quantity != null) {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
        return BigDecimal.ZERO;
    }

    // Getters y Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Sale getSale() { return sale; }
    public void setSale(Sale sale) { this.sale = sale; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}
