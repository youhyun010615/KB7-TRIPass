package com.tripass.travel.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TripStatus {

    PLANNING("여행 준비 중"),
    TRAVELING("여행 중"),
    ENDED("여행 종료"),
    ARCHIVED("지난 여행으로 보관");

    private final String description;
}
