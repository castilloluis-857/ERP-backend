package com.tony.erp.service;

import com.tony.erp.model.Category;
import com.tony.erp.model.Product;
import com.tony.erp.repository.CategoryRepository;
import com.tony.erp.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de lógica de negocio para la gestión del catálogo de productos.
 *
 * Aplica borrado lógico: los productos se marcan como inactivos ({@code active = false})
 * en lugar de eliminarse, para preservar el historial de ventas.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Busca productos activos. Si se proporciona un término de búsqueda, filtra por nombre.
     *
     * @param keyword Término opcional de búsqueda (puede ser nulo o vacío).
     * @return Lista de productos activos que coinciden con el filtro.
     */
    public List<Product> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return productRepository.findByActiveTrue();
        }
        return productRepository.findByActiveTrueAndNameContainingIgnoreCase(keyword);
    }

    /**
     * Obtiene un producto por su ID.
     *
     * @throws EntityNotFoundException si no existe ningún producto con ese ID.
     */
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + id));
    }

    /**
     * Crea un nuevo producto validando que la categoría asociada exista en la BD.
     *
     * @throws EntityNotFoundException si la categoría no existe.
     */
    @Transactional
    public Product saveProduct(Product product) {
        product.setCategory(resolveCategory(product.getCategory().getId()));
        return productRepository.save(product);
    }

    /**
     * Actualiza los campos de un producto existente.
     *
     * @throws EntityNotFoundException si el producto o la categoría no existen.
     */
    @Transactional
    public Product updateProduct(Long id, Product updatedData) {
        Product existing = getProductById(id);

        existing.setName(updatedData.getName());
        existing.setDescription(updatedData.getDescription());
        existing.setPrice(updatedData.getPrice());
        existing.setStock(updatedData.getStock());
        existing.setCategory(resolveCategory(updatedData.getCategory().getId()));

        return productRepository.save(existing);
    }

    /**
     * Realiza un borrado lógico del producto: lo marca como inactivo.
     *
     * @throws EntityNotFoundException si el producto no existe.
     */
    @Transactional
    public void softDeleteProduct(Long id) {
        Product product = getProductById(id);
        product.setActive(false);
        productRepository.save(product);
    }

    // -------------------------------------------------------------------------
    // Auxiliares privados
    // -------------------------------------------------------------------------

    /**
     * Verifica que la categoría con el ID dado exista en la base de datos.
     *
     * @throws EntityNotFoundException si la categoría no existe.
     */
    private Category resolveCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Categoría no encontrada con ID: " + categoryId));
    }
}
