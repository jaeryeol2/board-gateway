package com.board.gateway.zconfig.filters;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import com.board.gateway.common.base.BusinessException;
import com.board.gateway.common.utils.TokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayFilter extends OncePerRequestFilter {
  private final TokenUtil tokenUtil;

  private static final AntPathMatcher antPathMacher = new AntPathMatcher();
  private static final List<String> IGNORE_PATH_LIST =
      List.of("/actuator/health", "/home", "/user/login", "/user/logout", "/auth/token-expire",
          "/auth/guest", "/user/user-count", "/user/signup");

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    final String path = request.getRequestURI();
    final String method = request.getMethod();
    final String ip = this.extractClientIp(request);
    final String accessToken = this.getAccessToken(request);
    final boolean isIgnore = IGNORE_PATH_LIST.stream().map("/api"::concat)
        .anyMatch(pattern -> antPathMacher.match(pattern, path));

    log.info("api path : {}, method : {}, client ip : {}, ignore path : {}", path, method, ip,
        isIgnore);

    if (isIgnore) {
      filterChain.doFilter(request, response);
      return;
    }

    if (Objects.isNull(accessToken) || tokenUtil.isTokenExpired(accessToken)) {
      final String refreshToken = this.getRefreshToken(request);

      if (Objects.isNull(refreshToken) || tokenUtil.isTokenExpired(refreshToken)) {
        final String guestId = request.getHeader("X-Guest-Id");

        if (Objects.isNull(guestId) || guestId.isBlank()) {
          throw new BusinessException("접근할 수 없습니다.");
        }
      }

    }

    filterChain.doFilter(request, response);
  }

  private String extractClientIp(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");

    if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
      return ip.split(",")[0].trim();
    }

    return request.getRemoteAddr();
  }

  private String getAccessToken(HttpServletRequest request) {
    final String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (Objects.nonNull(accessToken)) {
      return accessToken.replaceAll("^(?i)Bearer\\s+", "");
    }

    return null;
  }

  private String getRefreshToken(HttpServletRequest request) {
    final Cookie[] cookies =
        Objects.requireNonNullElseGet(request.getCookies(), () -> new Cookie[0]);
    final String token = Arrays.asList(cookies).stream()//
        .filter(Objects::nonNull)//
        .filter(cookie -> "auth_token".equalsIgnoreCase(cookie.getName()))//
        .findFirst()//
        .orElseGet(() -> {
          final Cookie cookie = new Cookie("auth_token", null);
          cookie.setHttpOnly(true);
          cookie.setSecure(true);

          return cookie;
        })//
        .getValue();

    return !tokenUtil.isTokenExpired(token) ? token : null;
  }

}
