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
    public List<Map<String, Double>> findSpecificWeatherEventsLocations(
            double minLat, double maxLat, double minLon, double maxLon,
            double latStep, double lonStep) {

        List<Map<String, Double>> eventLocations = Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (double lat = minLat; lat <= maxLat; lat += latStep) {
            for (double lon = minLon; lon <= maxLon; lon += lonStep) {

                final double currentLat = lat;
                final double currentLon = lon;

                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        CurrentWeatherResponse res = tomorrowioApiService.getCurrentWeather(currentLat, currentLon);

                        if (res != null && res.getData() != null && res.getData().getValues() != null) {

                            CurrentWeatherResponse.Values v = res.getData().getValues();
                            boolean isEvent = false;

                            /* ───── 강수 여부 ───── */
                            if (v.getPrecipitationIntensity() != null && v.getPrecipitationIntensity() > 0) {
                                isEvent = true;
                            } else if (v.getPrecipitationType() != null && v.getPrecipitationType() > 0) {
                                isEvent = true;
                            }

                            /* ───── 흐림 여부 ───── */
                            if (!isEvent && v.getCloudCover() != null && v.getCloudCover() >= 50) {
                                isEvent = true;
                            }

                            if (isEvent) {
                                Map<String, Double> point = Map.of("lat", currentLat, "lon", currentLon);
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