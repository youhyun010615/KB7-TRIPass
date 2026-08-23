package com.tripass.schedule.service;

import com.tripass.schedule.dto.*;
import com.tripass.dev.util.DevDateUtil;
import com.tripass.schedule.exception.ScheduleException;
import com.tripass.schedule.mapper.ScheduleMapper;
import com.tripass.schedule.enums.SchedulePaymentStatus;
import com.tripass.schedule.enums.ScheduleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static com.tripass.schedule.exception.ScheduleErrorCode.CURRENCY_NOT_FOUND;
import static com.tripass.schedule.exception.ScheduleErrorCode.SCHEDULE_DATE_OUT_OF_RANGE;
import static com.tripass.schedule.exception.ScheduleErrorCode.SCHEDULE_NOT_FOUND;
import static com.tripass.schedule.exception.ScheduleErrorCode.TRIP_ACCESS_DENIED;
import static com.tripass.schedule.exception.ScheduleErrorCode.TRIP_COUNTRY_NOT_FOUND;
import static com.tripass.schedule.exception.ScheduleErrorCode.TRIP_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleMapper scheduleMapper;
    private final DevDateUtil devDateUtil;

    /** 여행에 등록된 일정 목록을 조회합니다. */
    public List<ScheduleListResponseDto> getSchedules(
            Long tripId,
            Long userId
    ) {
        validateTripOwnership(tripId, userId);

        return scheduleMapper.findAllByTripId(tripId)
                .stream()
                .map(row -> toListResponse(row, devDateUtil.today(userId)))
                .toList();
    }

    /** 여행에 등록된 특정 일정의 상세 정보를 조회합니다. */
    public ScheduleDetailResponseDto getScheduleDetail(
            Long tripId,
            Long scheduleId,
            Long userId
    ) {
        validateTripOwnership(tripId, userId);

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
            ScheduleCreateRequestDto request,
            Long userId
    ) {
        validateTripOwnership(tripId, userId);

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

        validateScheduledAtWithinCountryPeriod(
                request.getScheduledAt().toLocalDate(),
                tripCountry
        );

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

    /** 여행이 존재하고, 로그인한 사용자가 그 여행의 소유자인지 검증합니다. */
    private void validateTripOwnership(Long tripId, Long userId) {
        Long ownerId = scheduleMapper.findTripUserId(tripId);

        if (ownerId == null) {
            throw new ScheduleException(
                    TRIP_NOT_FOUND
            );
        }

        if (!ownerId.equals(userId)) {
            throw new ScheduleException(
                    TRIP_ACCESS_DENIED
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

    /** 일정 날짜가 선택한 여행 국가의 방문(도착~출발) 기간 안인지 검증합니다. */
    private void validateScheduledAtWithinCountryPeriod(
            LocalDate scheduledDate,
            TripCountryContextRowDto tripCountry
    ) {
        if (scheduledDate.isBefore(tripCountry.getArrivalDate())
                || scheduledDate.isAfter(tripCountry.getDepartureDate())) {
            throw new ScheduleException(
                    SCHEDULE_DATE_OUT_OF_RANGE
            );
        }
    }

    /** 목록 조회 결과를 API 응답으로 변환합니다. */
    private ScheduleListResponseDto toListResponse(
            ScheduleListRowDto row,
            LocalDate today
    ) {
        ZoneId zoneId = row.getTimeZone() != null
                ? ZoneId.of(row.getTimeZone())
                : ZoneId.of("Asia/Seoul");

        OffsetDateTime scheduledAt = toOffsetDateTime(row.getScheduledAt(), zoneId);
        ScheduleStatus visibleStatus = scheduledAt.toLocalDate().isBefore(today)
                ? ScheduleStatus.DONE
                : ScheduleStatus.UPCOMING;

        return ScheduleListResponseDto.builder()
                .id(row.getId())
                .tripId(row.getTripId())
                .tripCountryId(
                        row.getTripCountryId()
                )
                .countryName(row.getCountryName())
                .timeZone(row.getTimeZone())
                .scheduleName(row.getScheduleName())
                .scheduledAt(scheduledAt)
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
                .scheduleStatus(visibleStatus)
                .placeName(row.getPlaceName())
                .placeAddress(row.getPlaceAddress())
                .build();
    }

    /** 상세 조회 결과를 API 응답으로 변환합니다. */
    private ScheduleDetailResponseDto toDetailResponse(
            ScheduleDetailRowDto row
    ) {
        ZoneId zoneId = row.getTimeZone() != null
                ? ZoneId.of(row.getTimeZone())
                : ZoneId.of("Asia/Seoul");

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

    /** 여행 국가의 현지 시간을 기준으로 기존 일정을 수정합니다. */
    @Transactional
    public ScheduleUpdateResponseDto updateSchedule(
            Long tripId,
            Long scheduleId,
            ScheduleUpdateRequestDto request,
            Long userId
    ) {
        validateTripOwnership(tripId, userId);
        validateScheduleExists(
                tripId,
                scheduleId
        );

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

        validateScheduledAtWithinCountryPeriod(
                request.getScheduledAt().toLocalDate(),
                tripCountry
        );

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

        ScheduleUpdateCommandDto command =
                ScheduleUpdateCommandDto.builder()
                        .tripId(tripId)
                        .scheduleId(scheduleId)
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
                                request.getScheduleStatus()
                                        .name()
                        )
                        .placeName(request.getPlaceName())
                        .placeAddress(
                                request.getPlaceAddress()
                        )
                        .memo(request.getMemo())
                        .build();

        scheduleMapper.updateSchedule(command);

        return ScheduleUpdateResponseDto.builder()
                .scheduleId(scheduleId)
                .build();
    }

    /** 일정이 해당 여행에 존재하는지 검증합니다. */
    private void validateScheduleExists(
            Long tripId,
            Long scheduleId
    ) {
        boolean exists =
                scheduleMapper
                        .existsScheduleByTripIdAndScheduleId(
                                tripId,
                                scheduleId
                        );

        if (!exists) {
            throw new ScheduleException(
                    SCHEDULE_NOT_FOUND
            );
        }
    }

    /** 여행에 등록된 일정을 삭제(soft delete)합니다. */
    @Transactional
    public void deleteSchedule(
            Long tripId,
            Long scheduleId,
            Long userId
    ) {
        validateTripOwnership(tripId, userId);

        int deletedCount =
                scheduleMapper.softDeleteSchedule(
                        tripId,
                        scheduleId
                );

        if (deletedCount == 0) {
            throw new ScheduleException(
                    SCHEDULE_NOT_FOUND
            );
        }
    }
}
