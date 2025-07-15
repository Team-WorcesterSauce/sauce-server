package com.dgsw.heckathon.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherbitResponse {
    private List<WeatherData> data;
    private String cityName;
    private double lat;
    private double lon;
    private String timezone;
    private String stateCode; // state_code
    private String countryCode; // country_code

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeatherData {
        private Double temp;
        private Double rh; // Relative Humidity
        private Double pres; // Pressure
        private Double clouds;
        private Double precip; // Precipitation
        private Double windSpd; // wind_spd
        private Integer windDir; // wind_dir
        private String windCdirFull; // wind_cdir_full

        private WeatherInfo weather; // nested object

        private String datetime;
        private Long ts; // Unix timestamp
        private String obTime; // ob_time (observation time)

        // !!! 이 부분 수정 !!!
        @JsonProperty("timestamp_local") // JSON 필드 이름 명시
        private String timestampLocal; // Java 필드 이름

        @JsonProperty("timestamp_utc") // JSON 필드 이름 명시
        private String timestampUtc; // Java 필드 이름

        private Double appTemp; // app_temp (apparent temperature)
        private Double minTemp; // min_temp (일별 예보에서 사용)
        private Double maxTemp; // max_temp (일별 예보에서 사용)
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeatherInfo {
        private String icon;
        private String code;
        private String description;
    }
}