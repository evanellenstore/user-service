package com.store.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionResponse {
    private String id;
    private String customerId;
    private String type; // CREDIT or DEBIT
    private BigDecimal amount;
    private String description;
    private String createdAt;
}
