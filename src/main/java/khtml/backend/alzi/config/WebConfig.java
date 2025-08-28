package khtml.backend.alzi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
			.allowedOriginPatterns("*")  // 모든 도메인 허용
			.allowedMethods("*")         // 모든 HTTP 메소드 허용
			.allowedHeaders("*")         // 모든 헤더 허용
			.exposedHeaders("Authorization", "Content-Type")
			.allowCredentials(false)     // credentials 비활성화로 완전히 열어놓기
			.maxAge(3600);
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// 업로드된 파일들을 정적 리소스로 서빙
		registry.addResourceHandler("/uploads/**")
			.addResourceLocations("file:uploads/")
			.setCachePeriod(3600);

		// 기본 이미지들도 정적 리소스로 서빙
		registry.addResourceHandler("/uploads/default/**")
			.addResourceLocations("file:uploads/default/")
			.setCachePeriod(86400); // 하루 캐시

		// 시민 외교사 유형 이미지들을 정적 리소스로 서빙
		registry.addResourceHandler("/type-image/**")
			.addResourceLocations("file:type-image/")
			.setCachePeriod(86400); // 하루 캐시
	}
}
