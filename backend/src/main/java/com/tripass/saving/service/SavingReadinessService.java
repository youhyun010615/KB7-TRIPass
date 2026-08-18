package com.tripass.saving.service;

import com.tripass.saving.dto.SavingReadinessResponseDto;
import com.tripass.saving.dto.SavingReadinessStatus;
import com.tripass.saving.mapper.SavingReadinessMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SavingReadinessService {

    private final SavingReadinessMapper savingReadinessMapper;

    @Transactional(readOnly = true)
    public SavingReadinessResponseDto getReadiness(Long userId) {
        boolean travelGoalRegistered = savingReadinessMapper.existsRegisteredTravelGoal(userId);
        boolean accountLinked = savingReadinessMapper.existsLinkedAccount(userId);
        boolean cardLinked = savingReadinessMapper.existsLinkedCard(userId);
        boolean financialAssetLinked = accountLinked || cardLinked;
        boolean missionPrerequisitesMet = travelGoalRegistered && financialAssetLinked;

        return new SavingReadinessResponseDto(
                travelGoalRegistered,
                accountLinked,
                cardLinked,
                financialAssetLinked,
                missionPrerequisitesMet,
                resolveStatus(travelGoalRegistered, financialAssetLinked)
        );
    }

    private SavingReadinessStatus resolveStatus(
            boolean travelGoalRegistered,
            boolean financialAssetLinked
    ) {
        if (travelGoalRegistered && financialAssetLinked) {
            return SavingReadinessStatus.READY;
        }
        if (!travelGoalRegistered && financialAssetLinked) {
            return SavingReadinessStatus.TRAVEL_GOAL_REQUIRED;
        }
        if (travelGoalRegistered) {
            return SavingReadinessStatus.FINANCIAL_ASSET_REQUIRED;
        }
        return SavingReadinessStatus.TRAVEL_GOAL_AND_FINANCIAL_ASSET_REQUIRED;
    }
}
