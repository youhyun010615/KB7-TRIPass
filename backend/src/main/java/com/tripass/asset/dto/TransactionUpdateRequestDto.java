package com.tripass.asset.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionUpdateRequestDto {

    private Long categoryId;
    private String memo;
}
