package com.board.gateway.zconfig;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.board.gateway.zconfig.filters.GatewayFilter;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  @Value("${spring.profiles.active}")
  private String active;
  @Value("${front.url}")
  private String frontURL;
  private final GatewayFilter authFilter;

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) {
    http.httpBasic(basic -> basic.disable()).csrf(csrf -> csrf.disable())
        .formLogin(login -> login.disable()).logout(logout -> logout.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .cors(cors -> cors.configurationSource(configurationSource()))
        .securityContext(context -> context
            .securityContextRepository(new RequestAttributeSecurityContextRepository()))
        .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

    return http.build();
  }

  @Bean
  CorsConfigurationSource configurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

    config.setAllowCredentials(true);
    config.setMaxAge(Duration.ofHours(1L));
    config.setAllowedOrigins(Arrays.asList(frontURL.split(",")));
    config.setAllowedHeaders(List.of(HttpHeaders.CONTENT_TYPE, HttpHeaders.AUTHORIZATION));

    // 보완: 모든 REST Method 허용 (PUT, PATCH, DELETE, OPTIONS 추가)
    config.setAllowedMethods(
        List.of(HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.PUT.name(),
            HttpMethod.PATCH.name(), HttpMethod.DELETE.name(), HttpMethod.OPTIONS.name()));

    source.registerCorsConfiguration("/**", config);

    return source;
  }

}
