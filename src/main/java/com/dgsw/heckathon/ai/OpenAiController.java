package com.dgsw.heckathon.ai;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class OpenAiController {

    private final OpenAiDisasterService openAiDisasterService;

    public OpenAiController(OpenAiDisasterService openAiDisasterService) {
        this.openAiDisasterService = openAiDisasterService;
    }

    @GetMapping("/disaster")
    public ResponseEntity<Map<String, String>> getDisasterPrediction(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "12") int hours) { // 기본값 12시간

        String aiPrediction = openAiDisasterService.predictDisasterBasedOnWeather(lat, lon, hours);

        Map<String, String> response = new HashMap<>();
        if (aiPrediction != null && !aiPrediction.isEmpty()) {
            response.put("prediction", aiPrediction);
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "AI 재난 예측 정보를 가져오는 데 실패했습니다.");
            return ResponseEntity.status(500).body(response);
        }
    }
}