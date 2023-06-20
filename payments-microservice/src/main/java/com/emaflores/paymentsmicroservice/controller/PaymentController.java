package com.emaflores.paymentsmicroservice.controller;

import com.emaflores.paymentsmicroservice.entity.Payment;
import com.emaflores.paymentsmicroservice.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private DiscoveryClient discoveryClient;

    private boolean checkUser(String userId){
        String users = discoveryClient.getInstances("users-microservice")
                .stream()
                .findFirst()
                .map(instance -> instance.getUri().toString())
                .orElseThrow(() -> new RuntimeException("users microservice not available"));

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(users + "/user", String.class);

        return response.getStatusCode().is2xxSuccessful();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Payment>> getPaymentsByUserId(@PathVariable String userId) {
        boolean userExists = checkUser(userId);
        if (!userExists) {
            return ResponseEntity.notFound().build();
        }

        List<Payment> userPayments = paymentRepository.findByOrigin(userId);
        return ResponseEntity.ok(userPayments);
    }


    // Realizar un pago
    @PostMapping
    public ResponseEntity<String> createPayment(@RequestBody Payment payment) {
        if (!isValidDestination(payment.getDestination())) {
            return ResponseEntity.badRequest().body("The destination field does not meet the validation requirements.");
        }

        if (!isValidAmount(payment.getAmount())) {
            return ResponseEntity.badRequest().body("The amount field does not meet the validation requirements.");
        }

        if (!isValidPaymentDate(payment.getPaymentDate())) {
            return ResponseEntity.badRequest().body("The payment date field does not meet the validation requirements.");
        }

        String userId = getUserIdFromApi(payment.getOrigin());

        paymentRepository.save(payment);

        return ResponseEntity.status(HttpStatus.CREATED).body("Payment created successfully.");
    }

    // Actualizar un pago de un cliente
    @PutMapping("/{userId}")
    public ResponseEntity<String> updatePayment(@PathVariable String userId, @RequestBody Payment payment) {
        // Verifica si el pago existe en la base de datos
        Optional<Payment> existingPaymentOptional = paymentRepository.findById(payment.getId());
        if (existingPaymentOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Obtiene el pago existente
        Payment existingPayment = existingPaymentOptional.get();

        // Verifica si el USER_ID coincide con el origen del pago existente
        if (!existingPayment.getOrigin().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No tienes permiso para actualizar este pago.");
        }

        // Actualiza los campos del pago existente con los valores del pago recibido
        existingPayment.setPaymentMethod(payment.getPaymentMethod());
        existingPayment.setOrigin(payment.getOrigin());
        existingPayment.setDestination(payment.getDestination());
        existingPayment.setAmount(payment.getAmount());
        existingPayment.setPaymentDate(payment.getPaymentDate());

        // Guarda los cambios en la base de datos
        paymentRepository.save(existingPayment);

        return ResponseEntity.ok("Pago actualizado exitosamente.");
    }

    // Eliminar un pago de un cliente
    @DeleteMapping("/{userId}")
    public ResponseEntity<String> deletePayment(@PathVariable String userId) {
        // Busca el pago en la base de datos
        List<Payment> paymentOptional = paymentRepository.findByOrigin(userId);
        if (paymentOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Verifica si el USER_ID coincide con el origen del pago
        Payment payment = paymentOptional.get(1);
        if (!payment.getOrigin().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No tienes permiso para eliminar este pago.");
        }

        // Elimina el pago de la base de datos
        paymentRepository.delete(payment);

        return ResponseEntity.ok("Pago eliminado exitosamente.");
    }


    // Metodos de validacion
    private boolean isValidDestination(String destinatario) {
        return destinatario.matches("[0-9]{10}");
    }

    private boolean isValidAmount(BigDecimal monto) {
        return monto.compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean isValidPaymentDate(LocalDate fechaPago) {
        LocalDate currentDate = LocalDate.now();
        return fechaPago.isEqual(currentDate) || fechaPago.isAfter(currentDate);
    }

    private String getUserIdFromApi(String cuentaOrigen) {
        RestTemplate restTemplate = new RestTemplate();
        String apiEndpoint = "http://api-users-url/users?account=" + cuentaOrigen;

        ResponseEntity<String> response = restTemplate.getForEntity(apiEndpoint, String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            // Analiza la respuesta para obtener el USER_ID
            String userId = response.getBody(); // Supongamos que la respuesta es el USER_ID como un String
            return userId;
        } else {
            // Manejo de errores en caso de que la llamada a la API users falle
            throw new RuntimeException("Error al obtener el USER_ID de la API users");
        }
    }

}

