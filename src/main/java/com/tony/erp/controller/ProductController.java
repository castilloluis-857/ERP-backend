package com.tony.erp.controller;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.tony.erp.model.Product;
import com.tony.erp.repository.ProductRepository;
import com.tony.erp.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controlador REST para la gestión y exportación del catálogo de productos.
 *
 * Agrupa en un único controlador tanto el CRUD de productos como la
 * exportación del catálogo, evitando una clase ExportController separada
 * para operaciones del mismo dominio.
 *
 * Endpoints CRUD (requieren autenticación JWT):
 *   GET    /api/products             → lista productos activos (filtro opcional ?search=)
 *   GET    /api/products/{id}        → obtiene un producto por ID
 *   POST   /api/products             → crea un nuevo producto
 *   PUT    /api/products/{id}        → actualiza un producto existente
 *   DELETE /api/products/{id}        → borrado lógico (active=false)
 *
 * Endpoints de exportación (requieren autenticación JWT):
 *   GET    /api/products/export/pdf  → descarga el catálogo completo en PDF
 *   GET    /api/products/export/excel→ descarga el catálogo completo en CSV (Excel)
 */
@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProductController {

    private static final Color     HEADER_COLOR   = Color.DARK_GRAY;
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final ProductService    productService;
    private final ProductRepository productRepository; // usado solo para exportación (acceso directo optimizado)

    // =========================================================================
    // CRUD
    // =========================================================================

    @GetMapping
    public ResponseEntity<List<Product>> getAll(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(productService.searchProducts(search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(productService.getProductById(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Product product) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(productService.saveProduct(product));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody Product product) {
        try {
            return ResponseEntity.ok(productService.updateProduct(id, product));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            productService.softDeleteProduct(id);
            return ResponseEntity.ok("Producto con ID " + id + " desactivado correctamente.");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // =========================================================================
    // EXPORTACIÓN
    // =========================================================================

    /**
     * Genera y descarga un PDF con el catálogo de productos activos.
     *
     * Incluye: título, fecha de generación y tabla con ID, nombre, precio y stock.
     *
     * @return Bytes del PDF listos para descarga, o 500 si falla la generación.
     */
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            List<Product> products = productRepository.findByActiveTrue();

            Document doc = new Document(PageSize.A4, 36, 36, 40, 40);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // Título y subtítulo
            Font titleFont    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, HEADER_COLOR);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
            Font headerFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
            Font bodyFont     = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

            Paragraph title = new Paragraph("Reporte General de Inventario", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            Paragraph subtitle = new Paragraph(
                    "Generado el: " + LocalDateTime.now().format(DATE_FMT) + " | ERP Tony", subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20);
            doc.add(subtitle);

            // Tabla de productos
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1f, 3f, 1.5f, 1.5f});

            for (String header : new String[]{"ID", "Producto", "Precio Unitario", "Stock Actual"}) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(HEADER_COLOR);
                cell.setPadding(8);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            for (Product p : products) {
                table.addCell(new PdfPCell(new Phrase(String.valueOf(p.getId()), bodyFont)));
                table.addCell(new PdfPCell(new Phrase(
                        p.getName() != null ? p.getName() : "Sin nombre", bodyFont)));
                table.addCell(new PdfPCell(new Phrase(
                        String.format("%.2f €", p.getPrice()), bodyFont)));
                table.addCell(new PdfPCell(new Phrase(
                        String.valueOf(p.getStock()), bodyFont)));
            }

            doc.add(table);
            doc.close();

            return ResponseEntity.ok()
                    .headers(downloadHeaders("productos.pdf"))
                    .body(out.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Genera y descarga un CSV del catálogo de productos activos, optimizado para Excel.
     *
     * Incluye BOM UTF-8 para que Excel detecte correctamente tildes y eñes.
     * Separador punto y coma (;) compatible con la configuración regional española.
     *
     * @return Bytes del CSV listos para descarga, o 500 si falla la generación.
     */
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel() {
        try {
            List<Product> products = productRepository.findByActiveTrue();

            StringBuilder csv = new StringBuilder();
            csv.append('\ufeff'); // BOM UTF-8: necesario para tildes en Excel
            csv.append("ID;Producto;Precio Unitario;Stock Actual\n");

            for (Product p : products) {
                String name = (p.getName() != null)
                        ? p.getName().replace(";", ",").replace("\n", " ")
                        : "Sin nombre";
                csv.append(p.getId()).append(";")
                        .append(name).append(";")
                        .append(String.format("%.2f", p.getPrice())).append(";")
                        .append(p.getStock()).append("\n");
            }

            return ResponseEntity.ok()
                    .headers(downloadHeaders("productos.csv"))
                    .body(csv.toString().getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =========================================================================
    // Auxiliares privados
    // =========================================================================

    /**
     * Construye las cabeceras HTTP para que el cliente descargue el archivo
     * en lugar de mostrarlo en pantalla.
     */
    private HttpHeaders downloadHeaders(String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        return headers;
    }
}
