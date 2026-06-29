package com.board.gateway.zconfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

@Slf4j
@Configuration
public class GatewayConfig {

  @Bean
  @Primary
  public JsonMapper jsonMapper() {
    // maskFilter: 모든 필드 직렬화 허용 (필터 미적용 시 예외 방지용)
    final SimpleFilterProvider filterProvider =
        new SimpleFilterProvider().addFilter("maskFilter", SimpleBeanPropertyFilter.serializeAll());

    return JsonMapper.builder()
        // ── 필터 ──────────────────────────────────────────────
        .filterProvider(filterProvider)
        // null 필드 제외
        .changeDefaultPropertyInclusion(
            incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL)
                .withContentInclusion(JsonInclude.Include.NON_NULL))
        // ── 역직렬화(Deserialization) ─────────────────────────
        // 알 수 없는 Enum → null 처리 (EnumFeature로 이동됨)
        .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)

        .build();
  }
}
