package com.tripass.profile.service;

import com.tripass.common.exception.CustomException;
import com.tripass.profile.dto.request.IncomeSourceBulkUpdateRequest;
import com.tripass.profile.dto.request.IncomeSourceCreateListRequest;
import com.tripass.profile.dto.request.IncomeSourceCreateRequest;
import com.tripass.profile.dto.request.IncomeSourceSaveRequest;
import com.tripass.profile.dto.response.IncomeSourceListResponse;
import com.tripass.profile.dto.response.IncomeSourceResponse;
import com.tripass.profile.mapper.IncomeSourceMapper;
import com.tripass.profile.model.IncomeSource;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


//금융 프로필 기능을 구현하는 서비스
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialProfileServiceImpl implements FinancialProfileService {
    private final IncomeSourceMapper incomeSourceMapper;

    //최초 금융 프로필 등록 시 여러 급여 정보를 일괄 등록
    //한건이라도 등록 실패 시 모든 등록 작업 롤백

    @Override
    @Transactional
    public IncomeSourceListResponse createIncomeSources(Long userId, IncomeSourceCreateListRequest request) {
        validateAuthenticatedUser(userId);
        if (request == null
                || request.getIncomeSources() == null
                || request.getIncomeSources().isEmpty()) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "PROFILE_INCOME_SOURCE_REQUIRED",
                    "급여 정보는 최소 1개 이상 입력해 주세요."
            );
        }

        //최초 등록 API 중복 호출로 같은 급여가 다시 저장되는 것을 방지한다.
        //이후 금융 프로필 수정에서 추가 등록하는 급여는
        //수정 API에서 처리한다.
        List<IncomeSource> existingIncomeSources =
                incomeSourceMapper.findAllByUserId(userId);
        if (existingIncomeSources != null
                && !existingIncomeSources.isEmpty()) {
            throw new CustomException(
                    HttpStatus.CONFLICT,
                    "PROFILE_INCOME_SOURCE_ALREADY_EXISTS",
                    "이미 등록된 급여 정보가 있습니다."
            );
        }
        //Insert 시작 전 모든 계좌를 검증한다.
        for (IncomeSourceCreateRequest item : request.getIncomeSources()) {
            if (item == null) {
                throw new CustomException(
                        HttpStatus.BAD_REQUEST,
                        "PROFILE_INVALID_INPUT",
                        "급여 정보를 올바르게 입력해 주세요"
                );
            }
            validateActiveAccount(
                    userId,
                    item.getAccountId()
            );
        }
        //검증 완료된 급여 정보를 순서대로 등록한다.
        for (IncomeSourceCreateRequest item : request.getIncomeSources()) {
            IncomeSource incomeSource =
                    createIncomeSourceModel(
                            userId,
                            item
                    );
            int insertRows =
                    incomeSourceMapper.insertIncomeSource(
                            incomeSource
                    );
            if (insertRows != 1) {
                throw new CustomException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "PROFILE_INCOME_SOURCE_CREATE_FAILED",
                        "급여 정보 등록에 실패했습니다."
                );
            }
        }
        return buildIncomeSourceListResponse(userId);
    }

    //로그인 회원에게 등록된 모든 급여와 합계를 조회한다.
    @Override
    public IncomeSourceListResponse getIncomeSources(Long userId) {
        validateAuthenticatedUser(userId);
        return buildIncomeSourceListResponse(userId);
    }

    // 로그인 회원의 급여 정보를 단건 조회한다.
    @Override
    public IncomeSourceResponse getIncomeSource(
            Long userId,
            Long incomeSourceId
    ) {
        validateAuthenticatedUser(userId);

        IncomeSource incomeSource =
                findRequiredIncomeSource(
                        userId,
                        incomeSourceId
                );

        return toIncomeSourceResponse(incomeSource);
    }

    //수정 화면에서 기존급여 삭제, 신규 급여 등록, 기존 급여 수정
    //세 작업을 한번에 처리한다.
    //한 작업이라도 실패하면 전체 작업을 롤백한다.


    @Override
    @Transactional
    public IncomeSourceListResponse updateIncomeSources(Long userId, IncomeSourceBulkUpdateRequest request) {
        validateAuthenticatedUser(userId);
        if (request == null
                || request.getIncomeSources() == null
                || request.getIncomeSources().isEmpty()) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "PROFILE_INCOME_SOURCE_REQUIRED",
                    "급여 정보는 최소 1개 이상 등록해야 합니다."
            );
        }
        List<Long> deleteIds =
                request.getDeletedIncomeSourceIds() == null
                        ? Collections.emptyList() : request.getDeletedIncomeSourceIds();

        //수정 및 삭제 대상 검증을 먼저 수행한다.
        //검증이 끝날때까지 INSERT, UPDATE, DELETE를 실행하지 않는다
        validateBulkUpdateRequest(
                userId,
                request,
                deleteIds
        );

        //기존 급여 정보를 논리 삭제한다.
        for (Long deletedId : deleteIds) {
            int deletedRows =
                    incomeSourceMapper.softDeleteIncomeSource(
                            deletedId,
                            userId
                    );
            if (deletedRows != 1) {
                throw new CustomException(
                        HttpStatus.NOT_FOUND,
                        "PROFILE_INCOME_SOURCE_NOT_FOUND",
                        "삭제할 급여 정보를 찾을 수 없습니다."
                );
            }
        }
        //화면에 남아 있는 급여 항목을 저장
        //id가 null이면 신규 등록하고
        //id가 있으면 기존 급여를 수정한다.
        for (IncomeSourceSaveRequest item : request.getIncomeSources()) {
            if (item.getId() == null) {
                insertIncomeSource(
                        userId,
                        item
                );
            } else {
                updateIncomeSource(
                        userId,
                        item
                );
            }
        }
        return buildIncomeSourceListResponse(userId);
    }

    //일괄 수정 요청을 실제 DB에 반영하기 전에 검증한다.
    private void validateBulkUpdateRequest(
            Long userId,
            IncomeSourceBulkUpdateRequest request,
            List<Long> deletedIds) {
        Set<Long> saveIds = new HashSet<>();
        Set<Long> deleteIds = new HashSet<>();

        //삭제 대상의 중복 여부와 소유권을 검사한다.
        for (Long deletedId : deletedIds) {
            if (deletedId == null) {
                throw new CustomException(
                        HttpStatus.BAD_REQUEST,
                        "PROFILE_INVALID_INPUT",
                        "삭제할 급여 정보 ID가 올바르지 않습니다."
                );
            }
            if (!deleteIds.add(deletedId)) {
                throw new CustomException(
                        HttpStatus.BAD_REQUEST,
                        "PROFILE_DUPLICATED_INCOME_SOURCE_ID",
                        "삭제할 급여 정보 ID가 중복되었습니다."
                );
            }
            findRequiredIncomeSource(
                    userId,
                    deletedId
            );
        }
        //저장 대상의 중복 여부와 소유권 및 정상 계좌인지 검사
        for (IncomeSourceSaveRequest item : request.getIncomeSources()) {
            if (item == null) {
                throw new CustomException(
                        HttpStatus.BAD_REQUEST,
                        "PROFILE_INVALID_INPUT",
                        "급여 정보를 올바르게 입력해 주세요."
                );
            }
            validateActiveAccount(
                    userId,
                    item.getAccountId()
            );
            //id가 null인 항목은 산규 등록이므로 기존 데이터 검사가 필요 없음
            if (item.getId() == null) {
                continue;
            }
            if (!saveIds.add(item.getId())) {
                throw new CustomException(
                        HttpStatus.BAD_REQUEST,
                        "PROFILE_DUPLICATED_INCOME_SOURCE_ID",
                        "저장할 급여 정보 ID가 중복되었습니다."
                );
            }
            //동일한 급여 ID가 수정 목록과 삭제 목록에 동시에 포함되는 것을 방지한다.
            if (deleteIds.contains(item.getId())) {
                throw new CustomException(
                        HttpStatus.BAD_REQUEST,
                        "PROFILE_INCOME_SOURCE_REQUEST_CONFLICT",
                        "동일한 급여 정보에 대해 수정과 삭제를 동시에 할 수 없습니다."
                );
            }
            findRequiredIncomeSource(userId, item.getId());
        }
    }

    //수정 화면에서 새로 추가된 급여를 등록한다.
    private void insertIncomeSource(Long userId, IncomeSourceSaveRequest request) {
        IncomeSource incomeSource = createIncomeSourceModel(userId, request);
        int insertedRows =
                incomeSourceMapper.insertIncomeSource(
                        incomeSource
                );
        if (insertedRows != 1) {
            throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "PROFILE_INCOME_SOURCE_CREATE_FAILED",
                    "급여 정보 등록에 실패했습니다."
            );
        }
    }

    //수정 화면에서 기존 급여 정보를 수정한다.
    private void updateIncomeSource(Long userId, IncomeSourceSaveRequest request) {
        IncomeSource incomeSource = new IncomeSource();
        incomeSource.setId(request.getId());
        incomeSource.setUserId(userId);
        incomeSource.setAccountId(
                request.getAccountId()
        );
        incomeSource.setPaymentName(
                normalizeRequiredText(
                        request.getPaymentName(),
                        "급여명을 입력해 주세요."
                )
        );
        incomeSource.setPaymentDay(
                request.getPaymentDay()
        );
        incomeSource.setGrossAmount(
                request.getGrossAmount()
        );
        incomeSource.setMemo(
                normalizeOptionalText(
                        request.getMemo()
                )
        );
        int updatedRows =
                incomeSourceMapper.updateIncomeSource(
                        incomeSource
                );

        if (updatedRows != 1) {
            throw new CustomException(
                    HttpStatus.NOT_FOUND,
                    "PROFILE_INCOME_SOURCE_NOT_FOUND",
                    "수정할 급여 정보를 찾을 수 없습니다."
            );
        }
    }

    //최초 등록 요청을 IncomeSource 모델로 변환한다
    private IncomeSource createIncomeSourceModel(Long userId, IncomeSourceCreateRequest request) {
        IncomeSource incomeSource = new IncomeSource();
        incomeSource.setUserId(userId);
        incomeSource.setAccountId(
                request.getAccountId()
        );
        incomeSource.setPaymentName(
                normalizeRequiredText(
                        request.getPaymentName(),
                        "급여명을 입력해 주세요."
                )
        );
        incomeSource.setPaymentDay(
                request.getPaymentDay()
        );
        incomeSource.setGrossAmount(
                request.getGrossAmount()
        );
        incomeSource.setMemo(
                normalizeOptionalText(
                        request.getMemo()
                )
        );

        return incomeSource;
    }

    //수정 화면의 신규 항목을 IncomeSource 모델로 변환한다.
    private IncomeSource createIncomeSourceModel(
            Long userId,
            IncomeSourceSaveRequest request
    ) {
        IncomeSource incomeSource = new IncomeSource();

        incomeSource.setUserId(userId);
        incomeSource.setAccountId(
                request.getAccountId()
        );
        incomeSource.setPaymentName(
                normalizeRequiredText(
                        request.getPaymentName(),
                        "급여명을 입력해 주세요."
                )
        );
        incomeSource.setPaymentDay(
                request.getPaymentDay()
        );
        incomeSource.setGrossAmount(
                request.getGrossAmount()
        );
        incomeSource.setMemo(
                normalizeOptionalText(
                        request.getMemo()
                )
        );

        return incomeSource;
    }

    // 등록된 전체 급여 목록과 합계 응답을 만든다.
    private IncomeSourceListResponse
    buildIncomeSourceListResponse(Long userId) {
        List<IncomeSource> incomeSources =
                incomeSourceMapper.findAllByUserId(
                        userId
                );

        List<IncomeSourceResponse> responses =
                incomeSources == null
                        ? Collections.emptyList()
                        : incomeSources.stream()
                        .map(this::toIncomeSourceResponse)
                        .collect(Collectors.toList());

        BigDecimal totalMonthlyIncome =
                incomeSourceMapper
                        .sumGrossAmountByUserId(
                                userId
                        );

        if (totalMonthlyIncome == null) {
            totalMonthlyIncome = BigDecimal.ZERO;
        }

        return new IncomeSourceListResponse(
                totalMonthlyIncome,
                responses
        );
    }

    //급여 ID와 회원 ID를 함께 사용해서 조회한다.
    //다른 회원의 급여 ID를 전달하면 조회되지 않는다.
    private IncomeSource findRequiredIncomeSource(
            Long userId,
            Long incomeSourceId
    ) {
        if (incomeSourceId == null) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "PROFILE_INVALID_INPUT",
                    "급여 정보 ID를 입력해 주세요."
            );
        }

        IncomeSource incomeSource =
                incomeSourceMapper
                        .findByIdAndUserId(
                                incomeSourceId,
                                userId
                        );

        if (incomeSource == null) {
            throw new CustomException(
                    HttpStatus.NOT_FOUND,
                    "PROFILE_INCOME_SOURCE_NOT_FOUND",
                    "급여 정보를 찾을 수 없습니다."
            );
        }

        return incomeSource;
    }

    //선택한 계좌가 로그인 회원 소유리며
    //활성화된 계좌인지 확인한다.
    private void validateActiveAccount(
            Long userId,
            Long accountId
    ) {
        if (accountId == null) {
            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "PROFILE_ACCOUNT_REQUIRED",
                    "급여 입금 계좌를 선택해 주세요."
            );
        }
        int accountCount = incomeSourceMapper
                .countActiveAccountByIdAndUserId(accountId, userId);
        if (accountCount != 1) {
            throw new CustomException(
                    HttpStatus.NOT_FOUND,
                    "PROFILE_ACCOUNT_NOT_FOUND",
                    "사용할 수 있는 입금 계좌를 찾을 수 없습니다."
            );
        }
    }

    //Spring Security 인증 정보에서 회원 ID가 정상적으로 전달됐는지 확인한다.
    private void validateAuthenticatedUser(
            Long userId
    ) {
        if (userId == null) {
            throw new CustomException(
                    HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "로그인이 필요합니다."
            );
        }
    }

    // 필수 문자열의 앞뒤 공백을 제거하고 빈 문자열을 검사한다.
    private String normalizeRequiredText(
            String value,
            String errorMessage
    ) {
        if (value == null
                || value.trim().isEmpty()) {

            throw new CustomException(
                    HttpStatus.BAD_REQUEST,
                    "PROFILE_INVALID_INPUT",
                    errorMessage
            );
        }

        return value.trim();
    }

    // 선택 입력 문자열은 앞뒤 공백을 제거하고 빈 값이면 null로 변환한다.
    private String normalizeOptionalText(
            String value
    ) {
        if (value == null
                || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    // IncomeSource 모델을 외부 응답 DTO로 변환한다.
    private IncomeSourceResponse toIncomeSourceResponse(
            IncomeSource incomeSource
    ) {
        return new IncomeSourceResponse(
                incomeSource.getId(),
                incomeSource.getAccountId(),
                incomeSource.getAccountName(),
                incomeSource.getAccountNumber(),
                incomeSource.getPaymentName(),
                incomeSource.getPaymentDay(),
                incomeSource.getGrossAmount(),
                incomeSource.getMemo()
        );
    }
}
