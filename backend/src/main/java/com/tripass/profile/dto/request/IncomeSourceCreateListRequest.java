package com.tripass.profile.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

//최초 금융 프로필 등록 시 여러 급여 정보를 일괄 등록하는 요청 DTO
@Getter
@NoArgsConstructor
public class IncomeSourceCreateListRequest {
    //최초 등록할 급여 정보 목록
    @Valid
    @NotEmpty(message = "등록할 급여 정보를 입력해 주세요.")
    private List<IncomeSourceCreateRequest> incomeSources = new ArrayList<>();
}
