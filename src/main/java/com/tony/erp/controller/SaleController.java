package com.tony.erp.controller;

import com.tony.erp.model.Sale;
import com.tony.erp.service.SaleService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de ventas.
 *
 * Endpoints:
 *   GET  /api/sales               → historial completo (ordenado por fecha DESC)
 *   GET  /api/sales/{id}          → detalle de una venta
 *   POST /api/sales               → registra una nueva venta
 *   PUT  /api/sales/{id}/cancel   → cancela una venta y reintegra el stock
 *   GET  /api/sales/{id}/pdf      → descarga la factura en PDF
 *   GET  /api/sales/{id}/excel    → descarga la factura en CSV (abre en Excel)
 *
 * Nota sobre el estado de las ventas:
 *   El estado lo impone siempre el backend ("COMPLETED" al crear, "CANCELLED" al cancelar).
 *   El frontend no debe enviar el campo status — se ignora aunque llegue.
 */
@RestController
@RequestMapping("/api/sales")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @GetMapping
    public ResponseEntity<List<Sale>> getAll() {
        return ResponseEntity.ok(saleService.getAllSales());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(saleService.getSaleById(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Procesa y registra una nueva venta.
     *
     * El backend fuerza siempre status = "COMPLETED" al crear,
     * independientemente de lo que envíe el frontend.
     */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Sale sale) {
        // El backend es el único que decide el estado inicial — nunca el cliente
        sale.setStatus("COMPLETED");
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(saleService.createSale(sale));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error inesperado al procesar la venta: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(saleService.cancelSale(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<?> exportPdf(@PathVariable Long id) {
        try {
            byte[] pdfBytes = saleService.generateSalePdf(id);
            if (pdfBytes == null || pdfBytes.length == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("No se pudo generar el PDF.");
            }
            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=\"Factura_" + id + ".pdf\"")
                    .body(pdfBytes);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Exporta la factura de una venta en formato CSV compatible con Excel.
     *
     * Content-Type: text/csv con BOM UTF-8.
     * El nombre del archivo usa extensión .csv para que Excel lo abra
     * directamente sin mostrar el asistente de importación.
     *
     * Por qué NO usamos .xlsx ni application/vnd.openxmlformats:
     *   El contenido generado es CSV texto plano, no un binario Excel.
     *   Enviar el Content-Type de Excel con contenido CSV provoca que
     *   Excel muestre "archivo dañado" al intentar abrirlo.
     */
    @GetMapping("/{id}/excel")
    public ResponseEntity<?> exportExcel(@PathVariable Long id) {
        try {
            byte[] csvBytes = saleService.generateSaleExcel(id);
            if (csvBytes == null || csvBytes.length == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("No se pudo generar el archivo.");
            }
            return ResponseEntity.ok()
                    // ✅ CORRECTO: text/csv para contenido CSV, no Excel binario
                    .header("Content-Type", "text/csv; charset=UTF-8")
                    .header("Content-Disposition", "attachment; filename=\"Factura_" + id + ".csv\"")
                    .body(csvBytes);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
