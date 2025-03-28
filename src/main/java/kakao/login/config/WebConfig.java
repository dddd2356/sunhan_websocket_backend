package kakao.login.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${file.upload-dir}")
    private String uploadDir;
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true);
    }
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1) 상대경로 uploads → 절대경로로 변환
        String absolutePath = Paths.get(uploadDir)
                .toAbsolutePath().toString() + "/";

        // ① "/api/uploads/**" 로도, ② "/uploads/**" 로도 매핑
        registry.addResourceHandler("/api/uploads/**", "/uploads/**")
                .addResourceLocations("file:" + absolutePath);
    }
}
