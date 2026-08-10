package com.tripass.asset.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardDto {
    private Long id;
    private Long userId;
    private Long codefConnectionId;
    private String cardName;
    private String maskedCardNumber;
    private String cardType;         // CREDIT / CHECK
    private String organizationCode;
}
