package com.dgsw.heckathon.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // 응답에서 사용하지 않는 필드는 무시
public class ForecastResponse {

    private String cod;
    private Integer message;
    private Integer cnt;
    private List<ForecastList> list;
    private City city;

    @Data
    public static class ForecastList {
        private Long dt;
        private Main main;
        private List<Weather> weather;
        private Clouds clouds;
        private Wind wind;
        private Integer visibility;
        private Double pop; // Probability of precipitation
        private Rain rain; // 강수량 정보가 있을 수 있음
        private Snow snow; // 적설량 정보가 있을 수 있음
        private Sys sys;
        @JsonProperty("dt_txt")
        private String dtTxt; // Data forecast time in UTC

        @Data
        public static class Main {
            private Double temp;
            @JsonProperty("feels_like")
            private Double feelsLike;
            @JsonProperty("temp_min")
            private Double tempMin;
            @JsonProperty("temp_max")
            private Double tempMax;
            private Integer pressure;
            private Integer humidity;
            @JsonProperty("sea_level")
            private Integer seaLevel;
            @JsonProperty("grnd_level")
            private Integer grndLevel;
            @JsonProperty("temp_kf")
            private Double tempKf; // Internal parameter
        }

        @Data
        public static class Weather {
            private int id;
            private String main;
            private String description;
            private String icon;
        }

        @Data
        public static class Clouds {
            private Integer all;
        }

        @Data
        public static class Wind {
            private Double speed;
            private Integer deg;
            private Double gust;
        }

        @Data
        public static class Rain {
            @JsonProperty("3h")
            private Double _3h; // Rain volume for last 3 hours
        }

        @Data
        public static class Snow {
            @JsonProperty("3h")
            private Double _3h; // Snow volume for last 3 hours
        }

        @Data
        public static class Sys {
            private String pod; // Part of the day (d - day, n - night)
        }
    }

    @Data
    public static class City {
        private Integer id;
        private String name;
        private Coord coord;
        private String country;
        private Long population;
        private Integer timezone;
        private Long sunrise;
        private Long sunset;

        @Data
        public static class Coord {
            private Double lat;
            private Double lon;
        }
    }
}