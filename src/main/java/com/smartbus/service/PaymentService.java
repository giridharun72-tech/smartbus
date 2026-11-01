package com.smartbus.service;

import com.smartbus.entity.*;
import com.smartbus.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.Random;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Payment processSandboxPayment(Booking booking, Double amount, String method) {
        int year = Year.now().getValue();
        Random random = new Random();
        String ref = "PAY-" + year + "-" + (100000 + random.nextInt(900000));
        String txnId = "TXN-" + System.currentTimeMillis() + "-" + (1000 + random.nextInt(9000));

        Payment payment = new Payment(
                ref,
                booking,
                amount,
                txnId,
                PaymentStatus.SUCCESS,
                method != null && !method.isBlank() ? method : "SANDBOX_GATEWAY"
        );

        return paymentRepository.save(payment);
    }
}
