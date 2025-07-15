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
import java.util.List;
import java.util.stream.Collectors;

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

    // OpenAI에 전달할 요약 기능은 OpenAiDisasterService로 이동하므로 여기서 제거합니다.
    /*
    public String getForecastSummaryForOpenAI(double lat, double lon, int hours) {
        WeatherbitResponse forecastResponse = getHourlyForecast(lat, lon, hours);

        if (forecastResponse != null && forecastResponse.getData() != null && !forecastResponse.getData().isEmpty()) {
            StringBuilder summary = new StringBuilder();
            summary.append("위도: ").append(lat).append(", 경도: ").append(lon).append(" 지역의 ");
            summary.append("향후 ").append(hours).append("시간 동안의 시간별 기상 예보입니다:\n");

            List<WeatherbitResponse.WeatherData> relevantData = forecastResponse.getData().stream()
                    .limit(hours) // 요청한 시간만큼만 데이터 사용
                    .collect(Collectors.toList());

            for (WeatherbitResponse.WeatherData data : relevantData) {
                summary.append("- 시간: ").append(data.getTimestampLocal())
                        .append(", 온도: ").append(data.getTemp()).append("°C")
                        .append(", 습도: ").append(data.getRh()).append("%")
                        .append(", 강수량: ").append(data.getPrecip()).append("mm/h")
                        .append(", 풍속: ").append(data.getWindSpd()).append("m/s")
                        .append(", 날씨: ").append(data.getWeather().getDescription())
                        .append("\n");
            }
            return summary.toString();
        } else {
            return "기상 예보 데이터를 가져오는 데 실패했습니다.";
        }
    }
    */

    private WeatherbitResponse executeApiCall(URI uri) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                logger.info("Weatherbit API 호출 성공: {}", uri);
                logger.debug("Weatherbit API 응답 본문: {}", response.body());
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