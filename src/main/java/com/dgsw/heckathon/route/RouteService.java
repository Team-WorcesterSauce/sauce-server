package com.dgsw.heckathon.route;

import com.dgsw.heckathon.weather.CurrentWeatherResponse;
import com.dgsw.heckathon.weather.OpenWeatherApiService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Map.entry;

@Service
public class RouteService {

    @Value("${openai.api.key}")
    private String openaiApiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final OpenWeatherApiService openWeatherApiService;

    public RouteService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
                        OpenWeatherApiService openWeatherApiService) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
        this.openWeatherApiService = openWeatherApiService;
    }

    public List<Waypoint> calculateOptimalRoute(double startLat, double startLon,
                                                double endLat, double endLon) throws Exception {

        CurrentWeatherResponse currentWeather = openWeatherApiService.getCurrentWeather(startLat, startLon);
        Map<String, Object> weatherData = extractWeatherDataForPrompt(currentWeather);

        String prompt = buildPrompt(startLat, startLon, endLat, endLon, weatherData);

        String openaiResponse = callOpenAiApi(prompt);

        return parseOpenAiResponse(openaiResponse);
    }

    private Map<String, Object> extractWeatherDataForPrompt(CurrentWeatherResponse response) {
        Map<String, Object> data = new HashMap<>();
        if (response != null && response.getMain() != null) {
            data.put("temperature", response.getMain().getTemp());
            data.put("feelsLike", response.getMain().getFeelsLike());
            data.put("humidity", response.getMain().getHumidity());
            if (response.getWind() != null) {
                data.put("windSpeed", response.getWind().getSpeed());
                data.put("windDirection", response.getWind().getDeg());
            }
            if (response.getClouds() != null) {
                data.put("cloudCover", response.getClouds().getAll());
            }
            if (response.getWeather() != null && !response.getWeather().isEmpty()) {
                data.put("weatherMain", response.getWeather().get(0).getMain());
                data.put("weatherDescription", response.getWeather().get(0).getDescription());
                data.put("weatherId", response.getWeather().get(0).getId());
            }
            if (response.getRain() != null && response.getRain().get_1h() != null) {
                data.put("rain1h", response.getRain().get_1h());
            }
            if (response.getSnow() != null && response.getSnow().get_1h() != null) {
                data.put("snow1h", response.getSnow().get_1h());
            }
        }
        return data;
    }

    private String buildPrompt(double startLat, double startLon, double endLat, double endLon, Map<String, Object> weatherData) {
        StringBuilder weatherInfo = new StringBuilder();
        if (weatherData != null && !weatherData.isEmpty()) {
            weatherInfo.append("현재 날씨 정보 (출발 지점 주변):\n");
            weatherInfo.append("  온도: ").append(weatherData.getOrDefault("temperature", "N/A")).append("°C\n");
            weatherInfo.append("  체감 온도: ").append(weatherData.getOrDefault("feelsLike", "N/A")).append("°C\n");
            weatherInfo.append("  습도: ").append(weatherData.getOrDefault("humidity", "N/A")).append("%\n");
            weatherInfo.append("  풍속: ").append(weatherData.getOrDefault("windSpeed", "N/A")).append("m/s\n");
            weatherInfo.append("  풍향: ").append(weatherData.getOrDefault("windDirection", "N/A")).append("°\n");
            weatherInfo.append("  구름량: ").append(weatherData.getOrDefault("cloudCover", "N/A")).append("%\n");
            weatherInfo.append("  날씨: ").append(weatherData.getOrDefault("weatherDescription", "N/A")).append(" (").append(weatherData.getOrDefault("weatherMain", "N/A")).append(")\n");
            if (weatherData.containsKey("rain1h")) {
                weatherInfo.append("  1시간 강수량: ").append(weatherData.get("rain1h")).append("mm\n");
            }
            if (weatherData.containsKey("snow1h")) {
                weatherInfo.append("  1시간 적설량: ").append(weatherData.get("snow1h")).append("mm\n");
            }
            weatherInfo.append("이 날씨 정보는 경로 선택에 중요한 요소입니다. 특히 높은 풍속, 강한 강수량, 악천후(뇌우, 폭설 등)는 피해야 합니다.\n\n");
        } else {
            weatherInfo.append("날씨 정보를 가져올 수 없습니다. 경로 최적화 시 날씨를 고려할 수 없습니다.\n\n");
        }

        return String.format(
                "당신은 해양 경로 전문가입니다. 해양 기상 데이터를 기반으로 가장 안전하고 효율적인 해상 경로를 안내해야 합니다. " +
                        "다음은 출발지와 목적지 좌표입니다: 출발지 (위도: %.6f, 경도: %.6f), 목적지 (위도: %.6f, 경도: %.6f).\n\n" +
                        "%s" +
                        "이 두 지점 사이의 최적의 해양 경로를 위도와 경도 쌍의 JSON 배열로 제공해주세요. " +
                        "경로는 약 5~10개의 경유지(waypoint)로 구성되어야 하며, 각 경유지는 다음 형식의 JSON 객체여야 합니다: " +
                        "추가로 육지는 건널 수 없습니다." +
                        "{ \"latitude\": [위도], \"longitude\": [경도] }.\n" +
                        "JSON 응답만 제공하고 다른 설명은 일절 포함하지 마세요. JSON은 반드시 `{\"waypoints\": [...]}` 형식이어야 합니다.\n" +
                        "예시: {\"waypoints\": [{\"latitude\": 34.5, \"longitude\": 127.0}, {\"latitude\": 35.0, \"longitude\": 128.0}]}",
                startLat, startLon, endLat, endLon, weatherInfo.toString()
        );
    }

    private String callOpenAiApi(String prompt) {
        String url = "https://api.openai.com/v1/chat/completions";
        String model = "gpt-3.5-turbo";

        ArrayNode messages = objectMapper.createArrayNode();

        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are an expert in marine routing, providing the safest and most efficient sea routes based on current weather conditions. Provide only the JSON response for the waypoints.");
        messages.add(systemMessage);

        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.set("messages", messages);
        requestBody.put("temperature", 0.7);

        Mono<String> responseMono = webClient.post()
                .uri(url)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openaiApiKey)
                .bodyValue(requestBody.toString())
                .retrieve()
                .bodyToMono(String.class);

        return responseMono.block();
    }

    private List<Waypoint> parseOpenAiResponse(String openaiResponse) throws JsonProcessingException {
        List<Waypoint> waypoints = new ArrayList<>();

        // 1. 전체 OpenAI API 응답을 JsonNode로 파싱
        JsonNode rootNode = objectMapper.readTree(openaiResponse);

        // 2. choices[0].message.content 경로로 이동하여 실제 웨이포인트 JSON 문자열 추출
        JsonNode contentNode = rootNode.path("choices").path(0).path("message").path("content");

        if (contentNode.isMissingNode() || !contentNode.isTextual()) {
            System.err.println("OpenAI response did not contain expected content field. Raw response: " + openaiResponse);
            throw new IllegalStateException("OpenAI response structure invalid: missing or non-textual 'choices[0].message.content'.");
        }

        String jsonToParse = contentNode.asText(); // 이 문자열이 웨이포인트를 포함하는 JSON

        // 3. 추출된 문자열이 ```json ... ``` 형태로 래핑되어 있는지 확인하고 실제 JSON만 추출
        Pattern pattern = Pattern.compile("```json\\s*([\\s\\S]*?)\\s*```");
        Matcher matcher = pattern.matcher(jsonToParse);

        String finalJsonForWaypoints;
        if (matcher.find()) {
            finalJsonForWaypoints = matcher.group(1).trim();
        } else {
            // ```json 블록이 없으면, content 문자열 자체가 JSON일 수 있음
            finalJsonForWaypoints = jsonToParse.trim();
        }

        // 4. 웨이포인트를 포함하는 JSON 문자열을 다시 JsonNode로 파싱
        JsonNode actualWaypointsJson;
        try {
            actualWaypointsJson = objectMapper.readTree(finalJsonForWaypoints);
        } catch (JsonProcessingException e) {
            System.err.println("Failed to parse inner content as JSON. Content attempted to parse: \"" + finalJsonForWaypoints + "\"");
            throw new IllegalStateException("OpenAI returned inner content that was not valid JSON for waypoints. Content: \"" + finalJsonForWaypoints + "\"", e);
        }

        // 5. 파싱된 내부 JSON에서 'waypoints' 배열 노드 찾기
        JsonNode waypointsNode = actualWaypointsJson.path("waypoints");

        if (waypointsNode.isArray()) {
            for (JsonNode waypointNode : waypointsNode) {
                double lat = waypointNode.path("latitude").asDouble(0.0);
                double lon = waypointNode.path("longitude").asDouble(0.0);
                waypoints.add(new Waypoint(lat, lon));
            }
        } else {
            System.err.println("Inner OpenAI response JSON did not contain a 'waypoints' array as expected. Raw parsed inner JSON: " + actualWaypointsJson.toPrettyString());
            throw new IllegalStateException("OpenAI response inner content was valid JSON but did not contain a 'waypoints' array.");
        }

        return waypoints;
    }
}