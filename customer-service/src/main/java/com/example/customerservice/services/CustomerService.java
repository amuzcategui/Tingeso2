package com.example.customerservice.services;
import com.example.customerservice.entities.CustomerEntity;
import com.example.customerservice.repositories.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private RestTemplate restTemplate;

    public CustomerEntity createCustomer(CustomerEntity customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Cliente inválido");
        }

        if (customer.getRut() == null || customer.getRut().isBlank())
            throw new IllegalArgumentException("RUT requerido");

        if (customer.getName() == null || customer.getName().isBlank())
            throw new IllegalArgumentException("Nombre requerido");

        if (customer.getEmail() == null || customer.getEmail().isBlank())
            throw new IllegalArgumentException("Email requerido");

        // phone puede venir null desde Keycloak, no lo hacemos obligatorio
        // si quieres, lo normalizas:
        if (customer.getPhone() == null) customer.setPhone("");

        // defaults por si no vienen
        if (customer.getStatus() == null || customer.getStatus().isBlank()) customer.setStatus("Activo");
        if (customer.getQuantityLoans() < 0) customer.setQuantityLoans(0);

        return customerRepository.save(customer);
    }


    public CustomerEntity findByRutOrThrow(String rut) {
        return customerRepository.findByrut(rut)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + rut));
    }

    // Este método será llamado por loan-service vía HTTP
    public CustomerEntity setStatus(String rut, String status) {
        CustomerEntity customer = findByRutOrThrow(rut);
        customer.setStatus(status);
        return customerRepository.save(customer);
    }

    public List<CustomerEntity> findAll() {
        return customerRepository.findAll();
    }


    public CustomerEntity updateQuantityLoans(String rut, int delta) {
        CustomerEntity customer = findByRutOrThrow(rut);

        int newValue = customer.getQuantityLoans() + delta;
        if (newValue < 0) newValue = 0; // para no quedar negativo

        customer.setQuantityLoans(newValue);
        return customerRepository.save(customer);
    }


}

