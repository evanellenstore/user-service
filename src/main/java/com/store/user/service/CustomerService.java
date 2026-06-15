package com.store.user.service;

import com.store.user.dto.CustomerResponse;
import com.store.user.dto.PaginatedResponse;
import com.store.user.dto.WalletTransactionResponse;
import com.store.user.entity.Customer;
import com.store.user.entity.WalletTransaction;
import com.store.user.repository.CustomerRepository;
import com.store.user.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Get or create customer by mobile number
     */
    @Transactional
    public CustomerResponse getOrCreateCustomer(String mobileNo, String billingId) {
        if (mobileNo == null || mobileNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Mobile number cannot be empty");
        }

        Customer customer = customerRepository.findByMobileNo(mobileNo.trim())
                .orElseGet(() -> {
                    Customer newCustomer = Customer.builder()
                            .mobileNo(mobileNo.trim())
                            .billingId(billingId)
                            .walletBalance(BigDecimal.ZERO)
                            .build();
                    return customerRepository.save(newCustomer);
                });

        return mapToResponse(customer);
    }

    /**
     * Get customer by ID
     */
    public CustomerResponse getCustomerById(String customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        return mapToResponse(customer);
    }

    /**
     * Get customer by mobile number
     */
    public CustomerResponse getCustomerByMobile(String mobileNo) {
        Customer customer = customerRepository.findByMobileNo(mobileNo)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with mobile: " + mobileNo));
        return mapToResponse(customer);
    }

    /**
     * Add amount to customer wallet
     */
    @Transactional
    public CustomerResponse addToWallet(String customerId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        BigDecimal newBalance = customer.getWalletBalance().add(amount);
        customer.setWalletBalance(newBalance);
        Customer updatedCustomer = customerRepository.save(customer);

        // Save wallet transaction
        WalletTransaction transaction = WalletTransaction.builder()
                .customerId(customerId)
                .type("CREDIT")
                .amount(amount)
                .description(description)
                .build();
        walletTransactionRepository.save(transaction);

        log.info("Added ₹{} to wallet of customer {}. Description: {}. New balance: ₹{}", 
                amount, customerId, description, newBalance);

        return mapToResponse(updatedCustomer);
    }

    /**
     * Deduct amount from customer wallet
     */
    @Transactional
    public CustomerResponse deductFromWallet(String customerId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        if (customer.getWalletBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance. Available: ₹" + 
                    customer.getWalletBalance() + ", Required: ₹" + amount);
        }

        BigDecimal newBalance = customer.getWalletBalance().subtract(amount);
        customer.setWalletBalance(newBalance);
        Customer updatedCustomer = customerRepository.save(customer);

        // Save wallet transaction - store as negative amount for debit
        WalletTransaction transaction = WalletTransaction.builder()
                .customerId(customerId)
                .type("DEBIT")
                .amount(amount.negate())  // Store as negative for debit
                .description(description)
                .build();
        walletTransactionRepository.save(transaction);

        log.info("Deducted ₹{} from wallet of customer {}. Description: {}. New balance: ₹{}", 
                amount, customerId, description, newBalance);

        return mapToResponse(updatedCustomer);
    }

    /**
     * Get customer wallet transactions
     */
    public List<WalletTransactionResponse> getWalletTransactions(String customerId) {
        // Verify customer exists
        customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        
        List<WalletTransaction> transactions = walletTransactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        return transactions.stream()
                .map(this::mapTransactionToResponse)
                .toList();
    }

    /**
     * Get customer wallet transactions with pagination
     */
    public PaginatedResponse<WalletTransactionResponse> getWalletTransactionsPaginated(String customerId, int pageNumber, int pageSize) {
        // Validate pagination parameters
        if (pageNumber < 0) pageNumber = 0;
        if (pageSize <= 0) pageSize = 10;
        if (pageSize > 100) pageSize = 100; // Max page size limit

        // Verify customer exists
        customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        
        // Get all transactions for counting
        List<WalletTransaction> allTransactions = walletTransactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        long totalElements = allTransactions.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);

        // Calculate offset and apply pagination
        int offset = pageNumber * pageSize;
        List<WalletTransaction> pageTransactions = allTransactions.stream()
                .skip(offset)
                .limit(pageSize)
                .toList();

        // Map to response DTOs
        List<WalletTransactionResponse> content = pageTransactions.stream()
                .map(this::mapTransactionToResponse)
                .toList();

        return PaginatedResponse.<WalletTransactionResponse>builder()
                .content(content)
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(pageNumber < totalPages - 1)
                .hasPrevious(pageNumber > 0)
                .build();
    }

    /**
     * Map WalletTransaction entity to WalletTransactionResponse DTO
     */
    private WalletTransactionResponse mapTransactionToResponse(WalletTransaction transaction) {
        return WalletTransactionResponse.builder()
                .id(transaction.getId())
                .customerId(transaction.getCustomerId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt() != null ? transaction.getCreatedAt().format(formatter) : null)
                .build();
    }

    /**
     * Map Customer entity to CustomerResponse DTO
     */
    private CustomerResponse mapToResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .mobileNo(customer.getMobileNo())
                .billingId(customer.getBillingId())
                .walletBalance(customer.getWalletBalance())
                .createdAt(customer.getCreatedAt() != null ? customer.getCreatedAt().format(formatter) : null)
                .updatedAt(customer.getUpdatedAt() != null ? customer.getUpdatedAt().format(formatter) : null)
                .build();
    }
}
