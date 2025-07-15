package com.dgsw.heckathon.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CurrentWeatherResponse {

    private DataPayload data;

    @Data
    public static class DataPayload {
        private String time;

        @JsonProperty("values")
        private Values values;
    }

    @Data
    public static class Values {
        private Double temperature;
        private Double humidity;
        private Double precipitationIntensity;
        private Integer precipitationType;
        private Double windSpeed;
        private Double windDirection;
        private Double cloudCover;
        private Integer weatherCode;
        private Double temperatureApparent;
        // 필요한 필드 추가 가능
    }
}