package com.storechain.payment.controller;

import com.storechain.payment.entities.Payment;
import com.storechain.payment.repository.PaymentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentRepository paymentRepository;

    public PaymentController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @PostMapping
    public ResponseEntity<Payment> processPayment(@RequestBody Payment payment) {
        payment.setStatus("COMPLETED");
        return ResponseEntity.ok(paymentRepository.save(payment));
    }
}
