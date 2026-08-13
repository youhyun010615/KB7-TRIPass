package com.tripass.travel.client;

import com.tripass.travel.dto.AiBudgetResultDto;
import com.tripass.travel.dto.TripCountryBudgetContextDto;

public interface TravelBudgetAiClient {
    AiBudgetResultDto recommend(TripCountryBudgetContextDto context, long tripDays);
}
