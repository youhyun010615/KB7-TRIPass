package com.tripass.saving.service;

import com.tripass.saving.dto.WeeklySavingMissionDto;
import com.tripass.saving.mapper.SavingMissionMapper;
import com.tripass.wallet.domain.Wallet;
import com.tripass.wallet.domain.WalletLedger;
import com.tripass.wallet.exception.WalletException;
import com.tripass.wallet.mapper.WalletMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MissionWalletRewardServiceTest {

    private static final Long USER_ID = 3L;
    private static final Long WALLET_ID = 10L;

    @Mock private WalletMapper walletMapper;
    @Mock private SavingMissionMapper missionMapper;
    private MissionWalletRewardService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new MissionWalletRewardService(walletMapper, missionMapper);
    }

    @Test
    void actualSavingAboveExpectedSavingRewardsFullActualAmount() {
        WeeklySavingMissionDto mission = successfulMission(11L, 12_500, 9_750);
        givenWallet();
        givenInsertedLedgerId(101L);
        when(walletMapper.updateWalletBalance(WALLET_ID, BigDecimal.valueOf(112_500), 0L)).thenReturn(1);
        when(missionMapper.updateWeeklyMissionReward(11L, 12_500, 101L)).thenReturn(1);

        service.rewardSuccessfulMissions(USER_ID, List.of(mission));

        ArgumentCaptor<WalletLedger> captor = ArgumentCaptor.forClass(WalletLedger.class);
        verify(walletMapper).insertWalletLedger(captor.capture());
        WalletLedger ledger = captor.getValue();
        assertEquals("IN", ledger.getDirection());
        assertEquals("MISSION_REWARD", ledger.getTransactionType());
        assertEquals("MISSION", ledger.getSourceType());
        assertEquals(11L, ledger.getSourceId());
        assertEquals("WALLET", ledger.getTargetType());
        assertEquals(WALLET_ID, ledger.getTargetId());
        assertEquals("MISSION_REWARD:WEEKLY:11", ledger.getIdempotencyKey());
        assertEquals(0, BigDecimal.valueOf(12_500).compareTo(ledger.getAmount()));
        assertEquals(12_500, mission.getRewardAmount());
        assertEquals(101L, mission.getWalletLedgerId());
        assertNotNull(mission.getRewardedAt());
    }

    @Test
    void actualSavingBelowExpectedSavingRewardsOnlyActualAmount() {
        WeeklySavingMissionDto mission = successfulMission(12L, 5_000, 9_750);
        givenWallet();
        givenInsertedLedgerId(102L);
        when(walletMapper.updateWalletBalance(WALLET_ID, BigDecimal.valueOf(105_000), 0L)).thenReturn(1);
        when(missionMapper.updateWeeklyMissionReward(12L, 5_000, 102L)).thenReturn(1);

        service.rewardSuccessfulMissions(USER_ID, List.of(mission));

        verify(missionMapper).updateWeeklyMissionReward(12L, 5_000, 102L);
        assertEquals(5_000, mission.getRewardAmount());
    }

    @Test
    void failedMissionDoesNotRewardWallet() {
        WeeklySavingMissionDto mission = successfulMission(11L, 0, 9_750);
        mission.setStatus("FAILED");

        service.rewardSuccessfulMissions(USER_ID, List.of(mission));

        verifyNoInteractions(walletMapper, missionMapper);
    }

    @Test
    void alreadyRewardedMissionDoesNotRewardAgain() {
        WeeklySavingMissionDto mission = successfulMission(11L, 12_500, 9_750);
        mission.setRewardAmount(9_750);
        mission.setWalletLedgerId(101L);

        service.rewardSuccessfulMissions(USER_ID, List.of(mission));

        verifyNoInteractions(walletMapper, missionMapper);
    }

    @Test
    void existingIdempotentLedgerRestoresMissionRewardStateWithoutIncreasingBalance() {
        WeeklySavingMissionDto mission = successfulMission(11L, 12_500, 9_750);
        WalletLedger existing = WalletLedger.builder()
                .id(101L)
                .amount(BigDecimal.valueOf(9_750))
                .build();
        when(walletMapper.findWalletLedgerByIdempotencyKey("MISSION_REWARD:WEEKLY:11"))
                .thenReturn(existing);
        when(missionMapper.updateWeeklyMissionReward(11L, 9_750, 101L)).thenReturn(1);

        service.rewardSuccessfulMissions(USER_ID, List.of(mission));

        verify(walletMapper, never()).findWalletByUserIdForUpdate(USER_ID);
        verify(walletMapper, never()).updateWalletBalance(any(), any(), any());
        verify(walletMapper, never()).insertWalletLedger(any());
        assertEquals(101L, mission.getWalletLedgerId());
    }

    @Test
    void missingWalletRollsBackMissionReward() {
        WeeklySavingMissionDto mission = successfulMission(11L, 12_500, 9_750);
        when(walletMapper.findWalletByUserIdForUpdate(USER_ID)).thenReturn(null);

        WalletException error = assertThrows(WalletException.class,
                () -> service.rewardSuccessfulMissions(USER_ID, List.of(mission)));

        assertEquals("WALLET_NOT_FOUND", error.getErrorCode());
        verify(walletMapper, never()).insertWalletLedger(any());
        verifyNoInteractions(missionMapper);
    }

    private void givenWallet() {
        Wallet wallet = Wallet.builder()
                .id(WALLET_ID)
                .userId(USER_ID)
                .balanceAmount(BigDecimal.valueOf(100_000))
                .status("ACTIVE")
                .version(0L)
                .build();
        when(walletMapper.findWalletByUserIdForUpdate(USER_ID)).thenReturn(wallet);
    }

    private void givenInsertedLedgerId(Long ledgerId) {
        doAnswer(invocation -> {
            WalletLedger ledger = invocation.getArgument(0);
            ledger.setId(ledgerId);
            return 1;
        }).when(walletMapper).insertWalletLedger(any(WalletLedger.class));
    }

    private WeeklySavingMissionDto successfulMission(Long id, int actualSaving, int expectedSaving) {
        WeeklySavingMissionDto mission = new WeeklySavingMissionDto();
        mission.setId(id);
        mission.setWeekNumber(1);
        mission.setCategoryName("카페");
        mission.setWeeklyExpectedSaving(expectedSaving);
        mission.setActualSaving(actualSaving);
        mission.setStatus("SUCCESS");
        return mission;
    }
}
