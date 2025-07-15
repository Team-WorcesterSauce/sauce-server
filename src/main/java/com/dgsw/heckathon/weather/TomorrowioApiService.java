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
public class TomorrowioApiService {

    private static final Logger logger = LoggerFactory.getLogger(TomorrowioApiService.class);

    @Value("${tomorrowio.api.key}")
    private String apiKey;

    @Value("${tomorrowio.api.base-url}")
    private String baseUrl;  // 예: "https://api.tomorrow.io/v4/weather"

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private static final String COMMON_FIELDS = String.join(",",
            "temperature", "humidity", "precipitationIntensity", "precipitationType",
            "windSpeed", "windDirection", "cloudCover", "weatherCode", "temperatureApparent"
    );

    public TomorrowioApiService(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder().build();
        this.objectMapper = objectMapper;
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public CurrentWeatherResponse getCurrentWeather(double lat, double lon) {
        String location = lat + "," + lon;

        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/realtime")
                .queryParam("location", location)
                .queryParam("fields", COMMON_FIELDS)
                .queryParam("units", "metric")
                .queryParam("apikey", apiKey)
                .build()
                .toUri();

        logger.info("Realtime API 호출 URI: {}", uri);

        return executeCurrentWeatherApiCall(uri);
    }

    public ForecastResponse getForecast(double lat, double lon, String timesteps, String startTime, String endTime) {
        String location = lat + "," + lon;

        // startTime, endTime은 ISO 8601 UTC (Z포함) 형식인지 반드시 확인
        // 예: 2025-07-15T13:00:00Z

        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/forecast")
                .queryParam("location", location)
                .queryParam("fields", COMMON_FIELDS)
                .queryParam("units", "metric")
                .queryParam("timesteps", timesteps)
                .queryParam("startTime", startTime)
                .queryParam("endTime", endTime)
                .queryParam("apikey", apiKey)
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