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
        if (aiPrediction != null && !aiPrediction.isEmpty()) {
            response.put("prediction", aiPrediction);
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "AI 재난 예측 정보를 가져오는 데 실패했습니다.");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 현재 위치를 기준으로 주변 50도 범위 내에서 특정 날씨 이벤트(비, 눈, 우박, 흐림)가 발생하는 지점의 위도와 경도를 반환합니다.
     *
     * @param currentLat 현재 위치의 위도 (필수 파라미터)
     * @param currentLon 현재 위치의 경도 (필수 파라미터)
     * @param latStep 위도 간격 (기본값 2.0)
     * @param lonStep 경도 간격 (기본값 2.0)
     * @return 날씨 이벤트가 발생하는 지점의 위도, 경도 리스트
     */
    @GetMapping("/earth") // 이전 제안에 따라 엔드포인트 이름 변경 (earth에서 weather-events-around-me)
    public ResponseEntity<Map<String, Object>> getWeatherEventsAroundMe(
            @RequestParam double currentLat, // 현재 위도 (필수 파라미터)
            @RequestParam double currentLon, // 현재 경도 (필수 파라미터)
            @RequestParam(defaultValue = "5.0") double latStep, // 기본값 2.0도 간격으로 변경 (API 할당량 관리)
            @RequestParam(defaultValue = "5.0") double lonStep) { // 기본값 2.0도 간격으로 변경 (API 할당량 관리)

        // 50도 범위 계산 (각 방향으로 25도씩)
        double searchSpanDegrees = 25.0; // 50도 범위에 맞게 25.0으로 변경

        double minLat = currentLat - searchSpanDegrees;
        double maxLat = currentLat + searchSpanDegrees;
        double minLon = currentLon - searchSpanDegrees;
        double maxLon = currentLon + searchSpanDegrees;

        // 위도 범위 유효성 검사 및 클램핑 (-90 ~ 90)
        minLat = Math.max(minLat, -90.0);
        maxLat = Math.min(maxLat, 90.0);

        List<Map<String, Double>> eventLocations;

        // 경도 랩핑 처리 (경도 -180 ~ 180 경계를 넘어가는 경우)
        // 이 로직은 minLon이 maxLon보다 크면서 동시에 범위가 -180/180 경계를 넘어갈 때 적용
        // 예를 들어 minLon이 160, maxLon이 -170이 된 경우 (경도 160~180, -180~-170)
        // 경도 정규화 후 minLon이 maxLon보다 큰 경우 (경도 랩핑 발생)
        // 먼저 경도들을 -180 ~ 180 범위로 정규화합니다.
        double normalizedMinLon = (minLon + 180.0) % 360.0;
        if (normalizedMinLon < 0) normalizedMinLon += 360.0;
        normalizedMinLon -= 180.0;

        double normalizedMaxLon = (maxLon + 180.0) % 360.0;
        if (normalizedMaxLon < 0) normalizedMaxLon += 360.0;
        normalizedMaxLon -= 180.0;

        // 경도 범위가 -180/180 경계를 넘어서는 경우 (예: 170도에서 동쪽으로 25도 -> 170 ~ 180, 그리고 -180 ~ -165)
        if (normalizedMinLon > normalizedMaxLon) {
            // 예를 들어 minLon이 160, maxLon이 -170이 된 경우 (경도 160~180, -180~-170)
            List<Map<String, Double>> part1 = openAiNavigationService.findSpecificWeatherEventsLocations(
                    minLat, maxLat, normalizedMinLon, 180.0, latStep, lonStep);
            List<Map<String, Double>> part2 = openAiNavigationService.findSpecificWeatherEventsLocations(
                    minLat, maxLat, -180.0, normalizedMaxLon, latStep, lonStep);
            eventLocations = new ArrayList<>(part1);
            eventLocations.addAll(part2);
        } else {
            eventLocations = openAiNavigationService.findSpecificWeatherEventsLocations(
                    minLat, maxLat, normalizedMinLon, normalizedMaxLon, latStep, lonStep);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("eventLocations", eventLocations);
        response.put("message", "현재 위치 주변 50도 범위 내 특정 날씨 이벤트 발생 지점 조회 완료.");
        response.put("centerLat", currentLat);
        response.put("centerLon", currentLon);
        response.put("searchRadiusDegrees", searchSpanDegrees); // 실제 검색 반경
        response.put("disclaimer", "이 데이터는 Tomorrow.io API를 통해 격자별로 조회된 날씨를 기반으로 합니다. 격자 간격에 따라 정확도가 달라질 수 있으며, 무료 API 할당량으로 인해 검색 범위와 해상도에 제약이 있을 수 있습니다."); // 문구 수정

        return ResponseEntity.ok(response);
    }
}