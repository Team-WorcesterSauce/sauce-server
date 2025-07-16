package com.dgsw.heckathon;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration // 이 클래스가 스프링 설정 클래스임을 나타냅니다.
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 엔드포인트에 CORS를 적용합니다.
                 .allowedOrigins("http://localhost:8081")
                // 프론트엔드 애플리케이션이 실행되는 오리진(도메인:포트)을 정확히 지정해야 합니다.
                // 개발 중에는 '*'로 모든 오리진을 허용할 수 있지만, 실제 배포 시에는 보안상 특정 오리진만 허용하는 것이 좋습니다.
                // 현재는 테스트를 위해 '*'를 사용하고, 나중에 클라이언트 주소로 변경해주세요.
                .allowedOriginPatterns("*") // 모든 오리진 패턴을 허용 (allowCredentials와 함께 사용 시 '*' 대신 사용)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 허용할 HTTP 메서드를 지정합니다.
                .allowedHeaders("*") // 모든 헤더를 허용합니다.
                .allowCredentials(true) // 자격 증명(쿠키, HTTP 인증 등)을 허용합니다.
                .maxAge(3600); // Pre-flight 요청 결과를 캐싱할 시간 (초 단위)
    }
}