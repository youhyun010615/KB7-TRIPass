package com.tripass.ocr.service;

import com.tripass.common.exception.CustomException;
import com.tripass.ocr.dto.internal.ReceiptDetailRow;
import com.tripass.ocr.dto.internal.ReceiptImageData;
import com.tripass.ocr.dto.internal.ReceiptSummaryRow;
import com.tripass.ocr.dto.internal.StoredReceiptFile;
import com.tripass.ocr.dto.internal.ValidatedReceiptImage;
import com.tripass.ocr.dto.request.ReceiptItemSaveRequest;
import com.tripass.ocr.dto.request.ReceiptSaveRequest;
import com.tripass.ocr.dto.response.ReceiptDetailResponse;
import com.tripass.ocr.dto.response.ReceiptItemResponse;
import com.tripass.ocr.dto.response.ReceiptSummaryResponse;
import com.tripass.ocr.dto.response.ReceiptParticipantResponse;
import com.tripass.ocr.mapper.ReceiptMapper;
import com.tripass.ocr.model.Receipt;
import com.tripass.ocr.model.ReceiptItem;
import com.tripass.ocr.model.ReceiptParticipant;
import com.tripass.ocr.service.storage.ReceiptFileStorage;
import com.tripass.ocr.service.validation.ReceiptImageValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;

