package com.example.inventoryservice.services;


import com.example.inventoryservice.entities.ToolEntity;
import com.example.inventoryservice.repositories.ToolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

@Service
public class ToolService {

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private RestTemplate restTemplate;

    // Service name register in Eureka
    // (debes crear este endpoint en kardex-service)
    private static final String KARDEX_SERVICE_URL = "http://kardex-service/api/v1/kardex/movements";

    private final List<String> states = Arrays.asList(
            "Disponible", "Prestada", "En reparación", "Dada de baja"
    );

    // ------------------ Helpers ------------------

    private void validateNewTool(ToolEntity tool) {
        // Regla de negocio: nombre, categoría y valor de reposición obligatorios
        if (tool == null) throw new IllegalArgumentException("Herramienta inválida");

        if (tool.getName() == null || tool.getName().trim().isEmpty())
            throw new IllegalArgumentException("Nombre requerido");

        if (tool.getCategory() == null || tool.getCategory().trim().isEmpty())
            throw new IllegalArgumentException("Categoría requerida");

        if (tool.getToolValue() <= 0)
            throw new IllegalArgumentException("Valor de reposición inválido");

        if (tool.getStock() <= 0)
            throw new IllegalArgumentException("Stock debe ser mayor a 0");
    }

    private void validateQuantity(int qty, String msg) {
        if (qty <= 0) throw new IllegalArgumentException(msg);
    }

    private void validateState(String state) {
        if (state == null || !states.contains(state))
            throw new IllegalArgumentException("Estado inválido: " + state);
    }

