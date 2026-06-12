package com.tony.erp.controller;

import com.tony.erp.model.Client;
import com.tony.erp.service.ClientService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de clientes.
 *
 * Endpoints:
 *   GET    /api/clients           → lista clientes activos (con filtro opcional ?search=)
 *   GET    /api/clients/active    → lista solo clientes activos (alias explícito)
 *   GET    /api/clients/{id}      → obtiene un cliente por ID
 *   POST   /api/clients           → crea un nuevo cliente
 *   PUT    /api/clients/{id}      → actualiza un cliente
 *   DELETE /api/clients/{id}      → borrado lógico (solo ROLE_ADMIN según SecurityConfig)
 */
@RestController
@RequestMapping("/api/clients")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    public ResponseEntity<List<Client>> getAll(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(clientService.searchClients(search));
    }

    /**
     * Este endpoint está ANTES del @GetMapping("/{id}") para que Spring evalúe
     * "/active" como texto literal antes de intentar leerlo como un Long.
     */
    @GetMapping("/active")
    public ResponseEntity<List<Client>> getActive() {
        return ResponseEntity.ok(clientService.getActiveClients());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(clientService.getClientById(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Client client) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.saveClient(client));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody Client client) {
        try {
            return ResponseEntity.ok(clientService.updateClient(id, client));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            clientService.softDeleteClient(id);
            return ResponseEntity.ok("Cliente con ID " + id + " desactivado correctamente.");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
