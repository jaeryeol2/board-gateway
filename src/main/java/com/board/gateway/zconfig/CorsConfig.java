package com.board.gateway.zconfig;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class CorsConfig {
  @Value("${front.url}")
  private String frontURL;

  @Bean
  CorsConfigurationSource configurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

    config.setAllowCredentials(true);
    config.setMaxAge(Duration.ofHours(1L));
    config.setAllowedOrigins(Arrays.asList(frontURL.split(",")));
    config.setAllowedHeaders(List.of("*"));

    // 보완: 모든 REST Method 허용 (PUT, PATCH, DELETE, OPTIONS 추가)
    config.setAllowedMethods(
        List.of(HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.PUT.name(),
            HttpMethod.PATCH.name(), HttpMethod.DELETE.name(), HttpMethod.OPTIONS.name()));

    source.registerCorsConfiguration("/**", config);

    return source;
  }
}
