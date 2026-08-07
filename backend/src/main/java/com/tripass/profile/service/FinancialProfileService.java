package com.tripass.profile.service;

import com.tripass.profile.dto.request.IncomeSourceBulkUpdateRequest;
import com.tripass.profile.dto.request.IncomeSourceCreateListRequest;
import com.tripass.profile.dto.response.IncomeSourceListResponse;
import com.tripass.profile.dto.response.IncomeSourceResponse;


//금융 프로필 기능을 정의하는 서비스 인테페이스
public interface FinancialProfileService {

    // 최초 금융 프로필 등록 시 여러 급여를 일괄 등록
    IncomeSourceListResponse createIncomeSources(
            Long userId,
            IncomeSourceCreateListRequest request
    );

    // 로그인 회원의 등록된 모든 급여 조회
    IncomeSourceListResponse getIncomeSources(Long userId);


    //로그인 회원의 급여 단건 조회
    IncomeSourceResponse getIncomeSource(
            Long userId,
            Long incomeSourceId
    );

    // 수정 화면의 추가·수정·삭제 일괄 처리
    IncomeSourceListResponse updateIncomeSources(
            Long userId,
            IncomeSourceBulkUpdateRequest request
    );

}
