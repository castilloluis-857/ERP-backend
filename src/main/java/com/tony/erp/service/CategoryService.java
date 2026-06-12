package com.tony.erp.service;

import com.tony.erp.model.Category;
import com.tony.erp.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Servicio de lógica de negocio para la gestión de categorías de productos.
 *
 * Validaciones aplicadas:
 *   - El nombre no puede estar vacío.
 *   - No pueden existir dos categorías con el mismo nombre (sin distinguir mayúsculas).
 *   - No se puede eliminar una categoría que tenga productos asociados.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /** Devuelve todas las categorías registradas. */
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    /** Busca una categoría por su ID. */
    @Transactional(readOnly = true)
    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }

    /**
     * Guarda o actualiza una categoría aplicando validaciones de negocio.
     *
     * @throws IllegalArgumentException si el nombre está vacío o ya existe otra categoría con ese nombre.
     */
    @Transactional
    public Category saveCategory(Category category) {
        // Normalización: eliminamos espacios en blanco accidentales
        if (category.getName() != null) {
            category.setName(category.getName().trim());
        }

        if (category.getName() == null || category.getName().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio.");
        }

        // Verificamos que no exista otra categoría con el mismo nombre
        Optional<Category> existingOpt = categoryRepository.findByNameIgnoreCase(category.getName());
        if (existingOpt.isPresent()) {
            Long existingId = existingOpt.get().getId();
            Long incomingId = category.getId();
            boolean isNewRecord = (incomingId == null || incomingId == 0);

            // Es un duplicado si: (a) es nuevo y el nombre ya existe, o
            //                      (b) es edición pero el ID existente es distinto al que editamos
            if (isNewRecord || !existingId.equals(incomingId)) {
                throw new IllegalArgumentException(
                        "Ya existe una categoría con el nombre: " + category.getName());
            }
        }

        return categoryRepository.save(category);
    }

    /**
     * Elimina físicamente una categoría.
     *
     * Solo se permite si no tiene productos asociados, para mantener la integridad referencial.
     *
     * @throws IllegalArgumentException si la categoría no existe o tiene productos asociados.
     */
    @Transactional
    public void deleteById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La categoría que intentas eliminar no existe."));

        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            throw new IllegalArgumentException(
                    "No se puede eliminar la categoría porque tiene productos asociados. " +
                    "Mueve o elimina los productos antes de continuar.");
        }

        categoryRepository.deleteById(id);
    }
}
