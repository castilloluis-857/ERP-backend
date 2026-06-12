package com.tony.erp.repository;

import com.tony.erp.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Obtener todos los productos activos
    List<Product> findByActiveTrue();

    // Filtrar productos activos por nombre
    List<Product> findByActiveTrueAndNameContainingIgnoreCase(String name);
}