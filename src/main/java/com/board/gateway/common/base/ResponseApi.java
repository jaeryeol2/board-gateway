package com.board.gateway.common.base;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@ToString
@Jacksonized
public class ResponseApi<T> {
  private static Executor customeExecutor;

  private final int status;
  private final String code;
  private final String message;
  private final T data;

  private ResponseApi(final int status, final String code, final String message, final T data) {
    this.status = status;
    this.code = code;
    this.message = message;
    this.data = data;
  }

  static void setExecutor(Executor executor) {
    customeExecutor = executor;
  }

  /**
   * 가공이 완료되었거나 메모리/캐시에서 즉시 조회하여 지연(Blocking)이 없는 데이터를 반환할 때 사용. 컨트롤러나 서비스 메서드의 반환 타입이 일반
   * 객체(ResponseApi)일 때 사용하는 메서드.
   *
   * @param data 응답 바디에 담을 준비된 데이터
   * @param <T> 데이터의 타입
   * @return HTTP 200(OK) 상태와 데이터가 담긴 ResponseApi 객체
   */
  public static <T> ResponseApi<T> ok(final T data) {
    final HttpStatus ok = HttpStatus.OK;
    return new ResponseApi<>(ok.value(), ok.name(), "success", data);
  }

  /**
   * 가공이 완료되었거나 메모리/캐시에서 즉시 조회하여 지연(Blocking)이 없는 데이터를 반환할 때 사용. 컨트롤러나 서비스 메서드의 반환 타입이 일반
   * 객체(ResponseApi)일 때 사용하는 메서드.
   *
   * @param status http 응답 상태
   * @param message 메시지
   * @param <T> 데이터의 타입
   * @param data 데이터
   * @return
   */
  public static <T> ResponseApi<T> ok(final HttpStatus httpStatus, final String message,
      final T data) {
    return new ResponseApi<>(httpStatus.value(), httpStatus.name(), message, data);
  }

  /**
   * 가공이 완료되었거나 메모리/캐시에서 즉시 조회하여 지연(Blocking)이 없는 데이터를 반환할 때 사용. 컨트롤러나 서비스 메서드의 반환 타입이 일반
   * 객체(ResponseApi)일 때 사용하는 메서드.
   * 
   * @param <T>
   * @param status
   * @param code
   * @param message
   * @param data
   * @return
   */
  public static <T> ResponseApi<T> ok(final int status, final String code, final String message,
      final T data) {
    return new ResponseApi<>(status, code, message, data);
  }

  /**
   * 메모리/캐시 조회 등 이미 준비된 데이터를 반환하지만, 인터페이스 스펙이나 비동기 체이닝 등의 이유로 메서드의 반환 타입이 반드시 `CompletableFuture`로
   * 강제되어 있을 때 사용. 새로운 백그라운드 스레드를 생성하지 않고 현재 스레드에서 즉시 완료된(Completed) Future를 반환하여 불필요한 스레드 컨텍스트 스위칭
   * 비용을 없애고 성능을 최적화.
   *
   * @param <T> 데이터의 타입
   * @param data 이미 준비된 데이터 (새로운 스레드에서 연산되지 않음)
   * @return 완료된 상태의 ResponseApi를 담은 CompletableFuture
   */
  public static <T> CompletableFuture<ResponseApi<T>> completedOk(final T data) {
    return CompletableFuture.completedFuture(ok(data));
  }

  /**
   * 메모리/캐시 조회 등 이미 준비된 데이터를 반환하지만, 인터페이스 스펙이나 비동기 체이닝 등의 이유로 메서드의 반환 타입이 반드시 `CompletableFuture`로
   * 강제되어 있을 때 사용. 새로운 백그라운드 스레드를 생성하지 않고 현재 스레드에서 즉시 완료된(Completed) Future를 반환하여 불필요한 스레드 컨텍스트 스위칭
   * 비용을 없애고 성능을 최적화.
   *
   * @param status http 응답 상태
   * @param message 메시지
   * @param <T> 데이터의 타입
   * @param data 데이터
   * @return 완료된 상태의 ResponseApi를 담은 CompletableFuture
   */
  public static <T> CompletableFuture<ResponseApi<T>> completedOk(final HttpStatus httpStatus,
      final String message, final T data) {
    return CompletableFuture.completedFuture(ok(httpStatus, message, data));
  }

