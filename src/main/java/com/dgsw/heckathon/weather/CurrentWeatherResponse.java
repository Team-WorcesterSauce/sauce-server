package com.dgsw.heckathon.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // 응답에서 사용하지 않는 필드는 무시
public class CurrentWeatherResponse {

    private Coord coord;
    private List<Weather> weather;
    private String base;
    private Main main;
    private Wind wind;
    private Clouds clouds;
    private Rain rain; // 강수량 정보가 있을 수 있음
    private Snow snow; // 적설량 정보가 있을 수 있음
    private long dt;
    private Sys sys;
    private int timezone;
    private long id;
    private String name;
    private int cod;

    @Data
    public static class Coord {
        private Double lon;
        private Double lat;
    }

    @Data
    public static class Weather {
        private int id;
        private String main;
        private String description;
        private String icon;
    }

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
    }

    @Data
    public static class Wind {
        private Double speed;
        private Integer deg;
        private Double gust;
    }

    @Data
    public static class Clouds {
        private Integer all;
    }

    @Data
    public static class Rain {
        @JsonProperty("1h")
        private Double _1h;
        @JsonProperty("3h")
        private Double _3h;
    }

    @Data
    public static class Snow {
        @JsonProperty("1h")
        private Double _1h;
        @JsonProperty("3h")
        private Double _3h;
    }

    @Data
    public static class Sys {
        private Integer type;
        private Long id;
        private String country;
        private Long sunrise;
        private Long sunset;
    }
}