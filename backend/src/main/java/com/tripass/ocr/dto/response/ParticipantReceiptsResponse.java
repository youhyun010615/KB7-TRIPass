package com.tripass.ocr.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class ParticipantReceiptsResponse {

    private String participantName;
    private BigDecimal totalOwedAmount;
    private List<ReceiptSummaryResponse> receipts;
}
