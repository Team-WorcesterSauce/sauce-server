package com.dgsw.heckathon.ai;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OpenAiChatCompletionRequest {
    private String model; // 예: "gpt-3.5-turbo" 또는 "gpt-4"
    private List<Message> messages;
    private Double temperature; // 창의성 조절 (0.0 ~ 1.0)

    @Data
    @Builder
    public static class Message {
        private String role; // "system", "user", "assistant"
        private String content;
    }
}