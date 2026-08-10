package com.tripass.financial.controller;

import com.tripass.common.response.ApiResponse;
import com.tripass.financial.dto.TravelCardDetailResponseDto;
import com.tripass.financial.dto.TravelCardListResponseDto;
import com.tripass.financial.service.TravelCardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "GDS 트래블카드")
@RestController
@RequestMapping("/api/v1/products/travel-cards")
@RequiredArgsConstructor
public class TravelCardController {

    private final TravelCardService travelCardService;

    @ApiOperation(
            value = "트래블카드 목록 조회",
            notes = "활성화된 트래블카드 목록을 조회합니다. "
                    + "카드명·카드사·은행명 검색과 지원 통화 및 카드 기능 필터를 지원합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<TravelCardListResponseDto>>> getTravelCards(
            @ApiParam(
                    value = "카드명·카드사·은행명 검색어",
                    example = "하나"
            )
            @RequestParam(required = false)
            String keyword,

            @ApiParam(
                    value = "지원 통화 코드(ISO 4217)",
                    example = "EUR"
            )
            @RequestParam(required = false)
            String currencyCode,

            @ApiParam(
                    value = "별도 계좌 개설 없이 즉시 사용 가능 여부",
                    example = "true"
            )
            @RequestParam(required = false)
            Boolean instantUse,

            @ApiParam(
                    value = "교통카드 지원 여부",
                    example = "true"
            )
            @RequestParam(required = false)
            Boolean transitCard
    ) {
        List<TravelCardListResponseDto> data =
                travelCardService.getTravelCards(
                        keyword,
                        currencyCode,
                        instantUse,
                        transitCard
                );

        return ResponseEntity.ok(
                ApiResponse.success("트래블카드 목록 조회 성공", data)
        );
    }

    @ApiOperation(
            value = "트래블카드 상세 조회",
            notes = "카드 ID로 트래블카드 상세 정보와 지원 통화 목록을 조회합니다."
    )
    @GetMapping("/{cardId}")
    public ResponseEntity<ApiResponse<TravelCardDetailResponseDto>> getTravelCard(
            @ApiParam(
                    value = "트래블카드 ID",
                    required = true,
                    example = "1"
            )
            @PathVariable
            Long cardId
    ) {
        TravelCardDetailResponseDto data =
                travelCardService.getTravelCard(cardId);

        return ResponseEntity.ok(
                ApiResponse.success("트래블카드 상세 조회 성공", data)
        );
    }
}