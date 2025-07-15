package com.dgsw.heckathon.ai;

import com.dgsw.heckathon.weather.ForecastResponse;
import com.dgsw.heckathon.weather.OpenWeatherApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OpenAiDisasterService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiDisasterService.class);

    private final OpenAiApiService openAiApiService;
    private final OpenWeatherApiService openWeatherApiService; // 필드명 변경 및 타입 변경

    public OpenAiDisasterService(OpenAiApiService openAiApiService,
                                 OpenWeatherApiService openWeatherApiService) { // 생성자 주입 타입 변경
        this.openAiApiService = openAiApiService;
        this.openWeatherApiService = openWeatherApiService;
    }

    /** 시간별 예보를 요약해 OpenAI 로 재난 예측을 요청 */
    public String predictDisasterBasedOnWeather(double lat, double lon, int hours) {

        /* 1) OpenWeatherMap 예보 조회 */
        // OpenWeatherMap은 startTime, endTime, timesteps 개념 대신 5일 3시간 단위 예보를 제공
        ForecastResponse forecast = openWeatherApiService.getForecast(lat, lon);

        if (forecast == null || forecast.getList() == null || forecast.getList().isEmpty()) {
            logger.warn("OpenWeatherMap 예보 데이터를 가져오지 못했습니다. 위도: {}, 경도: {}", lat, lon);
            return "날씨 데이터를 가져오는 데 실패하여 재난 예측을 할 수 없습니다.";
        }

        /* 2) 예보 데이터 요약 */
        // OpenWeatherMap의 3시간 단위 예보에서 요청된 'hours' 만큼의 데이터만 사용
        // OpenWeatherMap의 예보는 3시간 간격이므로, 정확히 'hours'를 맞추기 어려울 수 있음
        List<ForecastResponse.ForecastList> hourlyForecasts = forecast.getList()
                .stream()
                // hours를 3시간 단위로 나누어 필요한 예보 항목 수 계산 (최대 5일 = 40개 항목)
                .limit(Math.min(hours / 3 + 1, forecast.getList().size())) // 대략적인 시간 범위
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder()
                .append("위도 ").append(lat)
                .append(", 경도 ").append(lon)
                .append(" 지역 향후 약 ").append(hours).append("시간 예보 (3시간 간격):\\n"); // 3시간 간격 명시

        for (ForecastResponse.ForecastList item : hourlyForecasts) {
            ForecastResponse.ForecastList.Main main = item.getMain();
            ForecastResponse.ForecastList.Wind wind = item.getWind();
            List<ForecastResponse.ForecastList.Weather> weatherList = item.getWeather();

            String dtTxt = item.getDtTxt() != null ? item.getDtTxt().substring(0, 16) : "N/A"; // 날짜 및 시간 형식 맞춤

            sb.append("- ").append(dtTxt)
                    .append(" | T ").append(n(main.getTemp())).append("°C")
                    .append(" / 체감 ").append(n(main.getFeelsLike())).append("°C")
                    .append(" | RH ").append(n(main.getHumidity())).append("%");

            // 강수량 (Rain 또는 Snow 객체가 존재할 경우)
            if (item.getRain() != null && item.getRain().get_3h() != null && item.getRain().get_3h() > 0) {
                sb.append(" | 강수 ").append(n(item.getRain().get_3h())).append("mm/3h");
            } else if (item.getSnow() != null && item.getSnow().get_3h() != null && item.getSnow().get_3h() > 0) {
                sb.append(" | 적설 ").append(n(item.getSnow().get_3h())).append("mm/3h");
            } else if (item.getPop() != null) { // 강수 확률 (Probability of precipitation)
                sb.append(" | 강수확률 ").append(n(item.getPop() * 100)).append("%");
            }

            sb.append(" | WS ").append(n(wind.getSpeed())).append("m/s");

            if (item.getClouds() != null && item.getClouds().getAll() != null) {
                sb.append(" | 구름 ").append(n(item.getClouds().getAll())).append("%");
            }
            if (weatherList != null && !weatherList.isEmpty()) {
                sb.append(" | 날씨: ").append(weatherList.get(0).getDescription()); // 첫 번째 날씨 설명
            }
            sb.append("\\n");
        }

        /* 3) OpenAI 모델로 재난 예측 요청 */
        return openAiApiService.getDisasterPrediction(sb.toString());
    }

    /** null → "N/A" 간단 변환 */
    private String n(Object value) {
        return value != null ? String.valueOf(value) : "N/A";
    }

    /** null → 0.0 간단 변환 */
    private Double nDouble(Double value) {
        return value != null ? value : 0.0;
    }
}