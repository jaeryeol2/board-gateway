package com.board.gateway.zconfig.filters;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import com.board.gateway.common.utils.TokenUtil;
import com.board.gateway.zconfig.entrypoint.GatewayEntryPoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayFilter extends OncePerRequestFilter {
  private final TokenUtil tokenUtil;
  private final GatewayEntryPoint gatewayEntryPoint;

  private static final AntPathMatcher antPathMacher = new AntPathMatcher();
  private static final List<String> IGNORE_PATH_LIST =
      List.of("/actuator/health", "/home", "/user/login", "/user/logout", "/auth/token-expire",
          "/auth/access", "/auth/guest", "/user/user-count", "/user/signup");

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    final String path = request.getRequestURI();
    final String method = request.getMethod();
    final String ip = extractClientIp(request);
    final String accessToken = getAccessToken(request);
    final boolean isIgnore = IGNORE_PATH_LIST.stream().map("/api"::concat)
        .anyMatch(pattern -> antPathMacher.match(pattern, path));

    log.info("api path : {}, method : {}, client ip : {}, ignore path : {}", path, method, ip,
        isIgnore);

    if (isIgnore) {
      filterChain.doFilter(request, response);
      return;
    }

    if (Objects.isNull(accessToken) || tokenUtil.isTokenExpired(accessToken)) {
      final String guestId =
          Objects.requireNonNullElseGet(request.getHeader("X-Guest-Id"), () -> "");

      if (guestId.isBlank()) {
        gatewayEntryPoint.commence(request, response,
            new InsufficientAuthenticationException("접근할 수 없습니다."));
        return;
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

}

