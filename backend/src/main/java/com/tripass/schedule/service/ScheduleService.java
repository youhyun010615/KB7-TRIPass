package com.tripass.schedule.service;

import com.tripass.schedule.dto.ScheduleCreateCommandDto;
import com.tripass.schedule.dto.ScheduleCreateRequestDto;
import com.tripass.schedule.dto.ScheduleCreateResponseDto;
import com.tripass.schedule.dto.ScheduleDetailResponseDto;
import com.tripass.schedule.dto.ScheduleDetailRowDto;
import com.tripass.schedule.dto.ScheduleListResponseDto;
import com.tripass.schedule.dto.ScheduleListRowDto;
import com.tripass.schedule.dto.TripCountryContextRowDto;
import com.tripass.schedule.exception.ScheduleException;
import com.tripass.schedule.mapper.ScheduleMapper;
import com.tripass.schedule.enums.SchedulePaymentStatus;
import com.tripass.schedule.enums.ScheduleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static com.tripass.schedule.exception.ScheduleErrorCode.CURRENCY_NOT_FOUND;
import static com.tripass.schedule.exception.ScheduleErrorCode.SCHEDULE_NOT_FOUND;
import static com.tripass.schedule.exception.ScheduleErrorCode.TRIP_COUNTRY_NOT_FOUND;
import static com.tripass.schedule.exception.ScheduleErrorCode.TRIP_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleMapper scheduleMapper;

    /** 여행에 등록된 일정 목록을 조회합니다. */
    public List<ScheduleListResponseDto> getSchedules(
            Long tripId
    ) {
        validateTripExists(tripId);

        return scheduleMapper.findAllByTripId(tripId)
                .stream()
                .map(this::toListResponse)
                .toList();
    }

    /** 여행에 등록된 특정 일정의 상세 정보를 조회합니다. */
    public ScheduleDetailResponseDto getScheduleDetail(
            Long tripId,
            Long scheduleId
    ) {
        validateTripExists(tripId);

        ScheduleDetailRowDto row =
                scheduleMapper
                        .findDetailByTripIdAndScheduleId(
                                tripId,
                                scheduleId
                        );

        if (row == null) {
            throw new ScheduleException(
                    SCHEDULE_NOT_FOUND
            );
        }

        return toDetailResponse(row);
    }

    /** 선택한 여행 국가의 현지 시간을 기준으로 일정을 등록합니다. */
    @Transactional
    public ScheduleCreateResponseDto createSchedule(
            Long tripId,
            ScheduleCreateRequestDto request
    ) {
        validateTripExists(tripId);

        TripCountryContextRowDto tripCountry =
                scheduleMapper.findTripCountryContext(
                        tripId,
                        request.getTripCountryId()
                );

        if (tripCountry == null) {
            throw new ScheduleException(
                    TRIP_COUNTRY_NOT_FOUND
            );
        }

        ZoneId zoneId = ZoneId.of(
                tripCountry.getTimeZone()
        );

        Long currencyId = resolveCurrencyId(
                request.getCurrencyCode(),
                tripCountry.getDefaultCurrencyId()
        );

        Instant scheduledInstant =
                request.getScheduledAt()
                        .atZone(zoneId)
                        .toInstant();

        ScheduleCreateCommandDto command =
                ScheduleCreateCommandDto.builder()
                        .tripId(tripId)
                        .tripCountryId(
                                request.getTripCountryId()
                        )
                        .currencyId(currencyId)
                        .scheduleName(
                                request.getScheduleName()
                        )
                        .scheduledAt(
                                Timestamp.from(
                                        scheduledInstant
                                )
                        )
                        .amount(request.getAmount())
                        .paymentStatus(
                                request.getPaymentStatus()
                                        .name()
                        )
                        .scheduleStatus(
                                ScheduleStatus.UPCOMING
                                        .name()
                        )
                        .placeName(request.getPlaceName())
                        .placeAddress(
                                request.getPlaceAddress()
                        )
                        .memo(request.getMemo())
                        .build();

        scheduleMapper.insertSchedule(command);

        return ScheduleCreateResponseDto.builder()
                .scheduleId(command.getId())
                .build();
    }

    /** 여행 존재 여부를 검증합니다. */
    private void validateTripExists(Long tripId) {
        if (!scheduleMapper.existsTripById(tripId)) {
            throw new ScheduleException(
                    TRIP_NOT_FOUND
            );
        }
    }

    /** 선택 통화 또는 국가 기본 통화 ID를 반환합니다. */
    private Long resolveCurrencyId(
            String currencyCode,
            Long defaultCurrencyId
    ) {
        if (currencyCode == null) {
            return defaultCurrencyId;
        }

        Long currencyId =
                scheduleMapper.findCurrencyIdByCode(
                        currencyCode
                );

        if (currencyId == null) {
            throw new ScheduleException(
                    CURRENCY_NOT_FOUND
            );
        }

        return currencyId;
    }

    /** 목록 조회 결과를 API 응답으로 변환합니다. */
    private ScheduleListResponseDto toListResponse(
            ScheduleListRowDto row
    ) {
        ZoneId zoneId = ZoneId.of(
                row.getTimeZone()
        );

        return ScheduleListResponseDto.builder()
                .id(row.getId())
                .tripId(row.getTripId())
                .tripCountryId(
                        row.getTripCountryId()
                )
                .countryName(row.getCountryName())
                .timeZone(row.getTimeZone())
                .scheduleName(row.getScheduleName())
                .scheduledAt(
                        toOffsetDateTime(
                                row.getScheduledAt(),
                                zoneId
                        )
                )
                .amount(row.getAmount())
                .currencyCode(row.getCurrencyCode())
                .currencySymbol(
                        row.getCurrencySymbol()
                )
                .paymentStatus(
                        SchedulePaymentStatus.valueOf(
                                row.getPaymentStatus()
                        )
                )
                .scheduleStatus(
                        ScheduleStatus.valueOf(
                                row.getScheduleStatus()
                        )
                )
                .placeName(row.getPlaceName())
                .placeAddress(row.getPlaceAddress())
                .build();
    }

    /** 상세 조회 결과를 API 응답으로 변환합니다. */
    private ScheduleDetailResponseDto toDetailResponse(
            ScheduleDetailRowDto row
    ) {
        ZoneId zoneId = ZoneId.of(
                row.getTimeZone()
        );

        return ScheduleDetailResponseDto.builder()
                .id(row.getId())
                .tripId(row.getTripId())
                .tripCountryId(
                        row.getTripCountryId()
                )
                .countryName(row.getCountryName())
                .timeZone(row.getTimeZone())
                .scheduleName(row.getScheduleName())
                .scheduledAt(
                        toOffsetDateTime(
                                row.getScheduledAt(),
                                zoneId
                        )
                )
                .amount(row.getAmount())
                .currencyCode(row.getCurrencyCode())
                .currencySymbol(
                        row.getCurrencySymbol()
                )
                .paymentStatus(
                        SchedulePaymentStatus.valueOf(
                                row.getPaymentStatus()
                        )
                )
                .scheduleStatus(
                        ScheduleStatus.valueOf(
                                row.getScheduleStatus()
                        )
                )
                .placeName(row.getPlaceName())
                .placeAddress(row.getPlaceAddress())
                .memo(row.getMemo())
                .build();
    }

    /** DB의 UTC Timestamp를 국가 현지 시간으로 변환합니다. */
    private OffsetDateTime toOffsetDateTime(
            Timestamp timestamp,
            ZoneId zoneId
    ) {
        if (timestamp == null) {
            return null;
        }

        return timestamp.toInstant()
                .atZone(zoneId)
                .toOffsetDateTime();
    }
}