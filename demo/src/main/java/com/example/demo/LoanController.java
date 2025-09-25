package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/loans")
@CrossOrigin(origins = "http://localhost:5432")
public class LoanController {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private UserRepository userRepository;

    // Consultar todos los préstamos (solo administrador)
    @CrossOrigin(origins = "http://localhost:5432")
    @GetMapping("/loan")
    public ResponseEntity<?> getAllLoans(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestHeader("document") String document,
            @RequestHeader("password") String password) {

        Optional<User> user = userRepository.findByDocumentAndPassword(document, password);
        if (user.isEmpty() || !"ADMIN".equals(user.get().getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Acceso denegado");
        }

        List<Loan> loans = loanRepository.findByLoanDateBetween(from, to);
        return ResponseEntity.ok(loans);
    }

    // Consultar préstamo por ID

    @CrossOrigin(origins = "http://localhost:5432")
    @GetMapping("/{id}")
    public ResponseEntity<?> getLoanById(
            @PathVariable Long id,
            @RequestHeader("document") String document,
            @RequestHeader("password") String password) {

        Optional<User> user = userRepository.findByDocumentAndPassword(document, password);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales inválidas");
        }

        Optional<Loan> loanOpt = loanRepository.findById(id);
        if (loanOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Préstamo no encontrado");
        }

        Loan loan = loanOpt.get();
        if (!"ADMIN".equals(user.get().getRole()) && !loan.getUserDocument().equals(document)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acceso denegado");
        }

        return ResponseEntity.ok(loan);
    }

    // Consultar préstamos por documento

    @CrossOrigin(origins = "http://localhost:5432")
    @GetMapping("/document/{targetDocument}")
    public ResponseEntity<?> getLoansByDocument(
            @PathVariable String targetDocument,
            @RequestHeader("document") String document,
            @RequestHeader("password") String password) {

        Optional<User> user = userRepository.findByDocumentAndPassword(document, password);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales inválidas");
        }

        boolean isAdmin = "ADMIN".equals(user.get().getRole());
        if (!isAdmin && !document.equals(targetDocument)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acceso denegado");
        }

        List<Loan> loans = loanRepository.findByUserDocument(targetDocument);
        return ResponseEntity.ok(loans);
    }

    // Crear préstamo
    @PostMapping("/user")
    public ResponseEntity<?> createLoan(@RequestBody LoanRequest request) {
        Optional<User> user = userRepository.findById(request.getDocument());

        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
        }

        Loan loan = new Loan();
        loan.setUserDocument(request.getDocument());
        loan.setLoanDate(LocalDateTime.now());
        loan.setReturned(false);

        loanRepository.save(loan);
        return ResponseEntity.ok("Préstamo registrado correctamente");
    }

    // Finalizar préstamo (solo admin)
    @PutMapping("/{id}/return")
    public ResponseEntity<?> returnLoan(
            @PathVariable Long id,
            @RequestHeader("document") String document,
            @RequestHeader("password") String password) {

        Optional<User> admin = userRepository.findByDocumentAndPassword(document, password);
        if (admin.isEmpty() || !"ADMIN".equals(admin.get().getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Acceso denegado");
        }

        Optional<Loan> loanOpt = loanRepository.findById(id);
        if (loanOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Préstamo no encontrado");
        }

        Loan loan = loanOpt.get();
        loan.setReturned(true);
        loan.setReturnDate(LocalDateTime.now());
        loanRepository.save(loan);

        return ResponseEntity.ok("Préstamo finalizado correctamente");
    }
}
