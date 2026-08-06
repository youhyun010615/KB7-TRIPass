package com.tripass.schedule.dto;

import com.tripass.schedule.enums.SchedulePaymentStatus;
import com.tripass.schedule.enums.ScheduleStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Builder
@ApiModel(description = "여행 일정 상세 응답")
public class ScheduleDetailResponseDto {

    @ApiModelProperty(
            value = "여행 일정 ID",
            example = "10"
    )
    private Long id;

    @ApiModelProperty(
            value = "여행 ID",
            example = "1"
    )
    private Long tripId;

    @ApiModelProperty(
            value = "여행 국가 ID",
            example = "2"
    )
    private Long tripCountryId;

    @ApiModelProperty(
            value = "국가명",
            example = "프랑스"
    )
    private String countryName;

    @ApiModelProperty(
            value = "IANA 시간대",
            example = "Europe/Paris"
    )
    private String timeZone;

    @ApiModelProperty(
            value = "일정명",
            example = "루브르 박물관 가이드 투어"
    )
    private String scheduleName;

    @ApiModelProperty(
            value = "현지 시간과 UTC 오프셋이 포함된 일정 시간",
            example = "2026-08-28T10:30:00+02:00"
    )
    private OffsetDateTime scheduledAt;

    @ApiModelProperty(
            value = "현지 통화 기준 금액",
            example = "85.00"
    )
    private BigDecimal amount;

    @ApiModelProperty(
            value = "통화 코드",
            example = "EUR"
    )
    private String currencyCode;

    @ApiModelProperty(
            value = "통화 기호",
            example = "€"
    )
    private String currencySymbol;

    @ApiModelProperty(
            value = "결제 상태",
            allowableValues = "PREPAID, ONSITE, UNDECIDED",
            example = "PREPAID"
    )
    private SchedulePaymentStatus paymentStatus;

    @ApiModelProperty(
            value = "일정 진행 상태",
            allowableValues = "UPCOMING, DONE",
            example = "UPCOMING"
    )
    private ScheduleStatus scheduleStatus;

    @ApiModelProperty(
            value = "장소명",
            example = "루브르 박물관"
    )
    private String placeName;

    @ApiModelProperty(
            value = "장소 주소",
            example = "Rue de Rivoli, 75001 Paris, France"
    )
    private String placeAddress;

    @ApiModelProperty(
            value = "일정 메모",
            example = "입장 10분 전까지 도착"
    )
    private String memo;
}