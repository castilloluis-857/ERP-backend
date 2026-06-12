package com.tony.erp.repository;

import com.tony.erp.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    // Corregido: Quitamos el List externo sobrante
    List<Client> findByActiveTrue();

    // Corregido: Quitamos el List externo sobrante
    List<Client> findByActiveTrueAndNameContainingIgnoreCaseOrActiveTrueAndNifContainingIgnoreCase(String name, String nif);


}