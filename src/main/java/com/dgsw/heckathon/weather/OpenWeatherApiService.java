package com.dgsw.heckathon.weather;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class OpenWeatherApiService {

    private static final Logger logger = LoggerFactory.getLogger(OpenWeatherApiService.class);

    @Value("${openweathermap.api.key}")
    private String apiKey;

    @Value("${openweathermap.api.base-url}")
    private String baseUrl;  // 예: "https://api.openweathermap.org/data/2.5"

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenWeatherApiService(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder().build();
        this.objectMapper = objectMapper;
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public CurrentWeatherResponse getCurrentWeather(double lat, double lon) {
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/weather")
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("units", "metric") // 섭씨 온도를 위해 metric 사용
                .queryParam("appid", apiKey)
                .build()
                .toUri();

        logger.info("Current Weather API 호출 URI: {}", uri);

        return executeCurrentWeatherApiCall(uri);
    }

    public ForecastResponse getForecast(double lat, double lon) {
        // OpenWeatherMap의 기본 예보는 5일치 3시간 단위이므로, 별도의 timesteps, startTime, endTime 필요 없음
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/forecast")
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("units", "metric") // 섭씨 온도를 위해 metric 사용
                .queryParam("appid", apiKey)
                .build()
                .toUri();

        logger.info("Forecast API 호출 URI: {}", uri);

        return executeForecastApiCall(uri);
    }

    private CurrentWeatherResponse executeCurrentWeatherApiCall(URI uri) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            logger.info("현재 날씨 API 상태 코드: {}", response.statusCode());
            logger.debug("현재 날씨 API 응답 본문: {}", response.body());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), CurrentWeatherResponse.class);
            } else {
                logger.error("현재 날씨 API 호출 실패. 상태 코드: {}, 응답: {}", response.statusCode(), response.body());
                return null;
            }
        } catch (IOException | InterruptedException e) {
            logger.error("현재 날씨 API 호출 중 오류 발생: ", e);
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private ForecastResponse executeForecastApiCall(URI uri) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            logger.info("예보 API 상태 코드: {}", response.statusCode());
            logger.debug("예보 API 응답 본문: {}", response.body());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), ForecastResponse.class);
            } else {
                logger.error("예보 API 호출 실패. 상태 코드: {}, 응답: {}", response.statusCode(), response.body());
                return null;
            }
        } catch (IOException | InterruptedException e) {
            logger.error("예보 API 호출 중 오류 발생: ", e);
            Thread.currentThread().interrupt();
            return null;
        }
    }
}