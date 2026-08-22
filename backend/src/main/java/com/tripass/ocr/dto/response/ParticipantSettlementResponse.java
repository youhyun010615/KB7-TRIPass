package com.tripass.ocr.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ParticipantSettlementResponse {

    private String participantName;
    private int receiptCount;
    private boolean settled;
    private List<CurrencyAmountResponse> amounts;
}
