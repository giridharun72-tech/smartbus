package com.smartbus.repository;

import com.smartbus.entity.Booking;
import com.smartbus.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBooking(Booking booking);
    Optional<Payment> findByPaymentReference(String paymentReference);
    Optional<Payment> findByTransactionId(String transactionId);
}
