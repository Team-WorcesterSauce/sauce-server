package com.dgsw.heckathon.weather;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ForecastResponse {

    private Timelines timelines;

    @Data
    public static class Timelines {
        private List<Interval> hourly;
        private List<Interval> daily;   // 필요 없으면 제거해도 무방
    }

    @Data
    public static class Interval {
        private String time;

        private Values values;
    }

    @Data
    public static class Values {
        @JsonProperty("temperature")
        private Double temperature;

        @JsonProperty("temperatureApparent")
        private Double temperatureApparent;

        @JsonProperty("humidity")
        private Double humidity;

        @JsonProperty("precipitationIntensity")
        private Double precipitationIntensity;

        @JsonProperty("precipitationType")
        private Integer precipitationType;

        @JsonProperty("windSpeed")
        private Double windSpeed;

        @JsonProperty("windDirection")
        private Double windDirection;

        @JsonProperty("cloudCover")
        private Double cloudCover;

        @JsonProperty("weatherCode")
        private Integer weatherCode;
    }
}