    private void postKardexMovement(String rutPerson, String movementType, String toolName, int qty) {
        // Sin DTO: enviamos Map -> JSON
        Map<String, Object> body = new HashMap<>();
        body.put("rutCustomer", rutPerson);
        body.put("movementType", movementType);
        body.put("movementDate", LocalDate.now().toString()); // ISO string
        body.put("toolName", toolName);
        body.put("toolQuantity", qty);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(KARDEX_SERVICE_URL, HttpMethod.POST, req, Void.class);
        } catch (Exception e) {
            // Decide si quieres "no romper" la operación si kardex falla.
            // Por trazabilidad, normalmente conviene FALLAR.
            throw new RuntimeException("No se pudo registrar el movimiento en Kardex (kardex-service)", e);
        }
    }

    // ------------------ RF1.1 Registrar nuevas herramientas ------------------

    @Transactional
    public ToolEntity saveTool(ToolEntity tool, String rutPerson) {

        validateNewTool(tool);

        // Estado inicial (regla negocio: válido; y normalmente al registrar queda Disponible)
        tool.setInitialState("Disponible");
        validateState(tool.getInitialState());

        // Si existe misma herramienta (mismo nombre, categoría, valor, fee, estado Disponible),
        // sumo stock al registro existente.
        List<ToolEntity> existingTools = toolRepository.findByname(tool.getName());
        ToolEntity toolToSave = tool;
        boolean merged = false;

        for (ToolEntity existing : existingTools) {
            boolean same =
                    "Disponible".equals(existing.getInitialState()) &&
                            Objects.equals(existing.getCategory(), tool.getCategory()) &&
                            existing.getToolValue() == tool.getToolValue();

            if (same) {
                existing.setStock(existing.getStock() + tool.getStock());
                toolToSave = existing;
                merged = true;
                break;
            }
        }

        if (!merged) {
            // si es nuevo registro, ya quedó como Disponible arriba
        }

        ToolEntity savedTool = toolRepository.save(toolToSave);

        // Regla de negocio: registrar herramienta genera movimiento en Kardex
        postKardexMovement(rutPerson, "Ingreso", savedTool.getName(), tool.getStock());

        return savedTool;
    }

    // ------------------ RF1.2 Dar de baja herramientas ------------------
    // (tú lo validarás por front, acá solo usamos rutPerson para trazabilidad)

    @Transactional
    public ToolEntity deactivateTool(Long idTool, String rutPerson, int quantityToDeactivate) {

        ToolEntity tool = toolRepository.findByid(idTool);
        if (tool == null) throw new IllegalArgumentException("Herramienta no encontrada");

        validateQuantity(quantityToDeactivate, "La cantidad a dar de baja debe ser mayor que 0");

        if (quantityToDeactivate > tool.getStock()) {
            throw new IllegalArgumentException("No se puede dar de baja más unidades de las existentes");
        }

        // Reduce stock del registro actual
        tool.setStock(tool.getStock() - quantityToDeactivate);

        // Si quedó 0, marcamos estado Dada de baja (tu lógica original)
        if (tool.getStock() == 0) {
            tool.setInitialState("Dada de baja");
        }

        toolRepository.save(tool);

        // Registramos un nuevo “lote” de herramientas dadas de baja (tu lógica original)
        ToolEntity dtool = new ToolEntity();
        dtool.setName(tool.getName());
        dtool.setCategory(tool.getCategory());
        dtool.setToolValue(tool.getToolValue());
        dtool.setInitialState("Dada de baja");
        dtool.setStock(quantityToDeactivate);
        toolRepository.save(dtool);

        // Kardex
        postKardexMovement(rutPerson, "Baja", tool.getName(), quantityToDeactivate);

        return tool;
    }

    // ------------------ Marcar herramientas como disponibles (devolución / reingreso) ------------------

    @Transactional
    public ToolEntity availableTool(Long idTool, String rutPerson, int quantityToActivate) {

        ToolEntity tool = toolRepository.findByid(idTool);
        if (tool == null) throw new IllegalArgumentException("Herramienta no encontrada");

        validateQuantity(quantityToActivate, "La cantidad a activar debe ser mayor que 0");

        if (quantityToActivate > tool.getStock()) {
            throw new IllegalArgumentException("No se puede activar más unidades de las existentes en ese registro");
        }

        // Buscar si ya existe un registro Disponible con mismo nombre/categoría/valor/fee para sumar stock
        List<ToolEntity> existingTools = toolRepository.findByname(tool.getName());
        ToolEntity targetAvailable = null;

        for (ToolEntity existing : existingTools) {
            boolean same =
                    "Disponible".equals(existing.getInitialState()) &&
                            Objects.equals(existing.getCategory(), tool.getCategory()) &&
                            existing.getToolValue() == tool.getToolValue();

            if (same) {
                targetAvailable = existing;
                break;
            }
        }

        if (targetAvailable == null) {
            // Creamos un registro Disponible
            ToolEntity newAvailable = new ToolEntity();
            newAvailable.setName(tool.getName());
            newAvailable.setCategory(tool.getCategory());
            newAvailable.setToolValue(tool.getToolValue());
            newAvailable.setInitialState("Disponible");
            newAvailable.setStock(quantityToActivate);
            toolRepository.save(newAvailable);
        } else {
            targetAvailable.setStock(targetAvailable.getStock() + quantityToActivate);
            toolRepository.save(targetAvailable);
        }

        // Restar del registro origen (por ejemplo: Prestada / En reparación / Dada de baja)
        tool.setStock(tool.getStock() - quantityToActivate);
        if (tool.getStock() <= 0) {
            toolRepository.delete(tool);
        } else {
            toolRepository.save(tool);
        }

        postKardexMovement(rutPerson, "Devolución", tool.getName(), quantityToActivate);

        return tool;
    }

    // ------------------ Préstamo ------------------

    @Transactional
    public ToolEntity loanTool(Long idTool, String rutPerson, int quantityToLoan) {

        ToolEntity tool = toolRepository.findByid(idTool);
        if (tool == null) throw new IllegalArgumentException("Herramienta no encontrada");

        validateQuantity(quantityToLoan, "La cantidad a prestar debe ser mayor que 0");

        if (quantityToLoan > tool.getStock()) {
            throw new IllegalArgumentException("No se puede prestar más unidades de las existentes");
        }

        // Solo deberías prestar desde Disponible (si quieres forzar la regla)
        if (!"Disponible".equals(tool.getInitialState())) {
            throw new IllegalArgumentException("Solo se pueden prestar herramientas en estado Disponible");
        }

        // Reduce stock del registro Disponible
        tool.setStock(tool.getStock() - quantityToLoan);
        toolRepository.save(tool);

        // Crear registro “Prestada”
        ToolEntity ptool = new ToolEntity();
        ptool.setName(tool.getName());
        ptool.setCategory(tool.getCategory());
        ptool.setToolValue(tool.getToolValue());
        ptool.setInitialState("Prestada");
        ptool.setStock(quantityToLoan);
        toolRepository.save(ptool);

        postKardexMovement(rutPerson, "Préstamo", tool.getName(), quantityToLoan);

        return tool;
    }

    // ------------------ Reparación ------------------

    @Transactional
    public ToolEntity repairTool(Long idTool, String rutPerson, int quantityToRepair) {

        ToolEntity tool = toolRepository.findByid(idTool);
        if (tool == null) throw new IllegalArgumentException("Herramienta no encontrada");

        validateQuantity(quantityToRepair, "La cantidad a reparar debe ser mayor que 0");

        if (quantityToRepair > tool.getStock()) {
            throw new IllegalArgumentException("No se puede enviar a reparación más unidades de las existentes");
        }

        // Si quieres, puedes permitir reparación desde Disponible o Prestada, etc.
        // Aquí solo evitamos "Dada de baja"
        if ("Dada de baja".equals(tool.getInitialState())) {
            throw new IllegalArgumentException("No se puede reparar una herramienta dada de baja");
        }

        tool.setStock(tool.getStock() - quantityToRepair);
        toolRepository.save(tool);

        // Crear registro “En reparación”
        ToolEntity rtool = new ToolEntity();
        rtool.setName(tool.getName());
        rtool.setCategory(tool.getCategory());
        rtool.setToolValue(tool.getToolValue());
        rtool.setInitialState("En reparación");
        rtool.setStock(quantityToRepair);
        toolRepository.save(rtool);

        postKardexMovement(rutPerson, "Reparación", tool.getName(), quantityToRepair);

        return tool;
    }

    @Transactional(readOnly = true)
    public ToolEntity getToolById(Long idTool) {
        ToolEntity tool = toolRepository.findByid(idTool);
        if (tool == null) throw new IllegalArgumentException("Herramienta no encontrada");
        return tool;
    }

    @Transactional
    public ToolEntity updateToolValue(Long idTool, double newValue) {
        ToolEntity tool = toolRepository.findByid(idTool);
        if (tool == null) throw new IllegalArgumentException("Herramienta no encontrada");
        if (newValue <= 0) throw new IllegalArgumentException("Valor de reposición inválido");
        tool.setToolValue(newValue);
        return toolRepository.save(tool);
    }

    @Transactional(readOnly = true)
    public List<ToolEntity> searchByName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name es requerido");
        return toolRepository.findByname(name);
    }




}

