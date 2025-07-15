package com.dgsw.heckathon.route;

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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Map.entry;

@Service
public class RouteService {

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${tomorrowio.api.key}")
    private String weatherApiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public RouteService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public List<Waypoint> calculateOptimalRoute(double startLat, double startLon,
                                                double endLat, double endLon) throws Exception {

        Map<String, Object> weatherData = getWeatherData(startLat, startLon);

        String promptJson = buildOpenAIPrompt(startLat, startLon, endLat, endLon, weatherData);

        String openaiResponse = callOpenAIApi(promptJson);

        return parseOpenAIResponse(openaiResponse);
    }

    private Map<String, Object> getWeatherData(double lat, double lon) {
        String fields = "temperature,windSpeed,windDirection";
        String units = "metric";
        String timesteps = "1h";
        String startTime = Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);

        String url = String.format("https://api.tomorrow.io/v4/timelines?location=%f,%f&fields=%s&units=%s&timesteps=%s&startTime=%s&apikey=%s",
                lat, lon, fields, units, timesteps, startTime, weatherApiKey);

        System.out.println("Tomorrow.io URL: " + url);

        Mono<String> responseMono = webClient.get()
                .uri(url)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            System.err.println("Tomorrow.io API Error Response: " + errorBody);
                            return Mono.error(new RuntimeException("Tomorrow.io API returned an error: " + clientResponse.statusCode()));
                        }))
                .bodyToMono(String.class);

        try {
            String jsonResponse = responseMono.block();
            System.out.println("Tomorrow.io Raw Response: " + jsonResponse);

            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode dataNode = rootNode.path("data");
            JsonNode timelinesNode = dataNode.path("timelines");

            if (timelinesNode.isArray() && timelinesNode.size() > 0) {
                JsonNode firstTimeline = timelinesNode.get(0);
                JsonNode intervalsNode = firstTimeline.path("intervals");

                if (intervalsNode.isArray() && intervalsNode.size() > 0) {
                    JsonNode currentInterval = intervalsNode.get(0);
                    JsonNode valuesNode = currentInterval.path("values");

                    double temperature = valuesNode.path("temperature").asDouble(-273.15);
                    double windSpeed = valuesNode.path("windSpeed").asDouble(-1.0);
                    double windDirection = valuesNode.path("windDirection").asDouble(-1.0);

                    return Map.ofEntries(
                            entry("current_latitude", lat),
                            entry("current_longitude", lon),
                            entry("temperature_celsius", temperature),
                            entry("wind_speed_mps", windSpeed),
                            entry("wind_direction_deg", windDirection)
                    );
                }
            }
            throw new RuntimeException("Tomorrow.io API response did not contain expected 'intervals' data.");

        } catch (Exception e) {
            System.err.println("Error fetching weather data from Tomorrow.io: " + e.getMessage());
            throw new RuntimeException("Failed to fetch weather data from Tomorrow.io", e);
        }
    }


    private String buildOpenAIPrompt(double startLat, double startLon,
                                     double endLat, double endLon,
                                     Map<String, Object> weatherData) throws Exception {

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", "gpt-4o");

        ArrayNode messages = objectMapper.createArrayNode();

        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are a highly experienced maritime navigation expert. Your task is to calculate the most optimal, safe, and efficient sailing route between two given points, considering real-time weather and oceanographic conditions. Provide the route as a series of intermediate waypoints (latitude and longitude pairs) in a JSON array format.");
        messages.add(systemMessage);

        StringBuilder userContentBuilder = new StringBuilder();
        userContentBuilder.append(String.format("Please provide an optimal sailing route from these starting coordinates: Latitude %.4f, Longitude %.4f\n", startLat, startLon));
        userContentBuilder.append(String.format("To these destination coordinates: Latitude %.4f, Longitude %.4f\n", endLat, endLon));
        userContentBuilder.append("Current and forecasted environmental conditions in the region are as follows:\n");

        weatherData.forEach((key, value) -> userContentBuilder.append(String.format("%s: %s\n", key, value)));

        // OpenAI에게 JSON만 반환하도록 더 강력히 지시하는 프롬프트 추가
        // ```json``` 블록을 요구하는 것이 아니라, 오직 JSON만 요구합니다.
        userContentBuilder.append("The ONLY output should be a JSON object with a single key 'waypoints', which contains an array of objects. Each object in the array must have 'latitude' and 'longitude' keys with double values. DO NOT include any explanatory text, markdown outside of the JSON, or any other conversational elements. Provide ONLY the JSON. Example format: {\"waypoints\": [{\"latitude\": 34.123, \"longitude\": -118.456}, {\"latitude\": 34.567, \"longitude\": -119.890}]}");

        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", userContentBuilder.toString());
        messages.add(userMessage);

        requestBody.set("messages", messages);

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestBody);
    }


    private String callOpenAIApi(String promptJson) throws Exception {
        String url = "https://api.openai.com/v1/chat/completions";

        Mono<String> responseMono = webClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + openaiApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(promptJson)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            System.err.println("OpenAI API Error Response: " + errorBody);
                            return Mono.error(new RuntimeException("OpenAI API returned an error: " + clientResponse.statusCode()));
                        }))
                .bodyToMono(String.class);

        try {
            String jsonResponse = responseMono.block();
            System.out.println("OpenAI Raw Response: " + jsonResponse);
            return jsonResponse;
        } catch (Exception e) {
            throw new RuntimeException("Failed to call OpenAI API: " + e.getMessage(), e);
        }
    }


    // --- OpenAI 응답 파싱 ---
    // ```json``` 블록이 있든 없든 처리할 수 있도록 수정
    private List<Waypoint> parseOpenAIResponse(String openaiResponse) throws Exception {
        List<Waypoint> waypoints = new ArrayList<>();
        JsonNode rootNode = objectMapper.readTree(openaiResponse);

        JsonNode messageContentNode = rootNode.path("choices").get(0).path("message").path("content");

        if (messageContentNode.isTextual()) {
            String contentString = messageContentNode.asText();
            System.out.println("OpenAI Content String (before JSON extraction): " + contentString); // 디버깅 추가

            JsonNode actualRouteData;
            String jsonToParse = null;

            // 1. 먼저 ```json ... ``` 블록이 있는지 시도한다.
            Matcher matcher = Pattern.compile("```json\\s*([\\s\\S]*?)\\s*```").matcher(contentString);
            if (matcher.find()) {
                jsonToParse = matcher.group(1); // 첫 번째 캡처 그룹이 JSON 내용
                System.out.println("Extracted JSON block: " + jsonToParse); // 추출된 JSON 블록 출력
            } else {
                // 2. ```json``` 블록이 없으면, 전체 contentString가 JSON이라고 가정한다.
                jsonToParse = contentString;
                System.out.println("No ```json``` block found, attempting to parse entire content string as JSON: " + jsonToParse);
            }

            try {
                actualRouteData = objectMapper.readTree(jsonToParse); // 추출 또는 전체 JSON 문자열 파싱
            } catch (JsonProcessingException e) { // JsonParseException은 JsonProcessingException의 하위 클래스
                System.err.println("Failed to parse extracted content as JSON. Content attempted to parse: \"" + jsonToParse + "\"");
                throw new IllegalStateException("OpenAI returned content that was not valid JSON. Content: \"" + jsonToParse + "\"", e);
            }

            JsonNode waypointsNode = actualRouteData.path("waypoints");

            if (waypointsNode.isArray()) {
                for (JsonNode waypointNode : waypointsNode) {
                    // 필드가 존재하지 않거나 숫자가 아니면 0.0을 기본값으로 사용
                    double lat = waypointNode.path("latitude").asDouble(0.0);
                    double lon = waypointNode.path("longitude").asDouble(0.0);
                    waypoints.add(new Waypoint(lat, lon));
                }
            } else {
                System.err.println("OpenAI response did not contain a 'waypoints' array as expected. Raw parsed JSON: " + actualRouteData.toPrettyString());
                // 'waypoints' 배열이 없으면 오류를 명확히 함
                throw new IllegalStateException("OpenAI response content was valid JSON but did not contain a 'waypoints' array.");
            }
        } else {
            System.err.println("OpenAI response content was not a text node. Raw response: " + openaiResponse);
            throw new IllegalStateException("OpenAI response content was not a text node as expected.");
        }

        return waypoints;
    }
}