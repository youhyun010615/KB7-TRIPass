package com.tripass.auth.mapper;

import com.tripass.auth.model.PhoneVerification;
import com.tripass.auth.model.VerificationPurpose;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

//phone_verifications(휴대폰 인증) 테이블 접근 Mapper
@Mapper
public interface PhoneVerificationMapper {
    //휴대폰 인증 요청 저장
    int insertPhoneVerification(PhoneVerification phoneVerification);

    //인증 요청 식별값으로 인증 정보 조회 - 인증번호 확인에 사용
    PhoneVerification findByRequestId(@Param("requestId")String requestId);

    //같은 전화번호와 인증 목적의 가장 최근 요청 시간 조회
    Date findLatestCreatedAt(@Param("phoneNumber") String phoneNumber, @Param("purpose") VerificationPurpose purpose);

    //인증번호 확인 실패 횟수 1 증가
    int incrementAttemptCount(@Param("requestId") String requestId);

    //인증 성공일시 기록
    int markVerified(@Param("requestId")String requestId);

    //인증 결과 사용일시 기록
    int markUsed(@Param("requestId")String requestId);




}
