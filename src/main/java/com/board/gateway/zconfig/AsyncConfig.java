package com.board.gateway.zconfig;

import java.util.Map;
import java.util.Objects;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskDecorator;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class AsyncConfig {

  @Bean
  @Primary
  public TaskDecorator taskDecorator() {
    return runnable -> {
      final Map<String, String> contextMap = MDC.getCopyOfContextMap();
      final RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

      return () -> {
        try {
          if (Objects.nonNull(contextMap)) {
            MDC.setContextMap(contextMap);
          }
          if (Objects.nonNull(requestAttributes)) {
            RequestContextHolder.setRequestAttributes(requestAttributes);
          }

          runnable.run();
        } catch (Exception e) {
          log.error("gateway async error : {}", e.getMessage());
        } finally {
          MDC.clear();
          RequestContextHolder.resetRequestAttributes();
        }
      };
    };
  }
}