  /**
   * 리턴 타입이 Void일때 인자는 없이 보낸다
   * 
   * @return
   */
  public static CompletableFuture<ResponseApi<Void>> completedOk() {
    return CompletableFuture.completedFuture(ok(null));
  }

  /**
   * DB 쿼리 실행, 외부 API 호출 등 메인(호출) 스레드를 멈추게(Blocking) 할 수 있는 무거운 작업을 처리할 때 사용. 전달받은 로직(Supplier)을
   * 백그라운드 스레드(ForkJoinPool 등)에서 비동기적으로 실행하고, 작업이 완료되면 그 결과를 공통 응답 포맷으로 래핑하여 반환.<br/>
   * <br/>
   * ※ 해당 함수는 사용하려는 함수에 @Async 어노테이션이 없을때 사용해야 합니다.
   *
   * @param <T> 데이터의 타입
   * @param supplier 백그라운드 스레드에서 비동기로 실행할 무거운 작업 로직
   * @return 비동기 실행 완료 후 ResponseApi를 담게 될 CompletableFuture
   */
  public static <T> CompletableFuture<ResponseApi<T>> asyncOk(final Supplier<T> supplier) {
    return CompletableFuture.supplyAsync(() -> ok(supplier.get()), customeExecutor);
  }

  /**
   * DB 쿼리 실행, 외부 API 호출 등 메인(호출) 스레드를 멈추게(Blocking) 할 수 있는 무거운 작업을 처리할 때 사용. 전달받은 로직(Supplier)을
   * 백그라운드 스레드(ForkJoinPool 등)에서 비동기적으로 실행하고, 작업이 완료되면 그 결과를 공통 응답 포맷으로 래핑하여 반환.<br/>
   * <br/>
   * ※ 해당 함수는 사용하려는 함수에 @Async 어노테이션이 없을때 사용해야 합니다.
   *
   * @param status http 응답 상태
   * @param message 메시지
   * @param <T> 데이터의 타입
   * @param supplier 백그라운드 스레드에서 비동기로 실행할 무거운 작업 로직
   * @return 비동기 실행 완료 후 ResponseApi를 담게 될 CompletableFuture
   */
  public static <T> CompletableFuture<ResponseApi<T>> asyncOk(final HttpStatus status,
      final String message, final Supplier<T> supplier) {
    return CompletableFuture.supplyAsync(() -> ok(status, message, supplier.get()),
        customeExecutor);
  }

  /**
   * 가공이 완료되었거나 메모리/캐시에서 즉시 조회하여 지연(Blocking)이 없는 데이터를 반환할 때 사용. 컨트롤러나 서비스 메서드의 반환 타입이 일반
   * 객체(ResponseApi)일 때 사용하는 메서드.
   *
   * @param <T> 데이터의 타입
   * @param total 총 갯수
   * @param list 응답 바디에 담을 준비된 리스트 데이터
   * @return HTTP 200(OK) 상태와 데이터가 담긴 ResponseApi 객체
   */
  public static <T> ResponseApi<ResponseList<T>> ok(final int total, final List<T> list) {
    return ok(ResponseList.of(total, list));
  }

