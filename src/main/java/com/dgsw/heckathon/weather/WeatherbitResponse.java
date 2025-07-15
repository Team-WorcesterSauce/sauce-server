package com.dgsw.heckathon.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherbitResponse {
    private List<WeatherData> data; // 핵심 데이터 배열
    private String city_name;
    private Double lat;
    private Double lon;
    private String timezone;
    private String state_code;
    private String country_code;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeatherData {
        private Double temp; // 온도 (섭씨)
        @JsonProperty("app_temp")
        private Double appTemp; // 체감 온도
        private Double rh; // 상대 습도 (%)
        private Double pres; // 기압 (mb)
        private Double clouds; // 운량 (%)
        private Double precip; // 강수량 (mm/hr)
        private Double wind_spd; // 풍속 (m/s)
        private Integer wind_dir; // 풍향 (도)
        private String wind_cdir_full; // 풍향 (텍스트)
        private Weather weather; // 날씨 정보
        private String datetime; // 날짜/시간 (YYYY-MM-DD:HH, or YYYY-MM-DD for daily)
        private Long ts; // Unix timestamp
        private String ob_time; // Observation time for current weather
        private String timestamp_local; // Local timestamp
        private String timestamp_utc; // UTC timestamp

        // 일별 예보 (daily forecast)에서만 사용되는 필드들 (필요시 추가)
        @JsonProperty("min_temp")
        private Double minTemp;
        @JsonProperty("max_temp")
        private Double maxTemp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Weather {
        private String icon;
        private String code;
        private String description;
    }
}