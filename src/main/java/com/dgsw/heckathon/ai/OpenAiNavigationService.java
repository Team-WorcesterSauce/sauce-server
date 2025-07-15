package com.dgsw.heckathon.ai;

import com.dgsw.heckathon.weather.OpenWeatherApiService;
import com.dgsw.heckathon.weather.CurrentWeatherResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class OpenAiNavigationService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiNavigationService.class);

    private final OpenWeatherApiService openWeatherApiService; // TomorrowioApiService 대신 OpenWeatherApiService 사용
    private final ExecutorService executorService = Executors.newFixedThreadPool(10); // 동시에 10개의 API 호출

    public OpenAiNavigationService(OpenWeatherApiService openWeatherApiService) {
        this.openWeatherApiService = openWeatherApiService;
    }

    /**
     * 비·눈·우박·흐림 지역 좌표 스캔
     */
    public List<Map<String, Object>> findSpecificWeatherEventsLocations( // 반환 타입을 List<Map<String, Object>>로 변경
                                                                         double minLat, double maxLat, double minLon, double maxLon,
                                                                         double latStep, double lonStep) {

        List<Map<String, Object>> eventLocations = Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (double lat = minLat; lat <= maxLat; lat += latStep) {
            for (double lon = minLon; lon <= maxLon; lon += lonStep) {
                final double currentLat = lat;
                final double currentLon = lon;

                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        CurrentWeatherResponse currentWeather = openWeatherApiService.getCurrentWeather(currentLat, currentLon);

                        // OpenWeatherMap 응답 구조에 따라 데이터 추출
                        if (currentWeather != null && currentWeather.getMain() != null && currentWeather.getWeather() != null) {
                            List<String> eventTypes = new ArrayList<>();

                            /* ───── 강수 여부 확인 ───── */
                            // OpenWeatherMap의 weather code 또는 rain/snow 객체로 강수 여부 판단
                            boolean isRaining = currentWeather.getRain() != null && currentWeather.getRain().get_1h() != null && currentWeather.getRain().get_1h() > 0;
                            boolean isSnowing = currentWeather.getSnow() != null && currentWeather.getSnow().get_1h() != null && currentWeather.getSnow().get_1h() > 0;

                            // OpenWeatherMap weather main 필드 확인 (Rain, Snow, Drizzle 등)
                            if (currentWeather.getWeather() != null && !currentWeather.getWeather().isEmpty()) {
                                String weatherMain = currentWeather.getWeather().get(0).getMain();
                                int weatherId = currentWeather.getWeather().get(0).getId(); // weather ID로 상세 분류

                                if ("Rain".equalsIgnoreCase(weatherMain) || isRaining) {
                                    eventTypes.add("Rain"); // 비
                                } else if ("Snow".equalsIgnoreCase(weatherMain) || isSnowing) {
                                    eventTypes.add("Snow"); // 눈
                                } else if ("Drizzle".equalsIgnoreCase(weatherMain)) {
                                    eventTypes.add("Drizzle"); // 이슬비
                                } else if (weatherId >= 200 && weatherId < 300) { // Thunderstorm (2xx)
                                    eventTypes.add("Thunderstorm"); // 뇌우
                                }
                                // 우박은 weather main/description에 명시적으로 없으므로, precipitationType이 없는 OpenWeatherMap에서는 판단하기 어려움.
                                // 필요하다면, 매우 높은 강수 강도나 특정 기상 조건(온도 등)을 조합하여 유추해야 함.
                                // 현재 OpenWeatherMap API의 기본 응답으로는 우박을 명확히 구분하기 어려우므로, 이 부분은 유의해야 합니다.
                                // 만약 우박 정보가 정말 필요하다면, 유료 API 또는 다른 데이터 소스를 고려해야 합니다.
                            }

                            /* ───── 흐림 여부 확인 ───── */
                            // 구름량이 50% 이상이면 흐림으로 간주
                            if (currentWeather.getClouds() != null && currentWeather.getClouds().getAll() != null && currentWeather.getClouds().getAll() >= 50) {
                                eventTypes.add("Cloudiness"); // 흐림
                            }

                            if (!eventTypes.isEmpty()) { // 최소 하나 이상의 이벤트 유형이 감지된 경우에만 추가
                                Map<String, Object> point = new HashMap<>(); // Map<String, Object> 사용
                                point.put("lat", currentLat);
                                point.put("lon", currentLon);
                                point.put("types", eventTypes); // 이벤트 유형 리스트 추가
                                eventLocations.add(point);
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("OpenWeatherMap API 호출 실패 (lat={}, lon={}): {}", currentLat, currentLon, e.getMessage());
                        // 오류 발생 시 해당 지점은 스캔에서 제외됩니다.
                    }
                }, executorService));
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return eventLocations;
    }
}