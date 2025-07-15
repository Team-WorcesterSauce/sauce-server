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

    private final TomorrowioApiService tomorrowioApiService;

    public WeatherController(TomorrowioApiService tomorrowioApiService) {
        this.tomorrowioApiService = tomorrowioApiService;
    }

    /* ---------- 실시간(현재) 날씨 ---------- */
    @GetMapping("/current")
    public ResponseEntity<CurrentWeatherResponse> getCurrentWeather(
            @RequestParam double lat,
            @RequestParam double lon) {

        CurrentWeatherResponse res = tomorrowioApiService.getCurrentWeather(lat, lon);

        // values가 있는지 확인
        if (res != null &&
                res.getData() != null &&
                res.getData().getValues() != null) {

            return ResponseEntity.ok(res);
        }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
    }


    /* ---------- 시간별 예보 ---------- */
    @GetMapping("/weather")
    public ResponseEntity<ForecastResponse> getHourlyForecast(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "24") int hours) {

        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
        String startTime = nowUtc.format(DateTimeFormatter.ISO_INSTANT);
        String endTime   = nowUtc.plusHours(hours).format(DateTimeFormatter.ISO_INSTANT);

        ForecastResponse res =
                tomorrowioApiService.getForecast(lat, lon, "1h", startTime, endTime);

        /* timelines → hourly 리스트 존재 여부 확인 (data 랩퍼 없음) */
        if (res != null &&
                res.getTimelines() != null &&
                res.getTimelines().getHourly() != null &&
                !res.getTimelines().getHourly().isEmpty()) {

            return ResponseEntity.ok(res);
        }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
    }
}