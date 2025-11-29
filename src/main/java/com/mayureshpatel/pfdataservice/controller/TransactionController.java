package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.model.Category;
import com.mayureshpatel.pfdataservice.model.Transaction;
import com.mayureshpatel.pfdataservice.repository.TransactionRepository;
import com.mayureshpatel.pfdataservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadCsv(
            @RequestParam("accountId") Long accountId,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please upload a CSV file");
        }

        try {
            int count = this.transactionService.importCsv(accountId, file.getInputStream());
            return ResponseEntity.ok("Successfully imported " + count + " transactions");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error importing transactions: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<Page<Transaction>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "1") Long userId) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                this.transactionRepository.findByAccount_User_IdOrderByDateDesc(userId, pageable)
        );
    }

    @PatchMapping("/{id}/category")
    public ResponseEntity<Void> updateCategory(@PathVariable Long id, @RequestBody Long categoryId) {
        Transaction transaction = this.transactionRepository.findById(id).orElseThrow(() -> new RuntimeException("Transaction not found"));

        Category category = new Category();
        category.setId(categoryId);

        transaction.setCategory(category);
        this.transactionRepository.save(transaction);

        return ResponseEntity.ok().build();
    }
}
