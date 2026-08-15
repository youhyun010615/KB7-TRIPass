package com.tripass.saving.analysis;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MissionStartWeekPolicyTest {

    private final MissionStartWeekPolicy policy = new MissionStartWeekPolicy();
    private final YearMonth targetMonth = YearMonth.of(2026, 8);

    @Test
    void startsCurrentWeekOnlyOnWeekBoundary() {
        assertEquals(1, policy.resolve(targetMonth, LocalDate.of(2026, 8, 1)));
        assertEquals(2, policy.resolve(targetMonth, LocalDate.of(2026, 8, 8)));
        assertEquals(3, policy.resolve(targetMonth, LocalDate.of(2026, 8, 15)));
        assertEquals(4, policy.resolve(targetMonth, LocalDate.of(2026, 8, 22)));
    }

    @Test
    void startsNextFullWeekWhenCurrentWeekAlreadyStarted() {
        assertEquals(2, policy.resolve(targetMonth, LocalDate.of(2026, 8, 2)));
        assertEquals(3, policy.resolve(targetMonth, LocalDate.of(2026, 8, 9)));
        assertEquals(4, policy.resolve(targetMonth, LocalDate.of(2026, 8, 16)));
    }

    @Test
    void rejectsAfterDay22() {
        assertThrows(IllegalArgumentException.class,
                () -> policy.resolve(targetMonth, LocalDate.of(2026, 8, 23)));
    }

    @Test
    void rejectsDifferentTargetMonth() {
        assertThrows(IllegalArgumentException.class,
                () -> policy.resolve(targetMonth, LocalDate.of(2026, 9, 1)));
    }
}
