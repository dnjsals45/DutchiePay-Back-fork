package dutchiepay.backend.global.payment.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum PaymentErrorCode {

    /**
     * 400 BAD_REQUEST
     */
    INVALID_PRODUCT(HttpStatus.BAD_REQUEST, "상품 정보가 없습니다."),
    INVALID_BUY(HttpStatus.BAD_REQUEST, "구매 정보가 없습니다."),
    FINISHED_PAYMENT(HttpStatus.BAD_REQUEST, "이미 처리된 요청입니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "정보가 유효하지 않습니다."),
    INVALID_KAKAO_CANCEL_STATUS(HttpStatus.BAD_REQUEST, "카카오페이 결제를 할 수 없는 상태입니다."),

    /**
     * 500 INTERNAL_SERVER_ERROR
     */
    PORTONE_CANCEL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "결제 취소에 실패하였습니다."),
    INVALID_KAKAO_APPROVE_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "카카오페이 결제 승인 응답이 유효하지 않습니다."),
    ERROR_KAKAOPAY_STATUS(HttpStatus.INTERNAL_SERVER_ERROR, "카카오페이 결제 상태를 확인할 수 없습니다."),
    ERROR_KAKAOPAY_CANCEL(HttpStatus.INTERNAL_SERVER_ERROR, "카카오페이 결제 취소 중 오류가 발생하였습니다."),
    PAYMENT_IN_PROGRESS(HttpStatus.INTERNAL_SERVER_ERROR, "진행중인 거래가 있습니다."),
    EXTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "외부 서버 오류가 발생하였습니다."),;

    private final HttpStatus httpStatus;
    private final String message;
}
