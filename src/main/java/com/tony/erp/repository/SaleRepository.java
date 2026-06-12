package com.tony.erp.repository;

import com.tony.erp.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    // 🔑 AÑADE ESTA LÍNEA:
    List<Sale> findAllByOrderBySaleDateDesc();
}