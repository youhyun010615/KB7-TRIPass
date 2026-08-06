package com.tripass.bank.domain;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ExchangeBankBranch {
    private Long id;
    private String bankName;
    private String branchName;
    private String address; 
    private BigDecimal latitude;
    private BigDecimal longitude;
    
    // Nullable fields
    private String businessHours;
    private String phoneNumber;
    
    private boolean isActive = true; // 스키마의 DEFAULT TRUE 반영
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
