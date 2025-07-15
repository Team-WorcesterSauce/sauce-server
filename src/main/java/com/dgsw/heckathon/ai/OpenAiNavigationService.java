package com.dgsw.heckathon.ai;

import com.dgsw.heckathon.weather.TomorrowioApiService;
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

    private final TomorrowioApiService tomorrowioApiService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public OpenAiNavigationService(TomorrowioApiService tomorrowioApiService) {
        this.tomorrowioApiService = tomorrowioApiService;
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
                final double currentLon = lon; // 'double currentLon' 중복 선언 수정

                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        CurrentWeatherResponse res = tomorrowioApiService.getCurrentWeather(currentLat, currentLon);

                        if (res != null && res.getData() != null && res.getData().getValues() != null) {

                            CurrentWeatherResponse.Values v = res.getData().getValues();
                            List<String> eventTypes = new ArrayList<>(); // 감지된 모든 이벤트 유형을 저장

                            /* ───── 강수 여부 확인 ───── */
                            if (v.getPrecipitationIntensity() != null && v.getPrecipitationIntensity() > 0) {
                                switch (v.getPrecipitationType() != null ? v.getPrecipitationType() : 0) {
                                    case 1:
                                        eventTypes.add("Rain"); // 비
                                        break;
                                    case 2:
                                        eventTypes.add("Freezing Rain"); // 어는 비
                                        break;
                                    case 3:
                                        eventTypes.add("Snow"); // 눈
                                        break;
                                    case 4:
                                        eventTypes.add("Sleet"); // 진눈깨비
                                        break;
                                    case 5:
                                        eventTypes.add("Hail"); // 우박
                                        break;
                                    default:
                                        eventTypes.add("Precipitation"); // 유형을 알 수 없지만 강수량이 0보다 큰 경우 일반적인 강수
                                }
                            }

                            /* ───── 흐림 여부 확인 ───── */
                            // 구름량이 50% 이상이면 흐림으로 간주
                            if (v.getCloudCover() != null && v.getCloudCover() >= 50) {
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
                        logger.warn("Tomorrow.io 호출 실패 (lat={}, lon={}): {}", currentLat, currentLon, e.getMessage());
                    }
                }, executorService));
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return eventLocations;
    }
}