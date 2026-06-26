package com.board.gateway.common.base;

import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
  private final int status;
  private final String code;

  public BusinessException(final String message) {
    super(message);
    this.status = 999;
    this.code = "Business Error.";
  }

  public BusinessException(final String message, final HttpStatus httpStatus) {
    super(message);
    this.status = httpStatus.value();
    this.code = httpStatus.name();
  }

  public BusinessException(final String message, final int status, final String code) {
    super(message);
    this.status = status;
    this.code = code;
  }
}
