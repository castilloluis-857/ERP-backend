package com.tony.erp.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.tony.erp.model.Client;
import com.tony.erp.model.Product;
import com.tony.erp.model.Sale;
import com.tony.erp.model.SaleItem;
import com.tony.erp.repository.ClientRepository;
import com.tony.erp.repository.ProductRepository;
import com.tony.erp.repository.SaleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Servicio de lógica de negocio para la gestión de ventas del ERP.
 *
 * Responsabilidades:
 *   - Crear ventas validando stock disponible y actualizando el inventario.
 *   - Cancelar ventas reintegrando el stock de los productos.
 *   - Generar reportes de factura en formato PDF e informe CSV/Excel.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SaleService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Color BLUE_HEADER = new Color(33, 150, 243);

    private final SaleRepository    saleRepository;
    private final ClientRepository  clientRepository;
    private final ProductRepository productRepository;

    // -------------------------------------------------------------------------
    // Consultas
    // -------------------------------------------------------------------------

    /** Devuelve todas las ventas ordenadas de más reciente a más antigua. */
    public List<Sale> getAllSales() {
        return saleRepository.findAllByOrderBySaleDateDesc();
    }

    /**
     * Obtiene una venta por su ID.
     *
     * @throws EntityNotFoundException si no existe ninguna venta con ese ID.
     */
    public Sale getSaleById(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada con ID: " + id));
    }

    // -------------------------------------------------------------------------
    // Operaciones de escritura
    // -------------------------------------------------------------------------

    /**
     * Crea una nueva venta procesando cada ítem del carrito:
     *   1. Valida que el cliente y los productos existan.
     *   2. Comprueba que haya stock suficiente para cada producto.
     *   3. Descuenta el stock vendido.
     *   4. Asigna el precio unitario actual si no se envió uno desde el frontend.
     *   5. Calcula y persiste el total.
     *
     * @throws EntityNotFoundException  si el cliente o algún producto no existe.
     * @throws IllegalArgumentException si no hay stock suficiente para algún producto.
     */
    @Transactional
    public Sale createSale(Sale sale) {
        Client client = clientRepository.findById(sale.getClient().getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Cliente no encontrado con ID: " + sale.getClient().getId()));
        sale.setClient(client);

        for (SaleItem item : sale.getItems()) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Producto no encontrado con ID: " + item.getProduct().getId()));

            if (product.getStock() < item.getQuantity()) {
                throw new IllegalArgumentException(
                        "Stock insuficiente para '" + product.getName() + "'. " +
                                "Disponible: " + product.getStock() + ", Solicitado: " + item.getQuantity());
            }

            // Descontamos el stock vendido
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);

            item.setProduct(product);
            item.setSale(sale);

            // Si el frontend no envió precio, tomamos el precio actual del producto
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                item.setUnitPrice(BigDecimal.valueOf(product.getPrice()));
            }
        }

        sale.calcularTotal();
        return saleRepository.save(sale);
    }

    /**
     * Cancela una venta activa y reintegra el stock de cada ítem al inventario.
     *
     * @throws EntityNotFoundException  si la venta no existe.
     * @throws IllegalStateException   si la venta ya estaba cancelada.
     */
    @Transactional
    public Sale cancelSale(Long id) {
        Sale sale = getSaleById(id);

        if ("CANCELLED".equals(sale.getStatus())) {
            throw new IllegalStateException("La venta ya se encuentra cancelada.");
        }

        // Devolvemos el stock de cada producto vendido
        for (SaleItem item : sale.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }

        sale.setStatus("CANCELLED");
        return saleRepository.save(sale);
    }

    // -------------------------------------------------------------------------
    // Exportación de informes
    // -------------------------------------------------------------------------

    /**
     * Genera el PDF de la factura de una venta usando la librería OpenPDF (iText fork).
     *
     * El documento incluye:
     *   - Datos de la factura (número, fecha, estado, cliente).
     *   - Tabla detallada de ítems con subtotales.
     *   - Total a pagar resaltado.
     *
     * @param saleId ID de la venta.
     * @return Array de bytes del PDF generado, o {@code null} si ocurre un error.
     */
    public byte[] generateSalePdf(Long saleId) {
        Sale sale = getSaleById(saleId);
        ensureTotalCalculated(sale);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fuentes del documento
            Font titleFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.DARK_GRAY);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font boldFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);

            // Título principal
            Paragraph title = new Paragraph("FACTURA DE VENTA", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Bloque de información de la factura
            document.add(buildInfoBlock(sale, normalFont, boldFont));

            // Tabla de ítems
            document.add(buildItemsTable(sale, headerFont, normalFont));

            // Total a pagar
            Paragraph totalParagraph = new Paragraph();
            totalParagraph.setAlignment(Element.ALIGN_RIGHT);
            totalParagraph.add(new Chunk("TOTAL A PAGAR: ", boldFont));
            totalParagraph.add(new Chunk(
                    String.format("%.2f €", sale.getTotal()),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BLUE_HEADER)));
            document.add(totalParagraph);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return out.toByteArray();
    }

    /**
     * Genera un informe CSV con los datos de una venta.
     * El archivo puede abrirse directamente en Excel.
     *
     * @param saleId ID de la venta.
     * @return Array de bytes del CSV, o {@code null} si ocurre un error.
     */
    public byte[] generateSaleExcel(Long saleId) {
        Sale sale = getSaleById(saleId);
        ensureTotalCalculated(sale);

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            StringBuilder csv = new StringBuilder();

            csv.append('\ufeff'); // BOM UTF-8: tildes correctas en Excel
            csv.append("FACTURA DE VENTA N°;").append(sale.getId()).append("\n");
            csv.append("Fecha;").append(sale.getSaleDate()).append("\n");
            csv.append("Cliente;")
                    .append(sale.getClient() != null ? sale.getClient().getName() : "Anónimo")
                    .append("\n\n");

            csv.append("Producto;Cantidad;Precio Unitario;Subtotal\n");
            for (SaleItem item : sale.getItems()) {
                csv.append(item.getProduct().getName()).append(";")
                        .append(item.getQuantity()).append(";")
                        .append(item.getUnitPrice()).append(";")
                        .append(item.getSubtotal()).append("\n");
            }
            csv.append("\n;;TOTAL:;").append(sale.getTotal()).append(" €\n");

            out.write(csv.toString().getBytes("UTF-8"));
            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Auxiliares privados de generación de PDF
    // -------------------------------------------------------------------------

    /** Si el total es nulo o cero (venta antigua), lo recalcula a partir de los ítems. */
    private void ensureTotalCalculated(Sale sale) {
        if (sale.getTotal() == null || sale.getTotal().compareTo(BigDecimal.ZERO) == 0) {
            sale.calcularTotal();
        }
    }

    /** Construye el bloque de información de cabecera del PDF. */
    private Paragraph buildInfoBlock(Sale sale, Font normalFont, Font boldFont) {
        Paragraph info = new Paragraph();

        addInfoLine(info, "Nº Factura: ",   String.valueOf(sale.getId()),    boldFont, normalFont);
        addInfoLine(info, "Estado: ",       sale.getStatus(),                boldFont, normalFont);
        addInfoLine(info, "Fecha: ",        formatDate(sale),                boldFont, normalFont);

        if (sale.getClient() != null) {
            addInfoLine(info, "Cliente: ", sale.getClient().getName(), boldFont, normalFont);
            addInfoLine(info, "NIF/DNI: ", sale.getClient().getNif(),  boldFont, normalFont);
        } else {
            addInfoLine(info, "Cliente: ", "Anónimo", boldFont, normalFont);
        }

        info.setSpacingAfter(20);
        return info;
    }

    /** Añade una línea etiqueta–valor al párrafo de información. */
    private void addInfoLine(Paragraph p, String label, String value, Font boldFont, Font normalFont) {
        p.add(new Chunk(label, boldFont));
        p.add(new Phrase(value + "\n", normalFont));
    }

    /** Construye la tabla de ítems de la factura. */
    private PdfPTable buildItemsTable(Sale sale, Font headerFont, Font normalFont) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4f, 1.5f, 2f, 2.5f});

        // Cabeceras de la tabla
        for (String header : new String[]{"Producto", "Cantidad", "Precio Unit.", "Subtotal"}) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(BLUE_HEADER);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6);
            table.addCell(cell);
        }

        // Filas de datos
        for (SaleItem item : sale.getItems()) {
            table.addCell(new PdfPCell(new Phrase(item.getProduct().getName(), normalFont)));

            PdfPCell qtyCell = new PdfPCell(new Phrase(String.valueOf(item.getQuantity()), normalFont));
            qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(qtyCell);

            PdfPCell priceCell = new PdfPCell(new Phrase(String.format("%.2f €", item.getUnitPrice()), normalFont));
            priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(priceCell);

            PdfPCell subtotalCell = new PdfPCell(new Phrase(String.format("%.2f €", item.getSubtotal()), normalFont));
            subtotalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(subtotalCell);
        }

        table.setSpacingAfter(20);
        return table;
    }

    /** Formatea la fecha de la venta, devolviendo "N/A" si es nula. */
    private String formatDate(Sale sale) {
        return sale.getSaleDate() != null
                ? sale.getSaleDate().format(DATE_FORMATTER)
                : "N/A";
    }
}
