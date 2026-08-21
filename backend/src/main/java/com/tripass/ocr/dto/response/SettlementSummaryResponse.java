package com.tripass.ocr.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SettlementSummaryResponse {

    private List<CurrencyAmountResponse> totalAmounts;
    private int participantCount;
    private List<ParticipantSettlementResponse> participants;
}
