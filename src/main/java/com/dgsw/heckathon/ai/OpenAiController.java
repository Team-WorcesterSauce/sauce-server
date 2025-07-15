package com.dgsw.heckathon.ai;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
public class OpenAiController {

    private final OpenAiDisasterService openAiDisasterService;
    private final OpenAiNavigationService openAiNavigationService;

    public OpenAiController(OpenAiDisasterService openAiDisasterService, OpenAiNavigationService openAiNavigationService) {
        this.openAiDisasterService = openAiDisasterService;
        this.openAiNavigationService = openAiNavigationService;
    }

    /**
     * 특정 위도, 경도 및 시간 범위를 기반으로 AI 재난 예측을 가져옵니다.
     * @param lat 위도
     * @param lon 경도
     * @param hours 예측할 시간 범위 (기본값 12시간)
     * @return AI가 생성한 재난 예측 텍스트
     */
    @GetMapping("/disaster")
    public ResponseEntity<Map<String, String>> getDisasterPrediction(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "12") int hours) { // 기본값 12시간

        String aiPrediction = openAiDisasterService.predictDisasterBasedOnWeather(lat, lon, hours);

        Map<String, String> response = new HashMap<>();
        response.put("prediction", aiPrediction);
        response.put("message", "AI 기반 재난 예측 조회 완료.");
        return ResponseEntity.ok(response);
    }

    /**
     * 현재 위치 주변 특정 반경 내에서 특정 날씨 이벤트(비, 눈, 우박, 흐림) 발생 지점 스캔
     * @param currentLat 현재 위도
     * @param currentLon 현재 경도
     * @param searchRadiusDegrees 검색 반경 (도 단위)
     * @param latStep 위도 간격
     * @param lonStep 경도 간격
     * @return 특정 날씨 이벤트가 감지된 위치 목록
     */
    @GetMapping("/earth")
    public ResponseEntity<Map<String, Object>> scanSpecificWeatherEvents(
            @RequestParam("lat") double currentLat,
            @RequestParam("lon") double currentLon,
            @RequestParam(defaultValue = "100") double searchRadiusDegrees, // 기본 검색 반경 50도
            @RequestParam(defaultValue = "10") double latStep,             // 격자 탐색 간격 (위도)
            @RequestParam(defaultValue = "10") double lonStep) {            // 격자 탐색 간격 (경도)

        // 검색 반경에 따른 위도/경도 범위 계산
        double minLat = currentLat - searchRadiusDegrees;
        double maxLat = currentLat + searchRadiusDegrees;
        double minLon = currentLon - searchRadiusDegrees;
        double maxLon = currentLon + searchRadiusDegrees;

        // 위도/경도 경계 보정
        minLat = Math.max(minLat, -90.0);
        maxLat = Math.min(maxLat, 90.0);

        // 경도 범위 정규화 (-180 ~ 180)
        double normalizedMinLon = (minLon + 180.0) % 360.0;
        if (normalizedMinLon < 0) normalizedMinLon += 360.0;
        normalizedMinLon -= 180.0;

        double normalizedMaxLon = (maxLon + 180.0) % 360.0;
        if (normalizedMaxLon < 0) normalizedMaxLon += 360.0;
        normalizedMaxLon -= 180.0;

        List<Map<String, Object>> eventLocations;

        // 경도 범위가 -180/180 경계를 넘어서는 경우
        if (normalizedMinLon > normalizedMaxLon) {
            List<Map<String, Object>> part1 = openAiNavigationService.findSpecificWeatherEventsLocations(
                    minLat, maxLat, normalizedMinLon, 180.0, latStep, lonStep);
            List<Map<String, Object>> part2 = openAiNavigationService.findSpecificWeatherEventsLocations(
                    minLat, maxLat, -180.0, normalizedMaxLon, latStep, lonStep);
            eventLocations = new ArrayList<>(part1);
            eventLocations.addAll(part2);
        } else {
            eventLocations = openAiNavigationService.findSpecificWeatherEventsLocations(
                    minLat, maxLat, normalizedMinLon, normalizedMaxLon, latStep, lonStep);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("eventLocations", eventLocations);
        response.put("message", "현재 위치 주변 " + searchRadiusDegrees + "도 범위 내 특정 날씨 이벤트 발생 지점 조회 완료.");
        response.put("centerLat", currentLat);
        response.put("centerLon", currentLon);
        response.put("searchRadiusDegrees", searchRadiusDegrees); // 실제 검색 반경
        response.put("disclaimer", "이 데이터는 OpenWeatherMap API를 통해 격자별로 조회된 날씨 데이터를 기반으로 합니다.");
        return ResponseEntity.ok(response);
    }
}