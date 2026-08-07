package com.tripass.profile.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

//급여 정보의 추가, 수정, 삭제를 한 번에 처리하는 요청 DTO
@Getter
@NoArgsConstructor
public class IncomeSourceBulkUpdateRequest {

    //저장 후 유지할 급여 정보 목록
    //항목의 id가 null이면 신규 등록
    //id 가 있으면 기존 급여 정보 수정한다.
    @Valid
    @NotEmpty(message = "급여 정보는 최소 1개 이상 등록해야 합니다.")
    private List<IncomeSourceSaveRequest> incomeSources = new ArrayList<>();

    //삭제할 기존 급여 정보의 PK 목록
    //DB에 아직 저장되지 않은 항목은 프론트에서만 제거하고 이 목록에 포함되지 않는다
    private List<Long> deletedIncomeSourceIds = new ArrayList<>();

}
