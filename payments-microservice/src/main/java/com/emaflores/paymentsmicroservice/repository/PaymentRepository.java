package com.emaflores.paymentsmicroservice.repository;

import com.emaflores.paymentsmicroservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrigin(String origin);
}
