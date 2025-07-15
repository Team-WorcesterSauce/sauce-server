package com.dgsw.heckathon.ai;

import com.dgsw.heckathon.weather.WeatherbitApiService;
import com.dgsw.heckathon.weather.WeatherbitResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OpenAiDisasterService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiDisasterService.class);

    private final OpenAiApiService openAiApiService;
    private final WeatherbitApiService weatherbitApiService; // WeatherbitApiService 주입

    public OpenAiDisasterService(OpenAiApiService openAiApiService, WeatherbitApiService weatherbitApiService) {
        this.openAiApiService = openAiApiService;
        this.weatherbitApiService = weatherbitApiService;
    }

    /**
     * Weatherbit.io에서 12시간 예보 데이터를 가져와 OpenAI에 전달하고 재난 예측을 받습니다.
     * @param lat 위도
     * @param lon 경도
     * @param hours 예측할 시간 범위 (예: 12)
     * @return OpenAI가 생성한 재난 예측 텍스트
     */
    public String predictDisasterBasedOnWeather(double lat, double lon, int hours) {
        // 1. Weatherbit.io에서 미래 예보 데이터 가져오기
        WeatherbitResponse forecastResponse = weatherbitApiService.getHourlyForecast(lat, lon, hours);

        if (forecastResponse == null || forecastResponse.getData() == null || forecastResponse.getData().isEmpty()) {
            logger.warn("Weatherbit API에서 기상 예보 데이터를 가져오는 데 실패했습니다.");
            return "기상 예보 데이터를 가져오는 데 실패하여 재난 예측을 할 수 없습니다.";
        }

        // 2. 가져온 기상 데이터를 OpenAI에 전달할 형식으로 요약
        StringBuilder weatherSummary = new StringBuilder();
        weatherSummary.append("위도: ").append(lat).append(", 경도: ").append(lon).append(" 지역의 ");
        weatherSummary.append("향후 ").append(hours).append("시간 동안의 시간별 기상 예보입니다:\n");

        List<WeatherbitResponse.WeatherData> relevantData = forecastResponse.getData().stream()
                .limit(hours) // 요청한 시간만큼만 데이터 사용
                .collect(Collectors.toList());

        for (WeatherbitResponse.WeatherData data : relevantData) {
            weatherSummary.append("- 시간: ").append(data.getTimestampLocal())
                    .append(", 온도: ").append(data.getTemp()).append("°C")
                    .append(", 습도: ").append(data.getRh()).append("%")
                    .append(", 강수량: ").append(data.getPrecip()).append("mm/h")
                    .append(", 풍속: ").append(data.getWindSpd()).append("m/s")
                    .append(", 날씨: ").append(data.getWeather().getDescription())
                    .append("\n");
        }

        // 3. OpenAI API에 요약된 데이터를 전달하여 재난 예측 요청
        return openAiApiService.getDisasterPrediction(weatherSummary.toString());
    }
}