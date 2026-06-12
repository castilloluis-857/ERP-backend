package com.tony.erp.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA que representa una venta completa en el ERP.
 *
 * Una venta tiene:
 *   - Un cliente asociado obligatorio.
 *   - Una lista de ítems ({@link SaleItem}).
 *   - Un total calculado a partir de los ítems.
 *   - Un estado: "COMPLETED" (por defecto) o "CANCELLED".
 *
 * La relación con SaleItem usa {@code @JsonManagedReference} para evitar
 * bucles de serialización JSON infinitos con {@code @JsonBackReference} en SaleItem.
 */
@Entity
@Table(name = "sales")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Fecha y hora de la venta. Formato de serialización: "yyyy-MM-dd HH:mm:ss". */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime saleDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "total", nullable = false)
    private BigDecimal total;

    /**
     * Estado de la venta. Valores posibles:
     *   - "COMPLETED": venta procesada correctamente.
     *   - "CANCELLED": venta anulada; el stock ha sido reintegrado.
     */
    @Column(name = "status", nullable = false)
    private String status;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<SaleItem> items;

    // Constructor por defecto: inicializa la venta como completada y sin ítems

    public Sale() {
        this.saleDate = LocalDateTime.now();
        this.items    = new ArrayList<>();
        this.status   = "COMPLETED";
        this.total    = BigDecimal.ZERO;
    }

    // -------------------------------------------------------------------------
    // Lógica de negocio
    // -------------------------------------------------------------------------

    /**
     * Recalcula el total de la venta sumando (cantidad × precio unitario) de cada ítem.
     * Debe llamarse antes de persistir la venta para garantizar la consistencia del total.
     */
    public void calcularTotal() {
        this.total = items == null ? BigDecimal.ZERO :
            items.stream()
                 .filter(item -> item.getUnitPrice() != null && item.getQuantity() != null)
                 .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                 .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Agrega un ítem a la venta y establece la referencia bidireccional
     * para que JPA persista correctamente la relación.
     */
    public void addItem(SaleItem item) {
        items.add(item);
        item.setSale(this);
    }

    // Getters y Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDateTime saleDate) { this.saleDate = saleDate; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<SaleItem> getItems() { return items; }
    public void setItems(List<SaleItem> items) { this.items = items; }
}
