package com.tripass.wallet.scheduler;

import com.tripass.wallet.domain.Wallet;
import com.tripass.wallet.domain.WalletAutoSavingLog;
import com.tripass.wallet.domain.WalletAutoSavingRule;
import com.tripass.wallet.domain.WalletLedger;
import com.tripass.wallet.dto.response.WalletAccountResponseDto;
import com.tripass.wallet.mapper.WalletMapper;
import com.tripass.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletAutoSavingSchedulerTest {

    @Mock
    private WalletService walletService;

    @Mock
    private WalletMapper walletMapper;

    @Test
    void 자동_송금_대상_규칙을_처리한다() {
        WalletAutoSavingScheduler scheduler = new WalletAutoSavingScheduler(walletService);

        when(walletService.executeDueAutoSavingRules(any(LocalDate.class)))
                .thenReturn(3);

        scheduler.executeDailyAutoSaving();

        verify(walletService).executeDueAutoSavingRules(any(LocalDate.class));
    }

    @Test
    void 자동_송금_처리_중_예외가_발생해도_스케줄러_밖으로_전파되지_않는다() {
        WalletAutoSavingScheduler scheduler = new WalletAutoSavingScheduler(walletService);

        when(walletService.executeDueAutoSavingRules(any(LocalDate.class)))
                .thenThrow(new RuntimeException("DB 오류"));

        assertDoesNotThrow(scheduler::executeDailyAutoSaving);
    }

    @Test
    void 매일_오전_10시_기준으로_실행되도록_스케줄이_설정되어_있다() throws NoSuchMethodException {
        Method method = WalletAutoSavingScheduler.class.getDeclaredMethod("executeDailyAutoSaving");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertNotNull(scheduled);
        assertEquals("0 0 10 * * *", scheduled.cron());
        assertEquals("Asia/Seoul", scheduled.zone());
    }

    @Test
    void 설정일_오전_10시_실행에서_주계좌_잔액이_충분하면_주계좌에서_빠져나가_월렛에_들어오고_성공_알림을_남긴다() {
        WalletService realWalletService = new WalletService(walletMapper);
        WalletAutoSavingScheduler scheduler = new WalletAutoSavingScheduler(realWalletService);

        WalletAutoSavingRule rule = WalletAutoSavingRule.builder()
                .id(7L)
                .walletId(100L)
                .sourceAccountId(10L)
                .amount(BigDecimal.valueOf(500_000))
                .dayOfMonth(25)
                .build();
        Wallet wallet = Wallet.builder()
                .id(100L)
                .userId(1L)
                .balanceAmount(BigDecimal.valueOf(1_000_000))
                .status("ACTIVE")
                .version(0L)
                .build();
        WalletAccountResponseDto primaryAccount = WalletAccountResponseDto.builder()
                .accountId(11L)
                .accountName("카카오뱅크 입출금통장")
                .withdrawableAmount(BigDecimal.valueOf(1_000_000))
                .build();

        when(walletMapper.findDueAutoSavingRules(any(LocalDate.class)))
                .thenReturn(List.of(rule));
        when(walletMapper.findWalletById(100L))
                .thenReturn(wallet);
        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(wallet);
        when(walletMapper.findPrimaryAccountByWalletId(100L))
                .thenReturn(primaryAccount);
        when(walletMapper.decreaseAccountBalance(1L, 11L, BigDecimal.valueOf(500_000)))
                .thenReturn(1);
        when(walletMapper.updateWalletBalance(eq(100L), any(), eq(0L)))
                .thenReturn(1);

        scheduler.executeDailyAutoSaving();

        // 주계좌(11L)에서 설정 금액만큼 빠져나간다.
        verify(walletMapper).decreaseAccountBalance(1L, 11L, BigDecimal.valueOf(500_000));

        // 같은 금액이 월렛 잔액에 더해지고 원장에 기록된다.
        ArgumentCaptor<WalletLedger> captor = ArgumentCaptor.forClass(WalletLedger.class);
        verify(walletMapper).insertWalletLedger(captor.capture());
        WalletLedger ledger = captor.getValue();
        assertEquals("IN", ledger.getDirection());
        assertEquals("CHARGE", ledger.getTransactionType());
        assertEquals("AUTO_SAVING", ledger.getTransferMethod());
        assertEquals(0, BigDecimal.valueOf(500_000).compareTo(ledger.getAmount()));
        assertEquals(0, BigDecimal.valueOf(1_500_000).compareTo(ledger.getBalanceAfter()));

        verify(walletMapper).updateWalletBalance(100L, BigDecimal.valueOf(1_500_000), 0L);

        // 성공 기록만 남고 실패 기록은 남지 않는다.
        ArgumentCaptor<WalletAutoSavingLog> logCaptor = ArgumentCaptor.forClass(WalletAutoSavingLog.class);
        verify(walletMapper).insertAutoSavingLog(logCaptor.capture());
        WalletAutoSavingLog log = logCaptor.getValue();
        assertEquals(100L, log.getWalletId());
        assertEquals("SUCCESS", log.getStatus());
        assertEquals(0, BigDecimal.valueOf(500_000).compareTo(log.getAmount()));
    }

    @Test
    void 설정_금액이_주계좌_잔액보다_크면_채우기가_일어나지_않고_실패_알림을_남긴다() {
        WalletService realWalletService = new WalletService(walletMapper);
        WalletAutoSavingScheduler scheduler = new WalletAutoSavingScheduler(realWalletService);

        WalletAutoSavingRule rule = WalletAutoSavingRule.builder()
                .id(7L)
                .walletId(100L)
                .sourceAccountId(10L)
                .amount(BigDecimal.valueOf(500_000))
                .dayOfMonth(25)
                .build();
        Wallet wallet = Wallet.builder()
                .id(100L)
                .userId(1L)
                .balanceAmount(BigDecimal.valueOf(1_000_000))
                .status("ACTIVE")
                .version(0L)
                .build();
        WalletAccountResponseDto primaryAccount = WalletAccountResponseDto.builder()
                .accountId(11L)
                .accountName("카카오뱅크 입출금통장")
                .withdrawableAmount(BigDecimal.valueOf(300_000))
                .build();

        when(walletMapper.findDueAutoSavingRules(any(LocalDate.class)))
                .thenReturn(List.of(rule));
        when(walletMapper.findWalletById(100L))
                .thenReturn(wallet);
        when(walletMapper.findWalletByUserIdForUpdate(1L))
                .thenReturn(wallet);
        when(walletMapper.findPrimaryAccountByWalletId(100L))
                .thenReturn(primaryAccount);

        scheduler.executeDailyAutoSaving();

        // 채우기가 일어나지 않는다: 계좌 출금도, 월렛 잔액 변경도, 원장 기록도 없다.
        verify(walletMapper, never()).decreaseAccountBalance(anyLong(), anyLong(), any());
        verify(walletMapper, never()).insertWalletLedger(any());
        verify(walletMapper, never()).updateWalletBalance(anyLong(), any(), anyLong());

        // 다음 송금일만 갱신된다.
        verify(walletMapper).updateAutoSavingNextTransferDate(eq(7L), any());

        // 실패 기록만 남고 성공 기록은 남지 않는다.
        ArgumentCaptor<WalletAutoSavingLog> logCaptor = ArgumentCaptor.forClass(WalletAutoSavingLog.class);
        verify(walletMapper).insertAutoSavingLog(logCaptor.capture());
        WalletAutoSavingLog log = logCaptor.getValue();
        assertEquals(100L, log.getWalletId());
        assertEquals("FAILED", log.getStatus());
        assertEquals(0, BigDecimal.valueOf(500_000).compareTo(log.getAmount()));
    }
}
