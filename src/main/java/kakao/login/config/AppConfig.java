package kakao.login.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration  // Spring에서 설정 클래스임을 나타내는 어노테이션
public class AppConfig {

    // RestTemplate Bean을 등록하여 애플리케이션에서 사용할 수 있게 함
    @Bean  // 이 메서드를 통해 반환된 객체는 Spring IoC 컨테이너에 관리될 빈으로 등록됨
    public RestTemplate restTemplate() {
        // RestTemplate 객체를 생성하여 반환
        return new RestTemplate();
    }

    // WebClient Bean을 등록하여 애플리케이션에서 사용할 수 있게 함
    @Bean  // 이 메서드를 통해 반환된 객체는 Spring IoC 컨테이너에 관리될 빈으로 등록됨
    public WebClient webClient() {
        // WebClient 객체를 빌더 패턴을 통해 생성하여 반환
        return WebClient.builder().build();
    }
}
