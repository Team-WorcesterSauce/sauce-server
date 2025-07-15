package com.dgsw.heckathon.weather;

import com.dgsw.heckathon.ai.OpenAiApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class WeatherController {

    private final WeatherbitApiService weatherbitApiService;
    // private final OpenAiApiService openAiApiService; // OpenAI 서비스 주입 제거

    public WeatherController(WeatherbitApiService weatherbitApiService) {
        this.weatherbitApiService = weatherbitApiService;
        // this.openAiApiService = openAiApiService; // 생성자에서도 제거
    }

    @GetMapping("/current")
    public ResponseEntity<WeatherbitResponse> getCurrentWeather(
            @RequestParam double lat,
            @RequestParam double lon) {

        WeatherbitResponse response = weatherbitApiService.getCurrentWeather(lat, lon);

        if (response != null && response.getData() != null && !response.getData().isEmpty()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/weather")
    public ResponseEntity<WeatherbitResponse> getHourlyForecast(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "48") int hours) {

        WeatherbitResponse response = weatherbitApiService.getHourlyForecast(lat, lon, hours);

        if (response != null && response.getData() != null && !response.getData().isEmpty()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}