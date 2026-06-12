package com.tony.erp.repository;

import com.tony.erp.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la gestión de persistencia de la entidad Category en MySQL.
 * Provee los métodos CRUD automáticos y consultas personalizadas.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * SOLUCIONADO: Busca una categoría por su nombre ignorando mayúsculas y minúsculas.
     * Spring Data JPA generará automáticamente la consulta:
     * SELECT * FROM categories WHERE LOWER(name) = LOWER(?1)
     *
     * @param name Nombre de la categoría a buscar.
     * @return Un Optional que contiene la categoría si es encontrada.
     */
    Optional<Category> findByNameIgnoreCase(String name);

    Optional<Category> findById(int id);
}