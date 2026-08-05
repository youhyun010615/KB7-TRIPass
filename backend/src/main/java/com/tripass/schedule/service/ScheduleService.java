package com.tripass.schedule.service;

import com.tripass.schedule.dto.ScheduleDetailResponseDto;
import com.tripass.schedule.dto.ScheduleDetailRowDto;
import com.tripass.schedule.dto.ScheduleListResponseDto;
import com.tripass.schedule.dto.ScheduleListRowDto;
import com.tripass.schedule.exception.ScheduleException;
import com.tripass.schedule.mapper.ScheduleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static com.tripass.schedule.exception.ScheduleErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleMapper scheduleMapper;

    /**
     * 여행 존재 여부를 확인한 후 해당 여행의 일정 목록을 조회하고,
     * 각 일정의 UTC 시각을 여행 국가 현지 시간으로 변환해 반환합니다.
     *
     * @param tripId 조회할 여행 ID
     * @return 여행 국가 현지 시간으로 변환된 일정 목록
     * @throws ScheduleException 여행 ID가 잘못됐거나 여행이 존재하지 않는 경우
     */
    public List<ScheduleListResponseDto> getSchedules(
            Long tripId
    ) {

        if (!scheduleMapper.existsTripById(tripId)) {
            throw new ScheduleException(TRIP_NOT_FOUND);
        }

        return scheduleMapper.findAllByTripId(tripId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Mapper에서 조회한 일정 데이터를 API 응답 DTO로 변환합니다.
     * DB의 UTC 시각에는 일정 국가의 시간대를 적용합니다.
     *
     * @param row DB에서 조회한 일정 데이터
     * @return API 응답용 일정 DTO
     */
    private ScheduleListResponseDto toResponse(
            ScheduleListRowDto row
    ) {
        ZoneId zoneId = ZoneId.of(row.getTimeZone());

        return ScheduleListResponseDto.builder()
                .id(row.getId())
                .tripId(row.getTripId())
                .tripCountryId(row.getTripCountryId())
                .countryName(row.getCountryName())
                .timeZone(row.getTimeZone())
                .scheduleName(row.getScheduleName())
                .startAt(
                        toOffsetDateTime(
                                row.getStartAt(),
                                zoneId
                        )
                )
                .endAt(
                        toOffsetDateTime(
                                row.getEndAt(),
                                zoneId
                        )
                )
                .amount(row.getAmount())
                .currencyCode(row.getCurrencyCode())
                .currencySymbol(row.getCurrencySymbol())
                .paymentStatus(row.getPaymentStatus())
                .scheduleStatus(row.getScheduleStatus())
                .placeName(row.getPlaceName())
                .placeAddress(row.getPlaceAddress())
                .build();
    }

    /**
     * DB에서 조회한 UTC Timestamp를 지정한 국가 시간대의
     * OffsetDateTime으로 변환합니다.
     *
     * @param timestamp DB에서 조회한 UTC 시각
     * @param zoneId 적용할 여행 국가 시간대
     * @return 국가 오프셋이 포함된 시각 또는 null
     */
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


    /**
     * 여행 및 일정 존재 여부를 검증한 후 여행 일정 상세 정보를 조회하고,
     * UTC 시각을 여행 국가 현지 시간으로 변환해 반환합니다.
     *
     * @param tripId 여행 ID
     * @param scheduleId 여행 일정 ID
     * @return 여행 일정 상세 정보
     */
    public ScheduleDetailResponseDto getScheduleDetail(
            Long tripId,
            Long scheduleId
    ) {

        if (!scheduleMapper.existsTripById(tripId)) {
            throw new ScheduleException(TRIP_NOT_FOUND);
        }

        ScheduleDetailRowDto row =
                scheduleMapper.findDetailByTripIdAndScheduleId(
                        tripId,
                        scheduleId
                );

        if (row == null) {
            throw new ScheduleException(SCHEDULE_NOT_FOUND);
        }

        return toDetailResponse(row);
    }

    /**
     * DB에서 조회한 상세 정보를 API 상세 응답으로 변환합니다.
     *
     * @param row DB 일정 상세 조회 결과
     * @return 일정 상세 API 응답
     */
    private ScheduleDetailResponseDto toDetailResponse(
            ScheduleDetailRowDto row
    ) {
        ZoneId zoneId = ZoneId.of(row.getTimeZone());

        return ScheduleDetailResponseDto.builder()
                .id(row.getId())
                .tripId(row.getTripId())
                .tripCountryId(row.getTripCountryId())
                .countryName(row.getCountryName())
                .timeZone(row.getTimeZone())
                .scheduleName(row.getScheduleName())
                .startAt(
                        toOffsetDateTime(
                                row.getStartAt(),
                                zoneId
                        )
                )
                .endAt(
                        toOffsetDateTime(
                                row.getEndAt(),
                                zoneId
                        )
                )
                .amount(row.getAmount())
                .currencyCode(row.getCurrencyCode())
                .currencySymbol(row.getCurrencySymbol())
                .paymentStatus(row.getPaymentStatus())
                .scheduleStatus(row.getScheduleStatus())
                .placeName(row.getPlaceName())
                .placeAddress(row.getPlaceAddress())
                .memo(row.getMemo())
                .build();
    }
}