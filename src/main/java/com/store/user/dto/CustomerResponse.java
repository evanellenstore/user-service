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
public class CustomerResponse {
    private String id;
    private String mobileNo;
    private String billingId;
    private BigDecimal walletBalance;
    private String createdAt;
    private String updatedAt;
}
