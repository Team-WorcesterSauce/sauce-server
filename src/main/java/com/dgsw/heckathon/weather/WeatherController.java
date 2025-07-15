package com.dgsw.heckathon.weather;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@RestController
public class WeatherController {

    private final OpenWeatherApiService openWeatherApiService; // 서비스 주입 변경

    public WeatherController(OpenWeatherApiService openWeatherApiService) {
        this.openWeatherApiService = openWeatherApiService;
    }

    /* ---------- 실시간(현재) 날씨 ---------- */
    @GetMapping("/current")
    public ResponseEntity<CurrentWeatherResponse> getCurrentWeather(
            @RequestParam double lat,
            @RequestParam double lon) {

        CurrentWeatherResponse res = openWeatherApiService.getCurrentWeather(lat, lon);

        // OpenWeatherMap 응답 구조에 맞게 null 체크
        if (res != null && res.getMain() != null && res.getWeather() != null && !res.getWeather().isEmpty()) {
            return ResponseEntity.ok(res);
        }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
    }


    /* ---------- 시간별 예보 (OpenWeatherMap은 5일 3시간 단위 예보 제공) ---------- */
    @GetMapping("/weather") // 엔드포인트 이름 변경 (시간별 예보임을 명확히)
    public ResponseEntity<ForecastResponse> getHourlyForecast(
            @RequestParam double lat,
            @RequestParam double lon) {

        // OpenWeatherMap은 startTime, endTime, timesteps 개념 대신 5일 3시간 단위 예보를 제공
        ForecastResponse res = openWeatherApiService.getForecast(lat, lon);

        /* list가 있는지 확인 */
        if (res != null && res.getList() != null && !res.getList().isEmpty()) {
            return ResponseEntity.ok(res);
        }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
    }
}