package com.dgsw.heckathon.ai;

import com.dgsw.heckathon.weather.ForecastResponse;
import com.dgsw.heckathon.weather.TomorrowioApiService;
import com.dgsw.heckathon.weather.CurrentWeatherResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OpenAiDisasterService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiDisasterService.class);

    private final OpenAiApiService     openAiApiService;
    private final TomorrowioApiService tomorrowioApiService;

    public OpenAiDisasterService(OpenAiApiService openAiApiService,
                                 TomorrowioApiService tomorrowioApiService) {
        this.openAiApiService   = openAiApiService;
        this.tomorrowioApiService = tomorrowioApiService;
    }

    /** 시간별 예보를 요약해 OpenAI 로 재난 예측을 요청 */
    public String predictDisasterBasedOnWeather(double lat, double lon, int hours) {

        /* 1) Tomorrow.io 예보 조회 */
        ZonedDateTime nowUtc   = ZonedDateTime.now(ZoneOffset.UTC);
        String startTime = nowUtc.format(DateTimeFormatter.ISO_INSTANT);
        String endTime   = nowUtc.plusHours(hours).format(DateTimeFormatter.ISO_INSTANT);

        ForecastResponse forecast =
                tomorrowioApiService.getForecast(lat, lon, "1h", startTime, endTime);

        if (forecast == null ||
                forecast.getTimelines() == null ||
                forecast.getTimelines().getHourly() == null ||
                forecast.getTimelines().getHourly().isEmpty()) {

            logger.warn("Tomorrow.io 예보 데이터를 가져오지 못했습니다.");
            return "예보 데이터를 가져오지 못해 재난 예측을 수행할 수 없습니다.";
        }

        /* 2) 필요 구간만 추려 요약 */
        List<ForecastResponse.Interval> hourly =
                forecast.getTimelines().getHourly()
                        .stream()
                        .limit(hours)
                        .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder()
                .append("위도 ").append(lat)
                .append(", 경도 ").append(lon)
                .append(" 지역 향후 ").append(hours)
                .append("시간 예보:\n");

        for (ForecastResponse.Interval iv : hourly) {
            ForecastResponse.Values v = iv.getValues();
            String t = iv.getTime() != null && iv.getTime().length() >= 16
                    ? iv.getTime().substring(11,16) : "N/A";

            sb.append("- ").append(t)
                    .append(" | T ").append(n(v.getTemperature())).append("°C")
                    .append(" / 체감 ").append(n(v.getTemperatureApparent())).append("°C")
                    .append(" | RH ").append(n(v.getHumidity())).append("%")
                    .append(" | 강수 ").append(n(v.getPrecipitationIntensity())).append("mm/h")
                    .append(" | WS ").append(n(v.getWindSpeed())).append("m/s")
                    .append(" | 구름 ").append(n(v.getCloudCover())).append("%")
                    .append(" | 코드 ").append(n(v.getWeatherCode()))
                    .append('\n');
        }

        /* 3) OpenAI 모델로 재난 예측 요청 */
        return openAiApiService.getDisasterPrediction(sb.toString());
    }

    /** null → "N/A" 간단 변환 */
    private static String n(Object o) {
        return o == null ? "N/A" : o.toString();
    }
}