// 해외 영수증 저장·조회·수정·삭제 기능 구현체
@Service
@RequiredArgsConstructor
@Log4j2
public class ReceiptServiceImpl
        implements ReceiptService {

    private static final String COMPLETED_STATUS =
            "COMPLETED";

    private static final int SPLIT_AMOUNT_SCALE = 2;

    private final ReceiptMapper receiptMapper;
    private final ReceiptImageValidator receiptImageValidator;
    private final ReceiptFileStorage receiptFileStorage;

    // 이미지 파일과 영수증 정보를 특정 여행에 저장한다.
    @Override
    @Transactional
    public ReceiptDetailResponse createReceipt(
            Long userId,
            Long tripId,
            ReceiptSaveRequest request,
            MultipartFile receiptImage
    ) {
        validateUserId(userId);
        validateTripId(tripId);
        validateRequest(request);
        validateReferenceData(
                userId,
                tripId,
                request
        );

        StoredReceiptFile storedFile =
                storeReceiptImageIfPresent(receiptImage);

        try {
            Receipt receipt = createReceiptModel(
                    userId,
                    tripId,
                    request,
                    storedFile
            );

            int insertedReceiptRows =
                    receiptMapper.insertReceipt(receipt);

            if (insertedReceiptRows != 1
                    || receipt.getId() == null) {
                throw new CustomException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "RECEIPT_SAVE_FAILED",
                        "영수증 저장에 실패했습니다."
                );
            }

            List<ReceiptItem> receiptItems =
                    createReceiptItems(
                            receipt.getId(),
                            request.getItems()
                    );

            insertReceiptItems(receiptItems);

            List<ReceiptParticipant> participants =
                    createReceiptParticipants(
                            receipt.getId(),
                            request.getParticipantNames()
                    );

            insertReceiptParticipants(participants);

            return getReceipt(
                    userId,
                    tripId,
                    receipt.getId()
            );

        } catch (RuntimeException exception) {
            if (storedFile != null) {
                deleteStoredFileAfterFailure(
                        storedFile.getStoredPath()
                );
            }

            throw exception;
        }
    }

    // 로그인 회원의 영수증 목록을 조회한다.
    @Override
    @Transactional(readOnly = true)
    public List<ReceiptSummaryResponse> getReceipts(
            Long userId,
            Long tripId
    ) {
        validateUserId(userId);
        validateTripId(tripId);

        if (!receiptMapper.existsTripByIdAndUserId(
                tripId,
                userId
        )) {
            throw new CustomException(
                    HttpStatus.NOT_FOUND,
                    "RECEIPT_TRIP_NOT_FOUND",
                    "여행 정보를 찾을 수 없습니다."
            );
        }

        List<ReceiptSummaryRow> rows =
                receiptMapper.findAllByUserIdAndTripId(
                        userId,
                        tripId
                );

        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        return rows.stream()
                .map(this::createSummaryResponse)
                .toList();
    }

    // 로그인 회원의 특정 여행 영수증 상세 정보를 조회한다.
    @Override
    @Transactional(readOnly = true)
    public ReceiptDetailResponse getReceipt(
            Long userId,
            Long tripId,
            Long receiptId
    ) {
        validateUserId(userId);
        validateTripId(tripId);
        validateReceiptId(receiptId);

        ReceiptDetailRow receiptRow =
                requireReceipt(
                        userId,
                        tripId,
                        receiptId
                );

        List<ReceiptItem> receiptItems =
                receiptMapper.findItemsByReceiptIdAndUserIdAndTripId(
                        receiptId,
                        userId,
                        tripId
                );

        List<ReceiptItemResponse> itemResponses =
                receiptItems == null
                        ? Collections.emptyList()
                        : receiptItems.stream()
                        .map(this::createItemResponse)
                        .toList();

        List<ReceiptParticipant> participants =
                receiptMapper.findParticipantsByReceiptIdAndUserIdAndTripId(
                        receiptId,
                        userId,
                        tripId
                );

        List<ReceiptParticipantResponse> participantResponses =
                participants == null
                        ? Collections.emptyList()
                        : participants.stream()
                        .map(this::createParticipantResponse)
                        .toList();

        return createDetailResponse(
                receiptRow,
                itemResponses,
                participantResponses
        );
    }

    // 특정 여행의 영수증 정보와 품목을 일괄 수정한다.
    @Override
    @Transactional
    public ReceiptDetailResponse updateReceipt(
            Long userId,
            Long tripId,
            Long receiptId,
            ReceiptSaveRequest request
    ) {
        // 1. 기본 요청값 검증
        validateUserId(userId);
        validateTripId(tripId);
        validateReceiptId(receiptId);
        validateRequest(request);

        // 2. 수정 대상 영수증 존재 및 접근 권한 확인
        requireReceipt(
                userId,
                tripId,
                receiptId
        );

        // 3. 여행, 국가, 통화, 카테고리 등 참조 데이터 검증
        validateReferenceData(
                userId,
                tripId,
                request
        );

        // 4. 영수증 기본 정보 수정
        Receipt receipt =
                createUpdatedReceiptModel(
                        userId,
                        tripId,
                        receiptId,
                        request
                );

        int updatedRows =
                receiptMapper.updateReceipt(receipt);

        if (updatedRows != 1) {
            throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "RECEIPT_UPDATE_FAILED",
                    "영수증 수정에 실패했습니다."
            );
        }

        /*
         * 5. 품목 ID를 기준으로 동기화
         *
         * - 기존 ID가 있는 품목: UPDATE
         * - ID가 없는 신규 품목: INSERT
         * - 요청에서 제외된 기존 품목: 논리 삭제
         *
         * 기존처럼 모든 품목을 논리 삭제한 뒤 다시 INSERT하지 않는다.
         */
        syncReceiptItems(
                userId,
                tripId,
                receiptId,
                request.getItems()
        );

        syncReceiptParticipants(
                userId,
                tripId,
                receiptId,
                request.getParticipantNames()
        );

        // 7. 수정 완료된 영수증 상세 정보를 다시 조회하여 반환
        return getReceipt(
                userId,
                tripId,
                receiptId
        );
    }

    // 특정 여행의 영수증과 관련 데이터를 논리 삭제한다.
    @Override
    @Transactional
    public void deleteReceipt(
            Long userId,
            Long tripId,
            Long receiptId
    ) {
        validateUserId(userId);
        validateTripId(tripId);
        validateReceiptId(receiptId);

        requireReceipt(
                userId,
                tripId,
                receiptId
        );

        receiptMapper.softDeleteItemsByReceiptIdAndUserIdAndTripId(
                receiptId,
                userId,
                tripId
        );

        receiptMapper.softDeleteParticipantsByReceiptIdAndUserIdAndTripId(
                receiptId,
                userId,
                tripId
        );


        int deletedRows =
                receiptMapper
                        .softDeleteReceiptByIdAndUserIdAndTripId(
                                receiptId,
                                userId,
                                tripId
                        );

        if (deletedRows != 1) {
            throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "RECEIPT_DELETE_FAILED",
                    "영수증 삭제에 실패했습니다."
            );
        }

        /*
         * DB에서는 논리 삭제하지만 이미지 파일은 즉시 삭제하지 않는다.
         * 복구 또는 정리 작업을 고려하여 파일 정리 정책은 별도로 적용한다.
         */
    }

    // 회원과 여행 소유권을 확인한 후 영수증 이미지를 읽는다.
    @Override
    @Transactional(readOnly = true)
    public ReceiptImageData getReceiptImage(
            Long userId,
            Long tripId,
            Long receiptId
    ) {
        validateUserId(userId);
        validateTripId(tripId);
        validateReceiptId(receiptId);

        ReceiptDetailRow receiptRow =
                requireReceipt(
                        userId,
                        tripId,
                        receiptId
                );

        if (receiptRow.getFileUrl() == null
                || receiptRow.getFileUrl().isBlank()) {
            throw new CustomException(
                    HttpStatus.NOT_FOUND,
                    "RECEIPT_IMAGE_NOT_FOUND",
                    "저장된 영수증 이미지가 없습니다."
            );
        }

        byte[] imageBytes =
                receiptFileStorage.load(
                        receiptRow.getFileUrl()
                );

        return new ReceiptImageData(
                receiptRow.getFileName(),
                receiptRow.getFileType(),
                imageBytes
        );
    }

    // 영수증 저장 모델을 생성한다.
    private Receipt createReceiptModel(
            Long userId,
            Long tripId,
            ReceiptSaveRequest request,
            StoredReceiptFile storedFile
    ) {
        Receipt receipt = new Receipt();

        receipt.setUserId(userId);
        receipt.setTripId(tripId);
        receipt.setCountryId(request.getCountryId());
        receipt.setCurrencyId(
                resolveCurrencyId(request.getCurrencyCode())
        );
        receipt.setPaymentDateTime(
                request.getPaymentDateTime()
        );
        if (storedFile != null) {
            receipt.setFileName(
                    storedFile.getOriginalFileName()
            );
            receipt.setFileUrl(
                    storedFile.getStoredPath()
            );
            receipt.setFileType(
                    storedFile.getFileType()
            );
        } else {
            receipt.setFileName(null);
            receipt.setFileUrl(null);
            receipt.setFileType(null);
        }
        receipt.setStatus(COMPLETED_STATUS);
        receipt.setMerchantOriginalName(
                trimToNull(
                        request.getMerchantOriginalName()
                )
        );
        receipt.setMerchantTranslatedName(
                trimToNull(
                        request.getMerchantTranslatedName()
                )
        );
        receipt.setTotalAmount(
                request.getTotalAmount()
        );
        receipt.setOcrRawText(
                request.getOcrRawText()
        );
        receipt.setSplitCount(
                calculateSplitCount(request)
        );
        receipt.setErrorMessage(null);
        receipt.setProcessedAt(
                LocalDateTime.now()
        );
        receipt.setCategoryId(
                request.getCategoryId()
        );
        receipt.setMemo(
                trimToNull(request.getMemo())
        );

        return receipt;
    }

    // 영수증 수정 모델을 생성한다.
    private Receipt createUpdatedReceiptModel(
            Long userId,
            Long tripId,
            Long receiptId,
            ReceiptSaveRequest request
    ) {
        Receipt receipt = new Receipt();

        receipt.setId(receiptId);
        receipt.setUserId(userId);
        receipt.setTripId(tripId);
        receipt.setCountryId(request.getCountryId());
        receipt.setCurrencyId(resolveCurrencyId(request.getCurrencyCode()));
        receipt.setPaymentDateTime(
                request.getPaymentDateTime()
        );
        receipt.setMerchantOriginalName(
                trimToNull(
                        request.getMerchantOriginalName()
                )
        );
        receipt.setMerchantTranslatedName(
                trimToNull(
                        request.getMerchantTranslatedName()
                )
        );
        receipt.setTotalAmount(
                request.getTotalAmount()
        );
        receipt.setOcrRawText(
                request.getOcrRawText()
        );
        receipt.setSplitCount(
                calculateSplitCount(request)
        );
        receipt.setCategoryId(
                request.getCategoryId()
        );
        receipt.setMemo(
                trimToNull(request.getMemo())
        );

        return receipt;
    }

    // 수정 요청의 품목과 현재 활성 품목을 ID 기준으로 동기화한다.
    private void syncReceiptItems(
            Long userId,
            Long tripId,
            Long receiptId,
            List<ReceiptItemSaveRequest> itemRequests
    ) {
        List<ReceiptItemSaveRequest> requests =
                itemRequests == null
                        ? Collections.emptyList()
                        : itemRequests;

        // 요청에 포함된 기존 품목 ID
        List<Long> keptItemIds =
                new ArrayList<>();

        // 수정 화면에서 새로 추가된 품목
        List<ReceiptItem> newItems =
                new ArrayList<>();

        // 동일한 품목 ID가 요청에 중복되는 것을 방지
        Set<Long> requestedItemIds =
                new HashSet<>();

        for (int index = 0;
             index < requests.size();
             index++) {

            ReceiptItemSaveRequest request =
                    requests.get(index);

            if (request == null) {
                throw new CustomException(
                        HttpStatus.BAD_REQUEST,
                        "RECEIPT_ITEM_INVALID",
                        "영수증 품목 정보를 올바르게 입력해 주세요."
                );
            }

            ReceiptItem item =
                    createReceiptItem(
                            receiptId,
                            request,
                            index + 1
                    );

            /*
             * ID가 없으면 수정 화면에서
             * 새로 추가한 품목이므로 INSERT 대상이다.
             */
            if (request.getId() == null) {
                newItems.add(item);
                continue;
            }

            if (!requestedItemIds.add(request.getId())) {
                throw new CustomException(
                        HttpStatus.BAD_REQUEST,
                        "RECEIPT_ITEM_DUPLICATED",
                        "동일한 영수증 품목이 중복되었습니다."
                );
            }

            /*
             * 다른 회원 또는 다른 영수증의 품목 ID를
             * 임의로 수정하는 것을 방지한다.
             */
            boolean exists =
                    receiptMapper
                            .existsActiveItemByIdAndReceiptIdAndUserIdAndTripId(
                                    request.getId(),
                                    receiptId,
                                    userId,
                                    tripId
                            );

            if (!exists) {
                throw new CustomException(
                        HttpStatus.BAD_REQUEST,
                        "RECEIPT_ITEM_INVALID",
                        "수정할 영수증 품목을 확인해 주세요."
                );
            }

            item.setId(request.getId());

            int updatedRows =
                    receiptMapper.updateReceiptItem(
                            item,
                            userId,
                            tripId
                    );

            if (updatedRows != 1) {
                throw new CustomException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "RECEIPT_ITEM_UPDATE_FAILED",
                        "영수증 품목 수정에 실패했습니다."
                );
            }

            keptItemIds.add(request.getId());
        }

        /*
         * 기존 활성 품목 중 수정 요청에 없는 품목만
         * 논리 삭제한다.
         */
        receiptMapper.softDeleteMissingItems(
                receiptId,
                userId,
                tripId,
                keptItemIds
        );

        /*
         * ID가 없는 신규 품목만 INSERT한다.
         */
        insertReceiptItems(newItems);
    }

    // 품목 수정 요청을 ReceiptItem 모델로 변환한다.
    private ReceiptItem createReceiptItem(
            Long receiptId,
            ReceiptItemSaveRequest request,
            int displayOrder
    ) {
        ReceiptItem item =
                new ReceiptItem();

        item.setReceiptId(receiptId);

        item.setOriginalName(
                request.getOriginalName().trim()
        );

        item.setTranslatedName(
                trimToNull(
                        request.getTranslatedName()
                )
        );

        item.setQuantity(
                request.getQuantity()
        );

        item.setAmount(
                request.getAmount()
        );

        item.setDisplayOrder(
                displayOrder
        );

        return item;
    }

    // 공동결제 참여자를 기존 표시 순서 기준으로 동기화한다.
    private void syncReceiptParticipants(
            Long userId,
            Long tripId,
            Long receiptId,
            List<String> participantNames
    ) {
        List<String> requestedNames =
                participantNames == null
                        ? Collections.emptyList()
                        : participantNames;

        List<ReceiptParticipant> existingParticipants =
                receiptMapper
                        .findParticipantsByReceiptIdAndUserIdAndTripId(
                                receiptId,
                                userId,
                                tripId
                        );

        if (existingParticipants == null) {
            existingParticipants =
                    Collections.emptyList();
        }

        List<Long> keptParticipantIds =
                new ArrayList<>();

        List<ReceiptParticipant> newParticipants =
                new ArrayList<>();

        for (int index = 0;
             index < requestedNames.size();
             index++) {

            String participantName =
                    trimToNull(requestedNames.get(index));

            if (participantName == null) {
                throw new CustomException(
                        HttpStatus.BAD_REQUEST,
                        "RECEIPT_PARTICIPANT_INVALID",
                        "공동결제 참여자 이름을 입력해 주세요."
                );
            }

            ReceiptParticipant participant =
                    new ReceiptParticipant();

            participant.setReceiptId(receiptId);
            participant.setParticipantName(
                    participantName
            );
            participant.setDisplayOrder(
                    index + 1
            );

            /*
             * 현재 순서에 기존 참여자가 있으면
             * 기존 PK를 유지하면서 이름과 순서만 수정한다.
             */
            if (index < existingParticipants.size()) {
                ReceiptParticipant existingParticipant =
                        existingParticipants.get(index);

                participant.setId(
                        existingParticipant.getId()
                );

                int updatedRows =
                        receiptMapper.updateReceiptParticipant(
                                participant,
                                userId,
                                tripId
                        );

                if (updatedRows != 1) {
                    throw new CustomException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "RECEIPT_PARTICIPANT_UPDATE_FAILED",
                            "공동결제 참여자 수정에 실패했습니다."
                    );
                }

                keptParticipantIds.add(
                        existingParticipant.getId()
                );

                continue;
            }

            /*
             * 기존 참여자 수보다 요청 참여자 수가 많으면
             * 뒤에 추가된 참여자이므로 INSERT한다.
             */
            newParticipants.add(participant);
        }

        /*
         * 기존 참여자 중 현재 요청에서 빠진 참여자만
         * 논리 삭제한다.
         */
        receiptMapper.softDeleteMissingParticipants(
                receiptId,
                userId,
                tripId,
                keptParticipantIds
        );

        // 새로 추가된 참여자만 INSERT한다.
        insertReceiptParticipants(newParticipants);
    }

    // 요청 품목을 DB 저장 모델로 변환한다.
    private List<ReceiptItem> createReceiptItems(
            Long receiptId,
            List<ReceiptItemSaveRequest> itemRequests
    ) {
        if (itemRequests == null
                || itemRequests.isEmpty()) {

            return Collections.emptyList();
        }

        List<ReceiptItem> receiptItems =
                new ArrayList<>();

        for (int index = 0;
             index < itemRequests.size();
             index++) {

            ReceiptItemSaveRequest itemRequest =
                    itemRequests.get(index);

            if (itemRequest == null) {
                throw new CustomException(
                        HttpStatus.BAD_REQUEST,
                        "RECEIPT_ITEM_INVALID",
                        "영수증 품목 정보를 올바르게 입력해 주세요."
                );
            }

            ReceiptItem receiptItem =
                    new ReceiptItem();

            receiptItem.setReceiptId(receiptId);
            receiptItem.setOriginalName(
                    itemRequest.getOriginalName().trim()
            );
            receiptItem.setTranslatedName(
                    trimToNull(
                            itemRequest.getTranslatedName()
                    )
            );
            receiptItem.setQuantity(
                    itemRequest.getQuantity()
            );
            receiptItem.setAmount(
                    itemRequest.getAmount()
            );
            receiptItem.setDisplayOrder(
                    index + 1
            );

            receiptItems.add(receiptItem);
        }

        return receiptItems;
    }

    // 품목이 있을 때만 일괄 INSERT를 실행한다.
    private void insertReceiptItems(
            List<ReceiptItem> receiptItems
    ) {
        if (receiptItems.isEmpty()) {
            return;
        }

        int insertedRows =
                receiptMapper.insertReceiptItems(
                        receiptItems
                );

        if (insertedRows != receiptItems.size()) {
            throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "RECEIPT_ITEMS_SAVE_FAILED",
                    "영수증 품목 저장에 실패했습니다."
            );
        }
    }

    // 여행·국가·카테고리·통화 참조값을 검증한다.
    private void validateReferenceData(
            Long userId,
            Long tripId,
            ReceiptSaveRequest request
    ) {
        if (!receiptMapper.existsTripByIdAndUserId(
                tripId,
                userId
        )) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "RECEIPT_TRIP_INVALID",
                    "선택한 여행 정보를 확인해 주세요."
            );
        }

        if (!receiptMapper.existsTripCountryByUserId(
                tripId,
                request.getCountryId(),
                userId
        )) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "RECEIPT_TRIP_COUNTRY_INVALID",
                    "선택한 국가는 해당 여행에 포함된 국가가 아닙니다."
            );
        }

        if (!receiptMapper.existsCategoryById(
                request.getCategoryId()
        )) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "RECEIPT_CATEGORY_INVALID",
                    "선택한 지출 카테고리를 확인해 주세요."
            );
        }
    }

    // 영수증 목록 조회 Row를 응답 DTO로 변환한다.
    private ReceiptSummaryResponse createSummaryResponse(
            ReceiptSummaryRow row
    ) {
        return new ReceiptSummaryResponse(
                row.getId(),
                row.getTripId(),
                row.getCountryId(),
                row.getCountryName(),
                row.getCategoryId(),
                row.getCategoryName(),
                row.getMerchantOriginalName(),
                row.getMerchantTranslatedName(),
                row.getPaymentDateTime(),
                row.getCurrencyCode(),
                row.getCurrencySymbol(),
                row.getTotalAmount(),
                row.getSplitCount(),
                calculateSplitAmount(
                        row.getTotalAmount(),
                        row.getSplitCount()
                ),
                createImageUrl(
                        row.getTripId(),
                        row.getId(),
                        row.getFileUrl()
                )
        );
    }

    // 영수증 상세 조회 Row를 응답 DTO로 변환한다.
    private ReceiptDetailResponse createDetailResponse(
            ReceiptDetailRow row,
            List<ReceiptItemResponse> items,
            List<ReceiptParticipantResponse> participants
    ) {
        return new ReceiptDetailResponse(
                row.getId(),
                row.getTripId(),
                row.getCountryId(),
                row.getCountryName(),
                row.getCategoryId(),
                row.getCategoryName(),
                row.getCurrencyId(),
                row.getCurrencyCode(),
                row.getCurrencyName(),
                row.getCurrencySymbol(),
                row.getPaymentDateTime(),
                row.getFileName(),
                createImageUrl(
                        row.getTripId(),
                        row.getId(),
                        row.getFileUrl()
                ),
                row.getFileType(),
                row.getMemo(),
                row.getStatus(),
                row.getMerchantOriginalName(),
                row.getMerchantTranslatedName(),
                row.getTotalAmount(),
                row.getSplitCount(),
                calculateSplitAmount(
                        row.getTotalAmount(),
                        row.getSplitCount()
                ),
                row.getOcrRawText(),
                items,
                participants,
                row.getCreatedAt(),
                row.getUpdatedAt()
        );
    }

    // 품목 모델을 응답 DTO로 변환한다.
    private ReceiptItemResponse createItemResponse(
            ReceiptItem receiptItem
    ) {
        return new ReceiptItemResponse(
                receiptItem.getId(),
                receiptItem.getOriginalName(),
                receiptItem.getTranslatedName(),
                receiptItem.getQuantity(),
                receiptItem.getAmount(),
                receiptItem.getDisplayOrder()
        );
    }

    // 1인당 분할 금액을 계산한다.
    private BigDecimal calculateSplitAmount(
            BigDecimal totalAmount,
            Integer splitCount
    ) {
        if (totalAmount == null
                || splitCount == null
                || splitCount < 1) {

            return null;
        }

        return totalAmount.divide(
                BigDecimal.valueOf(splitCount),
                SPLIT_AMOUNT_SCALE,
                RoundingMode.HALF_UP
        );
    }

    // 영수증 존재 여부와 회원·여행 소유권을 함께 확인한다.
    private ReceiptDetailRow requireReceipt(
            Long userId,
            Long tripId,
            Long receiptId
    ) {
        ReceiptDetailRow receiptRow =
                receiptMapper.findDetailByIdAndUserIdAndTripId(
                                receiptId,
                                userId,
                                tripId
                );

        if (receiptRow == null) {
            throw new CustomException(
                    HttpStatus.NOT_FOUND,
                    "RECEIPT_NOT_FOUND",
                    "영수증을 찾을 수 없습니다."
            );
        }

        return receiptRow;
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new CustomException(
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_UNAUTHORIZED",
                    "로그인이 필요합니다."
            );
        }
    }

    private void validateReceiptId(Long receiptId) {
        if (receiptId == null || receiptId <= 0) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "RECEIPT_ID_INVALID",
                    "올바른 영수증 ID를 입력해 주세요."
            );
        }
    }

    private void validateRequest(
            ReceiptSaveRequest request
    ) {
        if (request == null) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "RECEIPT_REQUEST_REQUIRED",
                    "영수증 정보를 입력해 주세요."
            );
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();

        return trimmedValue.isEmpty()
                ? null
                : trimmedValue;
    }

    // DB 저장 실패 후 남은 이미지 파일을 정리한다.
    private void deleteStoredFileAfterFailure(
            String storedPath
    ) {
        try {
            receiptFileStorage.delete(storedPath);

        } catch (RuntimeException deleteException) {
            log.warn(
                    "영수증 DB 저장 실패 후 이미지 파일 정리 실패: {}",
                    storedPath,
                    deleteException
            );
        }
    }
    // 내부 파일 경로 대신 인증이 적용되는 이미지 조회 API 주소를 반환한다.
    private String createImageUrl(
            Long tripId,
            Long receiptId,
            String storedFileUrl
    ) {
        if (storedFileUrl == null
                || storedFileUrl.isBlank()) {
            return null;
        }

        return "/api/v1/trips/"
                + tripId
                + "/receipts/"
                + receiptId
                + "/image";
    }
    // 로그인 회원을 포함한 공동결제 인원수를 계산한다.
    private int calculateSplitCount(
            ReceiptSaveRequest request
    ) {
        if (request.getParticipantNames() == null) {
            return 1;
        }

        return request.getParticipantNames().size() + 1;
    }

    private StoredReceiptFile storeReceiptImageIfPresent(
            MultipartFile receiptImage
    ) {
        if (receiptImage == null || receiptImage.isEmpty()) {
            return null;
        }

        ValidatedReceiptImage validatedImage =
                receiptImageValidator.validateAndRead(
                        receiptImage
                );

        return receiptFileStorage.store(validatedImage);
    }

    private List<ReceiptParticipant>
    createReceiptParticipants(
            Long receiptId,
            List<String> participantNames
    ) {
        if (participantNames == null
                || participantNames.isEmpty()) {
            return Collections.emptyList();
        }

        List<ReceiptParticipant> participants =
                new ArrayList<>();

        for (int index = 0;
             index < participantNames.size();
             index++) {

            String participantName =
                    trimToNull(participantNames.get(index));

            if (participantName == null) {
                throw new CustomException(
                        HttpStatus.BAD_REQUEST,
                        "RECEIPT_PARTICIPANT_INVALID",
                        "공동결제 참여자 이름을 입력해 주세요."
                );
            }

            ReceiptParticipant participant =
                    new ReceiptParticipant();

            participant.setReceiptId(receiptId);
            participant.setParticipantName(
                    participantName
            );
            participant.setDisplayOrder(index + 1);

            participants.add(participant);
        }

        return participants;
    }

    private void insertReceiptParticipants(
            List<ReceiptParticipant> participants
    ) {
        if (participants.isEmpty()) {
            return;
        }

        int insertedRows =
                receiptMapper.insertReceiptParticipants(
                        participants
                );

        if (insertedRows != participants.size()) {
            throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "RECEIPT_PARTICIPANTS_SAVE_FAILED",
                    "공동결제 참여자 저장에 실패했습니다."
            );
        }
    }

    private ReceiptParticipantResponse
    createParticipantResponse(
            ReceiptParticipant participant
    ) {
        return new ReceiptParticipantResponse(
                participant.getId(),
                participant.getParticipantName(),
                participant.getDisplayOrder()
        );
    }

    private void validateTripId(Long tripId) {
        if (tripId == null || tripId <= 0) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "RECEIPT_TRIP_ID_INVALID",
                    "올바른 여행 ID를 입력해 주세요."
            );
        }
    }

    // 통화 코드를 표준화한 후 해당 통화 PK를 조회한다.
    private Long resolveCurrencyId(
            String requestedCurrencyCode
    ) {
        String currencyCode =
                requestedCurrencyCode
                        .trim()
                        .toUpperCase(Locale.ROOT);


        Long currencyId =
                receiptMapper.findCurrencyIdByCode(
                        currencyCode
                );

        if (currencyId == null) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "RECEIPT_CURRENCY_INVALID",
                    "결제 통화를 확인해 주세요."
            );
        }

        return currencyId;
    }
}
