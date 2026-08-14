package com.tripass.travel.controller;

import com.tripass.common.response.ApiResponse;
import com.tripass.travel.dto.CountryStatusDto;
import com.tripass.travel.dto.TravelStatusResponseDto;
import com.tripass.travel.dto.TripHomeDashboardResponseDto;
import com.tripass.travel.service.TravelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.List;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class TravelControllerTest {

    @Mock
    private TravelService travelService;

    @InjectMocks
    private TravelController travelController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("TravelService 반환 데이터 구조 및 값 검증")
    void getTravelStatus_DataContractTest() {
        // given
        Long tripId = 1L;
        
        CountryStatusDto country = new CountryStatusDto();
        country.setCountryName("일본");
        country.setRemainingFund(800000L);
        country.setRemainingDays(10L);

        TravelStatusResponseDto responseDto = new TravelStatusResponseDto();
        responseDto.setTotalRemainingFund(800000L);
        responseDto.setDailyAvailableAmount(80000L);
        responseDto.setCountries(Collections.singletonList(country));
        Long userId = 1L;

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userId);
        when(travelService.getTravelStatus(tripId, userId)).thenReturn(responseDto);

        // when
        ResponseEntity<ApiResponse<TravelStatusResponseDto>> response = travelController.getTravelStatus(tripId, authentication);

        // then
        assertNotNull(response.getBody());
        assertEquals("SUCCESS", response.getBody().getCode());
        
        TravelStatusResponseDto data = response.getBody().getData();
        assertNotNull(data);
        assertEquals(800000L, data.getTotalRemainingFund());
        assertEquals(80000L, data.getDailyAvailableAmount());
        
        List<CountryStatusDto> countries = data.getCountries();
        assertNotNull(countries);
        assertEquals(1, countries.size());
        assertEquals("일본", countries.get(0).getCountryName());
        assertEquals(800000L, countries.get(0).getRemainingFund());
        assertEquals(10L, countries.get(0).getRemainingDays());
    }

    @Test
    @DisplayName("활성 여행 홈 대시보드 조회 응답을 반환한다")
    void getActiveTripHome() {
        Long userId = 1L;
        Authentication authentication = mock(Authentication.class);
        TripHomeDashboardResponseDto responseDto = TripHomeDashboardResponseDto.builder()
                .tripId(10L)
                .tripName("유럽 여행")
                .totalTargetAmount(new BigDecimal("2000000"))
                .walletBalance(new BigDecimal("500000"))
                .build();
        when(authentication.getPrincipal()).thenReturn(userId);
        when(travelService.getActiveTripHome(userId)).thenReturn(responseDto);

        ResponseEntity<ApiResponse<TripHomeDashboardResponseDto>> response =
                travelController.getActiveTripHome(authentication);

        assertNotNull(response.getBody());
        assertEquals("SUCCESS", response.getBody().getCode());
        assertEquals("유럽 여행", response.getBody().getData().getTripName());
        assertEquals(new BigDecimal("500000"), response.getBody().getData().getWalletBalance());
    }
}
