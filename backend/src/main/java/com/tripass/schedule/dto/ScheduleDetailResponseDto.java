package com.tripass.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Builder
@ApiModel(description = "여행 일정 상세 조회 응답")
public class ScheduleDetailResponseDto {

    @ApiModelProperty(value = "여행 일정 ID", example = "1")
    private final Long id;

    @ApiModelProperty(value = "여행 ID", example = "1")
    private final Long tripId;

    @ApiModelProperty(value = "여행 국가 ID", example = "1")
    private final Long tripCountryId;

    @ApiModelProperty(value = "국가명", example = "프랑스")
    private final String countryName;

    @ApiModelProperty(
            value = "국가 시간대",
            example = "Europe/Paris"
    )
    private final String timeZone;

    @ApiModelProperty(
            value = "일정명",
            example = "루브르 박물관 가이드 투어"
    )
    private final String scheduleName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    @ApiModelProperty(
            value = "일정 시작 시각",
            example = "2026-08-28T10:30:00+02:00"
    )
    private final OffsetDateTime startAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    @ApiModelProperty(
            value = "일정 종료 시각",
            example = "2026-08-28T12:00:00+02:00"
    )
    private final OffsetDateTime endAt;

    @ApiModelProperty(value = "현지 통화 금액", example = "85.00")
    private final BigDecimal amount;

    @ApiModelProperty(value = "통화 코드", example = "EUR")
    private final String currencyCode;

    @ApiModelProperty(value = "통화 기호", example = "€")
    private final String currencySymbol;

    @ApiModelProperty(value = "결제 상태", example = "PREPAID")
    private final String paymentStatus;

    @ApiModelProperty(value = "일정 상태", example = "UPCOMING")
    private final String scheduleStatus;

    @ApiModelProperty(value = "장소명", example = "루브르 박물관")
    private final String placeName;

    @ApiModelProperty(
            value = "장소 주소",
            example = "Rue de Rivoli, Paris"
    )
    private final String placeAddress;

    @ApiModelProperty(
            value = "일정 메모",
            example = "입장 10분 전까지 도착"
    )
    private final String memo;
}