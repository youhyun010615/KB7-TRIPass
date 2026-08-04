package com.tripass.asset.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class AccountDto {
    private Long id;
    private Long userId;
    private Long codefConnectionId;
    private String accountName;
    private String accountNumber;
    private String accountType;
    private BigDecimal balance;
    private BigDecimal withdrawableAmount;
    private BigDecimal recognizedAmount;
    private Boolean isTravelFundIncluded;
    private String connectionType;
    private String lastSyncedAt;
    private String organizationCode;
}