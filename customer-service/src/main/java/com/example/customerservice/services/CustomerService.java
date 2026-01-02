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
        if (customer.getName() != null &&
                customer.getRut() != null &&
                customer.getPhone() != null &&
                customer.getEmail() != null) {

            if (customer.getStatus() == null) {
                customer.setStatus("Activo");
            }

            return customerRepository.save(customer);
        }
        throw new IllegalArgumentException("Faltan datos para poder registrar al cliente");
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

    // Crear cliente desde JWT si no existe
    public CustomerEntity checkAndCreateCustomer(Jwt jwt) {
        String rut = jwt.getClaimAsString("rut");

        return customerRepository.findByrut(rut).orElseGet(() -> {
            CustomerEntity newCustomer = new CustomerEntity();

            newCustomer.setRut(rut);
            newCustomer.setEmail(jwt.getClaimAsString("email"));
            newCustomer.setName(
                    jwt.getClaimAsString("given_name") + " " +
                            jwt.getClaimAsString("family_name")
            );
            newCustomer.setPhone(jwt.getClaimAsString("phone"));

            String birthDateString = jwt.getClaimAsString("birthdate");
            if (birthDateString != null) {
                newCustomer.setBirthDate(LocalDate.parse(birthDateString));
            }

            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null) {
                Object rolesObj = realmAccess.get("roles");
                if (rolesObj instanceof List<?> roles) {
                    newCustomer.setAdmin(roles.contains("ADMIN"));
                }
            }

            newCustomer.setStatus("Activo");

            // Reutiliza validación + setStatus por defecto
            return createCustomer(newCustomer);
        });
    }

}

