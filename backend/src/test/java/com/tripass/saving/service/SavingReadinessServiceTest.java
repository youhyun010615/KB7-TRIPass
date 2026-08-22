package com.tripass.saving.service;

import com.tripass.saving.dto.SavingReadinessResponseDto;
import com.tripass.saving.dto.SavingReadinessStatus;
import com.tripass.saving.mapper.SavingReadinessMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavingReadinessServiceTest {

    private static final Long USER_ID = 7L;

    @Mock
    private SavingReadinessMapper mapper;

    @InjectMocks
    private SavingReadinessService service;

    @Test
    void returnsReadyWhenTravelGoalAndAccountAreRegistered() {
        givenState(true, true, false);

        SavingReadinessResponseDto result = service.getReadiness(USER_ID);

        assertEquals(SavingReadinessStatus.READY, result.status());
        assertTrue(result.financialAssetLinked());
        assertTrue(result.missionPrerequisitesMet());
    }

    @Test
    void requiresOnlyTravelGoalWhenCardIsAlreadyLinked() {
        givenState(false, false, true);

        SavingReadinessResponseDto result = service.getReadiness(USER_ID);

        assertEquals(SavingReadinessStatus.TRAVEL_GOAL_REQUIRED, result.status());
        assertTrue(result.cardLinked());
        assertTrue(result.financialAssetLinked());
        assertFalse(result.missionPrerequisitesMet());
    }

    @Test
    void requiresFinancialAssetWhenOnlyTravelGoalIsRegistered() {
        givenState(true, false, false);

        SavingReadinessResponseDto result = service.getReadiness(USER_ID);

        assertEquals(SavingReadinessStatus.FINANCIAL_ASSET_REQUIRED, result.status());
        assertFalse(result.financialAssetLinked());
        assertFalse(result.missionPrerequisitesMet());
    }

    @Test
    void requiresTravelGoalAndFinancialAssetWhenNeitherExists() {
        givenState(false, false, false);

        SavingReadinessResponseDto result = service.getReadiness(USER_ID);

        assertEquals(SavingReadinessStatus.TRAVEL_GOAL_AND_FINANCIAL_ASSET_REQUIRED, result.status());
        assertFalse(result.travelGoalRegistered());
        assertFalse(result.accountLinked());
        assertFalse(result.cardLinked());
        assertFalse(result.missionPrerequisitesMet());
    }

    private void givenState(boolean travelGoalRegistered, boolean accountLinked, boolean cardLinked) {
        when(mapper.existsRegisteredTravelGoal(USER_ID)).thenReturn(travelGoalRegistered);
        when(mapper.existsLinkedAccount(USER_ID)).thenReturn(accountLinked);
        when(mapper.existsLinkedCard(USER_ID)).thenReturn(cardLinked);
    }
}
