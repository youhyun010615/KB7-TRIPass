package com.tripass.checklist.service;

import com.tripass.checklist.domain.DDayStage;
import com.tripass.checklist.domain.TripChecklistItem;
import com.tripass.checklist.dto.*;
import com.tripass.checklist.exception.ChecklistErrorCode;
import com.tripass.checklist.exception.ChecklistException;
import com.tripass.checklist.mapper.ChecklistMapper;
import com.tripass.travel.domain.Trip;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChecklistService {

    private final ChecklistMapper checklistMapper;

    /**
     * 여행 생성 시 기본 체크리스트 항목 초기화 (이미 생성되었으면 스킵)
     */
    @Transactional
    public void initializeChecklist(Long tripId) {
        // 이미 생성된 체크리스트가 있는지 확인
        if (checklistMapper.selectChecklistSummaryByTripId(tripId).getTotalItemCount() > 0) {
            return;
        }
        checklistMapper.insertDefaultChecklistsFromTemplate(tripId);
    }

    /**
     * 1. 여행 체크리스트 전체 현황 요약 조회
     */
    public ChecklistSummaryResponseDto getChecklistSummary(Long tripId, Long currentUserId) {
        validateTripOwner(tripId, currentUserId);

        ChecklistSummaryResponseDto summary = checklistMapper.selectChecklistSummaryByTripId(tripId);

        if (summary == null || summary.getTotalItemCount() == 0) {
            return ChecklistSummaryResponseDto.builder()
                    .totalCompletedCount(0)
                    .totalItemCount(0)
                    .prepCompletedCount(0)
                    .prepItemCount(0)
                    .returnCompletedCount(0)
                    .returnItemCount(0)
                    .build();
        }

        return summary;
    }

    /**
     * 2. 단계별 체크리스트 상세 목록 조회 (실시간 D-Day 계산에 의한 이월 UPDATE 및 그룹화)
     */
    @Transactional
    public ChecklistGroupResponseDto getChecklists(Long tripId, String type, String ddayStage, Long currentUserId) {
        Trip trip = validateTripOwnerAndGetTrip(tripId, currentUserId);
        String checklistType = "PREV_TRAVEL".equals(type) ? "PRE_TRAVEL" : type;

        // 1. 타입별 ddayStage 검증 및 정규화
        if ("PRE_TRAVEL".equals(checklistType)) {
            // PRE_TRAVEL 타입은 D30, D7, D1 중 하나인지 필수 검증 (null, D99 입력 시 400 Bad Request)
            DDayStage stage = DDayStage.from(ddayStage);
            ddayStage = stage.getValue();

            long daysUntilTrip = ChronoUnit.DAYS.between(LocalDate.now(), trip.getStartDate());
            if (daysUntilTrip <= 7) {
                checklistMapper.updateCarriedOverStatus(tripId, daysUntilTrip);
            }
        } else if ("RETURN".equals(checklistType)) {
            // RETURN(귀국) 타입에 ddayStage 값이 들어오면 거부 (400 Bad Request)
            if (ddayStage != null && !ddayStage.isBlank()) {
                throw new ChecklistException(ChecklistErrorCode.INVALID_INPUT_VALUE,
                        "RETURN(귀국) 타입은 ddayStage 파라미터를 가질 수 없습니다.");
            }
            ddayStage = null; // 명시적 null 정규화
        }

        // 2. DB에서 항목 조회
        List<ChecklistResponseDto> allItems = checklistMapper.selectChecklistsByTripIdAndType(tripId, checklistType, ddayStage);

        final String targetDdayStage = ddayStage;
        final String finalChecklistType = checklistType;

        // 3. 이월 목록 및 현재 단계 목록 그룹화
        List<ChecklistResponseDto> carriedOverChecklists;
        List<ChecklistResponseDto> currentChecklists;

        if ("RETURN".equals(finalChecklistType)) {
            // 💡 RETURN 타입: 이월 항목은 무조건 0건, 전체 항목이 이번 단계(귀국) 항목으로 진입!
            carriedOverChecklists = List.of();
            currentChecklists = allItems;
        } else {
            // 💡 PRE_TRAVEL 타입: DB에서 이월(is_carried_over = true)로 판정된 항목만 carriedOverChecklists로 분류
            carriedOverChecklists = allItems.stream()
                    .filter(item -> Boolean.TRUE.equals(item.getIsCarriedOver())
                            && !Objects.equals(targetDdayStage, item.getDdayStage()))
                    .collect(Collectors.toList());

            // 자기 본래 탭(ddayStage가 동일한 항목) 항목만 currentChecklists로 분류
            currentChecklists = allItems.stream()
                    .filter(item -> Objects.equals(targetDdayStage, item.getDdayStage()))
                    .collect(Collectors.toList());
        }

        return ChecklistGroupResponseDto.builder()
                .carriedOverChecklists(carriedOverChecklists)
                .currentChecklists(currentChecklists)
                .build();
    }

    /**
     * 3. 체크리스트 항목 완료 상태 토글/변경
     */
    @Transactional
    public ChecklistToggleResponseDto toggleChecklistItem(Long tripId, Long itemId, ChecklistToggleRequestDto request, Long currentUserId) {
        // 1. Request Body 유효성 검증 (isCompleted가 null이면 400 Bad Request)
        if (request == null || request.getIsCompleted() == null) {
            throw new ChecklistException(ChecklistErrorCode.INVALID_INPUT_VALUE, "isCompleted 값은 필수입니다.");
        }

        // 2. [조회 1] 여행 존재 여부 및 유저 소유권 조회/검증 (400, 404, 403)
        validateTripOwner(tripId, currentUserId);

        // 3. [조회 2] 해당 여행(tripId)에 속한 체크리스트 항목이 실제로 존재하는지 DB 조회 (404 CHECKLIST_ITEM_NOT_FOUND)
        boolean itemExists = checklistMapper.existsChecklistItem(tripId, itemId);
        if (!itemExists) {
            throw new ChecklistException(ChecklistErrorCode.CHECKLIST_ITEM_NOT_FOUND,
                    "해당 체크리스트 항목을 찾을 수 없습니다. (itemId: " + itemId + ")");
        }

        // 4. [수정] 모든 조회가 무사히 통과되면 비로소 완료 상태 UPDATE 실행!
        checklistMapper.updateChecklistItemCompletion(itemId, request.getIsCompleted());

        // 5. 변경된 결과 반환
        return ChecklistToggleResponseDto.builder()
                .id(itemId)
                .isCompleted(request.getIsCompleted())
                .build();
    }

    /**
     * 4. 커스텀 체크리스트 항목 추가
     */
    @Transactional
    public ChecklistCreateResponseDto createChecklistItem(Long tripId, ChecklistCreateRequestDto request, Long currentUserId) {
        // 1. Request Body 기본 유효성 검증 (400 BAD_REQUEST)
        if (request == null || request.getItemName() == null || request.getItemName().trim().isEmpty()) {
            throw new ChecklistException(ChecklistErrorCode.INVALID_INPUT_VALUE, "항목명(itemName)은 필수 입력값입니다.");
        }

        String type = request.getChecklistType();
        if (!"PRE_TRAVEL".equals(type) && !"PREV_TRAVEL".equals(type) && !"RETURN".equals(type)) {
            throw new ChecklistException(ChecklistErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 checklistType 입력값입니다. (PRE_TRAVEL / RETURN)");
        }

        // 2. 여행 존재 여부 및 소유권 검증 (400, 404, 403)
        validateTripOwner(tripId, currentUserId);

        // 3. 타입 명세 호환성 처리 (PREV_TRAVEL -> PRE_TRAVEL) 및 ddayStage 검증/정규화
        String checklistType = "PREV_TRAVEL".equals(type) ? "PRE_TRAVEL" : type;
        String ddayStage;

        if ("PRE_TRAVEL".equals(checklistType)) {
            // PRE_TRAVEL 타입은 D30, D7, D1 중 하나인지 필수 검증 (null, D99 등 입력 시 400 BAD_REQUEST)
            DDayStage stage = DDayStage.from(request.getDdayStage());
            ddayStage = stage.getValue();
        } else {
            // RETURN 타입 생성 시 ddayStage 값이 들어온 경우 400 BAD_REQUEST 거부
            if (request.getDdayStage() != null && !request.getDdayStage().isBlank()) {
                throw new ChecklistException(ChecklistErrorCode.INVALID_INPUT_VALUE,
                        "RETURN(귀국) 타입 생성 시 ddayStage를 입력할 수 없습니다.");
            }
            ddayStage = null; // 명시적 null 정규화
        }

        // 4. DB Insert 객체 준비
        TripChecklistItem newItem = TripChecklistItem.builder()
                .tripId(tripId)
                .checklistType(checklistType)
                .ddayStage(ddayStage)
                .itemName(request.getItemName().trim())
                .isCompleted(false)
                .isExcluded(false)
                .isCustom(true)         // 커스텀 항목 (is_custom = 1)
                .isCarriedOver(false)
                .isDeleted(0)
                .build();

        // 5. DB Insert 실행 (useGeneratedKeys로 생성된 PK 발급)
        checklistMapper.insertChecklistItem(newItem);

        // 6. Response 전달
        return ChecklistCreateResponseDto.builder()
                .id(newItem.getId())
                .build();
    }

    /**
     * 체크리스트 항목 삭제 (커스텀 항목만 삭제 가능)
     */
    @Transactional
    public ChecklistDeleteResponseDto deleteChecklistItem(Long tripId, Long itemId, Long currentUserId) {
        // 1. 여행 존재 여부 및 소유권 검증 (400, 404, 403)
        validateTripOwner(tripId, currentUserId);

        // 2. 체크리스트 항목 정보 조회 (존재 여부 검증)
        TripChecklistItem item = checklistMapper.selectChecklistItemById(tripId, itemId);
        if (item == null) {
            throw new ChecklistException(ChecklistErrorCode.CHECKLIST_ITEM_NOT_FOUND,
                    "해당 체크리스트 항목을 찾을 수 없습니다. (itemId: " + itemId + ")");
        }

        // 3. 기본 제공 항목(is_custom = false) 삭제 시도 시 예외 처리 (400 Bad Request)
        if (!Boolean.TRUE.equals(item.getIsCustom())) {
            throw new ChecklistException(ChecklistErrorCode.DEFAULT_ITEM_CANNOT_BE_DELETED);
        }

        // 4. Soft Delete (is_deleted = 1) 실행
        checklistMapper.deleteChecklistItem(itemId);

        // 5. Response 반환
        return ChecklistDeleteResponseDto.builder()
                .id(itemId)
                .build();
    }

    /**
     * 공통 소유권 인가 검증
     */
    private void validateTripOwner(Long tripId, Long currentUserId) {
        validateTripOwnerAndGetTrip(tripId, currentUserId);
    }

    /**
     * 공통 여행 ID 유효성 & 소유권 검증 후 Trip 객체 반환
     */
    private Trip validateTripOwnerAndGetTrip(Long tripId, Long currentUserId) {
        // 1. Path Variable 유효성 검증 (400 BAD_REQUEST)
        if (tripId == null || tripId <= 0) {
            throw new ChecklistException(ChecklistErrorCode.INVALID_PATH_VARIABLE);
        }

        // 2. 여행 데이터 조회 (404 NOT_FOUND)
        Trip trip = checklistMapper.selectTripById(tripId);
        if (trip == null) {
            throw new ChecklistException(ChecklistErrorCode.TRIP_NOT_FOUND);
        }

        // 3. 여행 소유권(작성자) 대조 검증 (403 FORBIDDEN)
        if (!trip.getUserId().equals(currentUserId)) {
            throw new ChecklistException(ChecklistErrorCode.FORBIDDEN_TRIP_ACCESS);
        }

        return trip;
    }
}