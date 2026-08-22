package com.tripass.asset.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CodefConnectedInstitutionDto {
    private Long id;
    private Long codefConnectionId;
    private String organizationCode;
    private String organizationName;
    private String businessType;
    private String connectionStatus;
}
