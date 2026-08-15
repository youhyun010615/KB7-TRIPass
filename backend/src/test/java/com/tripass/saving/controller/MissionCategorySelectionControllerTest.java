package com.tripass.saving.controller;

import com.tripass.common.exception.CustomException;
import com.tripass.common.response.ApiResponse;
import com.tripass.saving.dto.CategorySelectionItemDto;
import com.tripass.saving.dto.MissionOptionResponseDto;
import com.tripass.saving.dto.MissionSelectionRequestDto;
import com.tripass.saving.dto.MissionSelectionResponseDto;
import com.tripass.saving.dto.MissionSelectionsResponseDto;
import com.tripass.saving.dto.ReductionRateOptionDto;
import com.tripass.saving.service.MissionCategorySelectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MissionCategorySelectionControllerTest {

    private static final Long USER_ID = 1L;

    @Mock
    private MissionCategorySelectionService missionCategorySelectionService;

    @InjectMocks
    private MissionCategorySelectionController controller;

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(USER_ID);
    }

    @Test
    @DisplayName("절감률 옵션 조회는 서비스 결과를 그대로 응답한다")
    void getMissionOptions_returnsServiceResult() {
        List<MissionOptionResponseDto> options = List.of(new MissionOptionResponseDto(
                1L, "FOOD", "식비", 1, "최근 3개월 평균보다 소비가 35% 증가했어요.", 100000,
                List.of(new ReductionRateOptionDto(10, 10000, 90000, 22500, 2500))));
        when(missionCategorySelectionService.getMissionOptions(USER_ID, YearMonth.of(2026, 7)))
                .thenReturn(options);

        ResponseEntity<ApiResponse<List<MissionOptionResponseDto>>> response =
                controller.getMissionOptions("2026-07", authentication);

        assertNotNull(response.getBody());
        assertEquals("SUCCESS", response.getBody().getCode());
        assertEquals(options, response.getBody().getData());
    }

    @Test
    @DisplayName("절감률 선택 저장은 인증된 사용자 ID와 파싱된 연월로 서비스를 호출한다")
    void saveMissionSelections_callsServiceWithParsedYearMonth() {
        MissionSelectionRequestDto request = MissionSelectionRequestDto.builder()
                .selections(List.of(CategorySelectionItemDto.builder().categoryId(1L).reductionRate(30).build()))
                .build();
        MissionSelectionsResponseDto saved = new MissionSelectionsResponseDto(1, 30000, 7500, List.of(
                new MissionSelectionResponseDto(1L, "FOOD", "식비", 30, 100000, 30000, 70000, 17500, 7500)));
        when(missionCategorySelectionService.saveMissionSelections(USER_ID, YearMonth.of(2026, 7), request))
                .thenReturn(saved);

        ResponseEntity<ApiResponse<MissionSelectionsResponseDto>> response =
                controller.saveMissionSelections("2026-07", request, authentication);

        assertEquals(saved, response.getBody().getData());
        verify(missionCategorySelectionService).saveMissionSelections(USER_ID, YearMonth.of(2026, 7), request);
    }

    @Test
    @DisplayName("절감률 선택 조회는 서비스 결과를 그대로 응답한다")
    void getMissionSelections_returnsServiceResult() {
        MissionSelectionsResponseDto saved = new MissionSelectionsResponseDto(1, 30000, 7500, List.of(
                new MissionSelectionResponseDto(1L, "FOOD", "식비", 30, 100000, 30000, 70000, 17500, 7500)));
        when(missionCategorySelectionService.getMissionSelections(USER_ID, YearMonth.of(2026, 7)))
                .thenReturn(saved);

        ResponseEntity<ApiResponse<MissionSelectionsResponseDto>> response =
                controller.getMissionSelections("2026-07", authentication);

        assertEquals(saved, response.getBody().getData());
    }

    @Test
    @DisplayName("yearMonth 형식이 올바르지 않으면 400 예외를 던지고 서비스는 호출하지 않는다")
    void invalidYearMonth_throwsBadRequestAndDoesNotCallService() {
        CustomException exception = assertThrows(CustomException.class,
                () -> controller.getMissionOptions("2026/07", authentication));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("INVALID_YEAR_MONTH", exception.getErrorCode());
    }
}
