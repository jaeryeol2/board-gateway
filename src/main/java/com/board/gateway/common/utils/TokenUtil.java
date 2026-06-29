package com.board.gateway.common.utils;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Component
public class TokenUtil {
  @Value("${jwt.secret.key}")
  private String secretKeyString;

  private SecretKey secretKey;
  private JwtParser jwtParser;

  @PostConstruct
  protected void init() {
    // secret key 문자열을 UTF-8 바이트 배열로 변환하고, 이를 기반으로 SecretKey 객체를 생성합니다.
    // 이 키는 HS256 알고리즘에 사용됩니다.
    this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes(StandardCharsets.UTF_8));
    this.jwtParser = Jwts.parser().verifyWith(secretKey).build();
  }

  public String createToken(final String username, final long hour) {
    return Jwts.builder().subject(username).issuedAt(new Date())
        .expiration(
            Date.from(OffsetDateTime.now(ZoneId.of("Asia/Seoul")).plusHours(hour).toInstant()))
        .signWith(secretKey, Jwts.SIG.HS256) // HS256 알고리즘으로 서명
        .compact();
  }

  public Claims getTokenInfo(final String token) {
    try {
      Jws<Claims> jws = jwtParser.parseSignedClaims(token);
      return jws.getPayload();
    } catch (ExpiredJwtException e) {
      return e.getClaims();
    }
  }

  public boolean isTokenExpired(final String token) {
    if (!StringUtils.hasText(token)) {
      return true;
    }
    try {
      Jws<Claims> jws = jwtParser.parseSignedClaims(token);
      return jws.getPayload().getExpiration().before(new Date());
    } catch (Exception _) {
      return true;
    }
  }

}

