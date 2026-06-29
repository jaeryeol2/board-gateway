package com.board.gateway.zconfig.entrypoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import com.board.gateway.common.base.ResponseApi;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayEntryPoint implements AuthenticationEntryPoint {

  private final JsonMapper jsonMapper;

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException authException) throws IOException, ServletException {
    log.warn("security exception : {}", authException.getMessage());

    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

    ResponseApi<Boolean> fail = ResponseApi.fail(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");

    response.getWriter().print(jsonMapper.writeValueAsString(fail));
  }
}
