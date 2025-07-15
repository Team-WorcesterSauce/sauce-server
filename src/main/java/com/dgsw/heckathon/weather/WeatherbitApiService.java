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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class WeatherbitApiService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherbitApiService.class);

    @Value("${weatherbit.api.key}")
    private String apiKey;

    @Value("${weatherbit.api.base-url}")
    private String baseUrl;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public WeatherbitApiService(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder().build();
        this.objectMapper = objectMapper;
        // Weatherbit API 응답은 비교적 표준적이지만, 혹시 모를 경우를 대비하여 설정 유지
        this.objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    }

    /**
     * 현재 날씨 데이터를 가져옵니다.
     * @param lat 위도
     * @param lon 경도
     * @return WeatherbitResponse 객체
     */
    public WeatherbitResponse getCurrentWeather(double lat, double lon) {
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/current")
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("key", apiKey)
                .build()
                .toUri();
        return executeApiCall(uri);
    }

    /**
     * 시간별 예보 데이터를 가져옵니다 (최대 48시간, 무료 플랜).
     * @param lat 위도
     * @param lon 경도
     * @param hours 예보를 가져올 시간 (최대 48)
     * @return WeatherbitResponse 객체
     */
    public WeatherbitResponse getHourlyForecast(double lat, double lon, int hours) {
        if (hours > 48) {
            hours = 48; // 무료 플랜 제한
        }
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/forecast/hourly")
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("key", apiKey)
                .queryParam("hours", hours) // 요청 시간 수
                .build()
                .toUri();
        return executeApiCall(uri);
    }

    /**
     * 시간별 과거 데이터를 가져옵니다 (최근 24시간, 무료 플랜).
     * Weatherbit.io는 'start_date'와 'end_date' 파라미터를 YYYY-MM-DD 형식으로 받습니다.
     * 무료 플랜은 현재 날짜 기준 과거 24시간 이내의 데이터만 제공합니다.
     * @param lat 위도
     * @param lon 경도
     * @param startDate 조회 시작 날짜 (YYYY-MM-DD)
     * @param endDate 조회 종료 날짜 (YYYY-MM-DD) - 보통 startDate와 같거나 다음날
     * @return WeatherbitResponse 객체
     */
    public WeatherbitResponse getHourlyHistory(double lat, double lon, LocalDate startDate, LocalDate endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE; // YYYY-MM-DD
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/history/hourly")
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("key", apiKey)
                .queryParam("start_date", startDate.format(formatter))
                .queryParam("end_date", endDate.format(formatter))
                .build()
                .toUri();
        return executeApiCall(uri);
    }

    private WeatherbitResponse executeApiCall(URI uri) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                logger.info("Weatherbit API 호출 성공: {}", uri);
                logger.debug("Weatherbit API 응답 본문: {}", response.body()); // 디버그 레벨로 상세 응답 기록
                return objectMapper.readValue(response.body(), WeatherbitResponse.class);
            } else {
                logger.error("Weatherbit API 호출 실패. 상태 코드: {}, 응답: {}", response.statusCode(), response.body());
                return null;
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Weatherbit API 호출 중 오류 발생: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        }
    }
}