package com.tripass.asset.dto;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SupportedInstitutionDto {
    private Long id;
    private String organizationCode;
    private String institutionName;
    private String businessType;
    private String logoUrl;
    private Integer displayOrder;
}
