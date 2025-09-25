package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByLoanDateBetween(LocalDateTime from, LocalDateTime to);

    List<Loan> findByUserDocument(String targetDocument);
}
