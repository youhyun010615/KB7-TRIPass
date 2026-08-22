package com.tripass.asset.dto;


import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class CalendarDayDto {
    private String date;
    private BigDecimal totalDeposit;
    private BigDecimal totalWithdrawal;
}