  /**
   * 메모리/캐시 조회 등 이미 준비된 데이터를 반환하지만, 인터페이스 스펙이나 비동기 체이닝 등의 이유로 메서드의 반환 타입이 반드시 `CompletableFuture`로
   * 강제되어 있을 때 사용. 새로운 백그라운드 스레드를 생성하지 않고 현재 스레드에서 즉시 완료된(Completed) Future를 반환하여 불필요한 스레드 컨텍스트 스위칭
   * 비용을 없애고 성능을 최적화.
   * 
   * @param <T> 데이터의 타입
   * @param total 총 갯수
   * @param list 응답 바디에 담을 준비된 리스트 데이터
   * @return 비동기 실행 완료 후 ResponseApi를 담게 될 CompletableFuture
   */
  public static <T> CompletableFuture<ResponseApi<ResponseList<T>>> completedOk(final int total,
      final List<T> list) {
    return completedOk(ResponseList.of(total, list));
  }


  /**
   * DB 쿼리 실행, 외부 API 호출 등 메인(호출) 스레드를 멈추게(Blocking) 할 수 있는 무거운 작업을 처리할 때 사용. 전달받은 로직(Supplier)을
   * 백그라운드 스레드(ForkJoinPool 등)에서 비동기적으로 실행하고, 작업이 완료되면 그 결과를 공통 응답 포맷으로 래핑하여 반환.<br/>
   * <br/>
   * ※ 해당 함수는 사용하려는 함수에 @Async 어노테이션이 없을때 사용해야 합니다.
   * 
   * @param <T> 데이터의 타입
   * @param total 총 갯수
   * @param supplier 백그라운드 스레드에서 비동기로 실행할 무거운 작업 로직
   * @return 비동기 실행 완료 후 ResponseApi를 담게 될 CompletableFuture
   */
  public static <T> CompletableFuture<ResponseApi<ResponseList<T>>> asyncOk(final int total,
      final Supplier<List<T>> supplier) {
    return asyncOk(() -> ResponseList.of(total, supplier.get()));
  }

  /**
   * 에러 발생 시 공통 실패 응답 포맷을 생성합니다. (기본 HTTP 500)
   *
   * @param message 클라이언트에게 전달할 에러 메시지
   * @param <T> 데이터의 타입 (실패 시 보통 null)
   * @return 에러 메시지가 담긴 ResponseApi 객체
   */
  public static <T> ResponseApi<T> fail(final String message) {
    return ok(HttpStatus.INTERNAL_SERVER_ERROR, message, null);
  }

  /**
   * 에러 발생 시 공통 실패 응답 포맷을 생성합니다. (기본 HTTP 500)
   *
   * @param message 클라이언트에게 전달할 에러 메시지
   * @param <T> 데이터의 타입
   * @return 에러 메시지가 담긴 ResponseApi 객체
   */
  public static <T> ResponseApi<T> fail(final String message, final T data) {
    return ok(HttpStatus.INTERNAL_SERVER_ERROR, message, data);
  }

  /**
   * 커스텀 상태 코드와 함께 에러 응답을 생성합니다. (예: 400 Bad Request, 404 Not Found 등)
   *
   * @param status HTTP 상태 코드
   * @param message 클라이언트에게 전달할 에러 메시지
   * @param <T> 데이터의 타입
   * @return 상태 코드와 에러 메시지가 담긴 ResponseApi 객체
   */
  public static <T> ResponseApi<T> fail(final HttpStatus httpStatus, final String message) {
    return ok(httpStatus, message, null);
  }


  /**
   * 커스텀 상태 코드와 함께 에러 응답을 생성합니다.
   * 
   * @param <T>
   * @param status
   * @param code
   * @param message
   * @return
   */
  public static <T> ResponseApi<T> fail(final int status, final String code, final String message) {
    return ok(status, code, message, null);
  }


  @Getter
  @Builder
  @ToString
  @Jacksonized
  public static class ResponseList<L> {
    private int total;
    private List<L> list;

    private ResponseList(int total, List<L> list) {
      this.total = total;
      this.list = list;
    }

    public static <L> ResponseList<L> of(int total, List<L> list) {
      return new ResponseList<>(total, list);
    }
  }

}


@Component
@RequiredArgsConstructor
class ResponseApiInitializer {
  private final TaskExecutor appAsync;

  @PostConstruct
  public void init() {
    ResponseApi.setExecutor(appAsync);
  }
}
