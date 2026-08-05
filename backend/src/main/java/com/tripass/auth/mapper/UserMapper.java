package com.tripass.auth.mapper;

import com.tripass.auth.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

//users 테이블에 접근하는 Mapper
@Mapper
public interface UserMapper {
    //일반 로그인 아이디 중복 조회 - 일치하는 회원 수 조회
    int countLocalUserByLoginId(@Param("loginId") String loginId);
    //로그인 아이디로 회원 조회 - 로그인 시 사용(회원이면 회원정보, 없으면 null반환)
    User findLocalUserByLoginId(@Param("loginId") String loginId);
    //일반 회원을 users 테이블에 저장한다. - 저장된 행 갯수 리턴
    int insertLocalUser(User user);
    // 회원 PK로 활성 회원 조회
    User findActiveUserById(@Param("userId") Long userId);
}
