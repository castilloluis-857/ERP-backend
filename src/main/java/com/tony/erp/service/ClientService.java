package com.tony.erp.service;

import com.tony.erp.model.Client;
import com.tony.erp.repository.ClientRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de lógica de negocio para la gestión de clientes del ERP.
 *
 * Aplica borrado lógico ({@code active = false}) para preservar el historial
 * de ventas vinculado a cada cliente.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientService {

    private final ClientRepository clientRepository;

    /** Devuelve todos los clientes activos. */
    public List<Client> getActiveClients() {
        return clientRepository.findByActiveTrue();
    }

    /**
     * Busca clientes activos por nombre o NIF.
     * Si el término está vacío, devuelve todos los clientes activos.
     */
    public List<Client> searchClients(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getActiveClients();
        }
        return clientRepository
                .findByActiveTrueAndNameContainingIgnoreCaseOrActiveTrueAndNifContainingIgnoreCase(
                        keyword, keyword);
    }

    /**
     * Obtiene un cliente por su ID.
     *
     * @throws EntityNotFoundException si no existe ningún cliente con ese ID.
     */
    public Client getClientById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con ID: " + id));
    }

    /**
     * Crea un nuevo cliente.
     * El NIF se normaliza a mayúsculas antes de persistir.
     */
    @Transactional
    public Client saveClient(Client client) {
        normalizeNif(client);
        return clientRepository.save(client);
    }

    /**
     * Actualiza los datos de un cliente existente.
     *
     * @throws EntityNotFoundException si el cliente no existe.
     */
    @Transactional
    public Client updateClient(Long id, Client updatedData) {
        Client existing = getClientById(id);

        existing.setName(updatedData.getName());
        existing.setNif(updatedData.getNif().toUpperCase().trim());
        existing.setEmail(updatedData.getEmail());
        existing.setPhone(updatedData.getPhone());

        return clientRepository.save(existing);
    }

    /**
     * Realiza un borrado lógico del cliente: lo marca como inactivo.
     *
     * @throws EntityNotFoundException si el cliente no existe.
     */
    @Transactional
    public void softDeleteClient(Long id) {
        Client client = getClientById(id);
        client.setActive(false);
        clientRepository.save(client);
    }

    // Normaliza el NIF a mayúsculas y sin espacios
    private void normalizeNif(Client client) {
        if (client.getNif() != null) {
            client.setNif(client.getNif().toUpperCase().trim());
        }
    }
}
