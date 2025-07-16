package com.dgsw.heckathon.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Service
public class OpenAiApiService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiApiService.class);

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.base-url}")
    private String baseUrl;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiApiService(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)) // 연결 타임아웃 설정 (선택 사항)
                .build();
        this.objectMapper = objectMapper;
    }

    public String getDisasterPrediction(String weatherDataSummary) {
        // OpenAI 모델에 보낼 메시지 구성
        String systemPrompt = "당신은 기상 데이터를 분석하여 잠재적인 해양 재난 위험을 경고하고 안전 수칙을 제안하는 전문가입니다. " +
                "아래 제공된 12시간 기상 예보 데이터를 기반으로, 발생 가능한 항해 관련 재난 위험을 간결하게 요약하고 " +
                "각 위험에 대한 간단한 안전 수칙을 제시해주세요. 한국어로 답변해주세요. 그리고 글로만 써주세요 강조표시 같은 거 없이";

        String userPrompt = "다음은 12시간 동안의 기상 예보입니다:\n\n" + weatherDataSummary +
                "\n\n이 기상 조건에서 예상되는 재난 위험과 그에 대한 안전 수칙을 알려주세요.";

        OpenAiChatCompletionRequest request = OpenAiChatCompletionRequest.builder()
                .model("gpt-3.5-turbo") // 또는 "gpt-4" (비용 및 성능 고려)
                .messages(List.of(
                        OpenAiChatCompletionRequest.Message.builder().role("system").content(systemPrompt).build(),
                        OpenAiChatCompletionRequest.Message.builder().role("user").content(userPrompt).build()
                ))
                .temperature(0.7) // 0.0 (보수적) ~ 1.0 (창의적)
                .build();

        try {
            String requestBody = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                logger.info("OpenAI API 호출 성공");
                logger.debug("OpenAI API 응답 본문: {}", response.body());
                OpenAiChatCompletionResponse chatResponse = objectMapper.readValue(response.body(), OpenAiChatCompletionResponse.class);
                if (chatResponse != null && chatResponse.getChoices() != null && !chatResponse.getChoices().isEmpty()) {
                    return chatResponse.getChoices().get(0).getMessage().getContent();
                }
            } else {
                logger.error("OpenAI API 호출 실패. 상태 코드: {}, 응답: {}", response.statusCode(), response.body());
            }
        } catch (IOException | InterruptedException e) {
            logger.error("OpenAI API 호출 중 오류 발생: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
        return "재난 예측 정보를 가져오는 데 실패했습니다.";
    }
}
