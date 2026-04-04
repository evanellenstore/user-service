package com.store.user.controller;

import com.store.user.dto.*;
import com.store.user.service.CustomerService;
import com.store.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    private final CustomerService customerService;

    @PostMapping
    public UserResponse create(@RequestBody @Valid UserRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<UserResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public UserResponse update(
            @PathVariable Long id,
            @RequestBody @Valid UserRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/by-username")
    public UserResponse getByUsername(@RequestParam String username) {
        return service.getByUsername(username);
    }




    /**
     * Get or create customer by mobile number
     */
    @PostMapping("/customers/getOrCreate")
    public ResponseEntity<CustomerResponse> getOrCreateCustomer(@RequestBody Map<String, String> payload) {
        try {
            String mobileNo = payload.get("mobileNo");
            String billingId = payload.get("billingId");
            CustomerResponse customer = customerService.getOrCreateCustomer(mobileNo, billingId);
            return ResponseEntity.ok(customer);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get customer by ID
     */
    @GetMapping("/customers/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable String customerId) {
        try {
            CustomerResponse customer = customerService.getCustomerById(customerId);
            return ResponseEntity.ok(customer);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get customer by mobile number
     */
    @GetMapping("/customers/mobile/{mobileNo}")
    public ResponseEntity<CustomerResponse> getCustomerByMobile(@PathVariable String mobileNo) {
        try {
            CustomerResponse customer = customerService.getCustomerByMobile(mobileNo);
            return ResponseEntity.ok(customer);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Add amount to customer wallet
     */
    @PostMapping("/customers/{customerId}/wallet/add")
    public ResponseEntity<CustomerResponse> addToWallet(
            @PathVariable String customerId,
            @RequestBody Map<String, Object> payload) {
        try {
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());
            String description = (String) payload.get("description");
            CustomerResponse customer = customerService.addToWallet(customerId, amount, description);
            return ResponseEntity.ok(customer);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Deduct amount from customer wallet
     */
    @PostMapping("/customers/{customerId}/wallet/deduct")
    public ResponseEntity<?> deductFromWallet(
            @PathVariable String customerId,
            @RequestBody Map<String, Object> payload) {
        try {
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());
            String description = (String) payload.get("description");
            CustomerResponse customer = customerService.deductFromWallet(customerId, amount, description);
            return ResponseEntity.ok(customer);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get customer wallet transactions
     */
    @GetMapping("/customers/{customerId}/wallet/transactions")
    public ResponseEntity<?> getWalletTransactions(@PathVariable String customerId) {
        try {
            var transactions = customerService.getWalletTransactions(customerId);
            return ResponseEntity.ok(transactions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }


}
