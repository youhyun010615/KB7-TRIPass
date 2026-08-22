package com.tripass.travel.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CountryBudgetBaselineSeedTest {

    private static final List<String> ADDED_COUNTRIES = List.of(
            "아랍에미리트", "호주", "바레인", "브루나이", "캐나다", "덴마크", "영국",
            "인도네시아", "쿠웨이트", "말레이시아", "노르웨이", "뉴질랜드", "사우디아라비아",
            "스웨덴", "싱가포르", "태국", "미국", "이탈리아", "스페인", "네덜란드", "벨기에",
            "오스트리아", "포르투갈", "그리스", "아일랜드", "핀란드", "중국", "괌"
    );

    @Test
    void 추가_28개국을_국가명으로_조회해_재실행_가능하게_갱신한다() throws IOException {
        String sql = readSeed();

        ADDED_COUNTRIES.forEach(country ->
                assertTrue(sql.contains("'" + country + "'"), country + " 기준값이 없습니다."));
        assertTrue(sql.contains("b.country_name = c.country_name"));
        assertTrue(sql.contains("ON DUPLICATE KEY UPDATE"));
        assertFalse(sql.matches("(?s).*SELECT\\s+[0-9]+\\s*,\\s*[0-9]+.*"),
                "환경마다 달라지는 country_id를 직접 사용하면 안 됩니다.");
    }

    private String readSeed() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/sql/insert_country_budget_baselines.sql")) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
