package com.tripass.auth.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

//users 테이블의 회원 정보를 표현하는 도메인 모델
//회원가입 시 Insert 파라미터로도 사용
@Getter
@Setter
@NoArgsConstructor
public class User {
    //회원 PK
    private Long id;
    //로그인 아이디
    private String loginId;
    //비밀번호
    private String password;
    //회원 이름
    private String name;
    //휴대전화번호
    private String phoneNumber;
    //로그인 방식
    private String loginProvider;
    //소셜 로그인 유저의 사용자 고유 식별값
    private String providerKey;
    //현재 서비스 화면 모드
    private String currentViewMode;
    //탈퇴 여부
    private boolean deleted;
    //탈퇴일
    private Date deletedAt;
    //생성일
    private Date createdAt;
    //수정일
    private Date updatedAt;

}
